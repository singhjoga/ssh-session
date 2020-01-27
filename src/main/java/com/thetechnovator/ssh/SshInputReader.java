package com.thetechnovator.ssh;

import static com.thetechnovator.ssh.Constants.CR;
import static com.thetechnovator.ssh.Constants.LF;

import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.lang3.math.NumberUtils;

public abstract class SshInputReader {

	private SshSession session;
	private int status = -1;
	private SshCommand cmd;
	private String error = null;

	public SshInputReader(SshCommand cmd, SshSession session) {
		this.session = session;
		this.cmd = cmd;
	}

	public int readCommandOutput(String cmd) throws SshSessionlException {
		InactivityCheck inactivityCheck = new InactivityCheck(this);
		MaxExecutionTimeCheck executionTimeCheck = new MaxExecutionTimeCheck(this);
		Timer timer = new Timer();
		timer.schedule(executionTimeCheck, this.cmd.getMaxExecutionTimeMs());
		timer.schedule(inactivityCheck, 5, 5);

		int readLen = 0;
		InputStream in = session.getShell().getInvertedOut();
		boolean groupReceived = false;
		boolean start = true;
		status = -1;
		try {
			StringBuilder sb = new StringBuilder();
			while (status == -1) {
				if (in.available() == 0) {
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				int c = in.read();
				if (c == -1) {
					break;
				}
				inactivityCheck.setLastActivityTime(System.currentTimeMillis());
				// sb.append((char)c);
				readLen++;
				if (readLen < cmd.length()) {
					// ignore command output
					continue;
				} else if (readLen == cmd.length()) {
					if (cmd.endsWith(Constants.CMD_SUFFIX)) {
						writeCommand(cmd.substring(0, cmd.length() - Constants.CMD_SUFFIX.length()) + System.lineSeparator());
					} else {
						writeCommand(cmd + System.lineSeparator());
					}
					continue;
				}
				// carriage return line feed in the beginning
				if (start && (c == 13 || c == 10)) {
					continue;
				}
				if (c == Constants.END_OF_TEXT_GROUP) {
					groupReceived = true;
					continue;
				}
				if (groupReceived && c == Constants.END_OF_TEXT_RECORD) {
					// now we are sure that it is the output from our command which we append to the
					// real command
					// read the exist code
					status = readExitCode(in);
					writeCommand(System.lineSeparator());
					break;
				}
				// System.out.print(sb.toString());
				start = false;
				groupReceived = false;
				write((char) c);
			}
			if (!groupReceived) {
				// if it is some command which does not return the marker. Wait for the output
				sleep(1000);
			}
			flushBuffer(in);
			return status;
		} catch (IOException e) {
			throw handleException(e);
		} catch (SshSessionlException e) {
			throw e;
		} finally {
			flushBufferQuiet(in);
			timer.cancel();
		}
	}

	private SshSessionlException handleException(IOException e) throws SshSessionlException {
		if (this.error != null) {
			// it could be a timeout error or something where session is closed
			throw new SshSessionlException(error);
		} else {
			throw new SshSessionlException(e);
		}
	}

	public SshSession getSession() {
		return session;
	}

	public SshCommand getCommand() {
		return cmd;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
		try {
			write(error + System.lineSeparator());
		} catch (SshSessionlException e) {
			throw new IllegalStateException(e);
		}
	}

	private int readExitCode(InputStream input) throws SshSessionlException {
		StringBuilder sb = new StringBuilder();
		try {
			while (true) {
				if (input.available() == 0) {
					sleep(10);
				}
				if (input.available() == 0) {
					throw new SshSessionlException("Response marker record not fully received");
				}
				int c = input.read();
				if (c == Constants.END_OF_TEXT_UNIT) {
					// received the complete marker record.
					String buf = sb.toString();
					if (buf.length() == 0) {
						throw new SshSessionlException("Invalid Response marker record. It does not contain exit code");
					}
					// verify if it is a valid number
					if (!NumberUtils.isCreatable(buf)) {
						throw new SshSessionlException("Invalid Response marker record. Not a valid exit code: " + buf);
					}
					return NumberUtils.createInteger(buf);
				}
				sb.append((char) c);
			}
		} catch (IOException e) {
			throw handleException(e);
		}
	}

	protected static boolean isNewLine(int c) {
		return c == CR || c == LF;
	}

	protected static void sleep(int ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			// on existing case
		}
	}

	public static boolean skipLine(InputStream in, String line) throws SshSessionlException {
		int readLen = 0;
		StringBuilder buf = new StringBuilder();
		try {
			in.mark(line.length());
			while (readLen < line.length()) {
				if (in.available() == 0) {
					sleep(10);
					if (in.available() == 0) {
						break;
					}
				}
				int c = in.read();
				if (c == -1) {
					break;
				}
				readLen++;
				buf.append((char) c);
			}
			if (!line.equals(buf.toString())) {
				if (in.markSupported()) {
					in.reset();
				}
				return false;
			}
			System.out.println("Skipped: " + buf.toString());
			// also skip carriage return and line feed
			skipLineEnd(in);
			return true;
		} catch (IOException e) {
			throw new SshSessionlException(e);
		}
	}

	public static boolean skipLineEnd(InputStream in) throws SshSessionlException {
		try {
			in.mark(1);
			int c = in.read();
			if (!(c == 13 || c == 10)) {
				in.reset();
				return true;
			}
			System.out.println("Skipped: CR");
			in.mark(1);
			c = in.read();
			if (!(c == 13 || c == 10)) {
				System.out.print(c);
				in.reset();
				return true;
			}
			System.out.println("Skipped: LF");
			return true;
		} catch (IOException e) {
			throw new SshSessionlException(e);
		}
	}

	public static void flushBuffer(InputStream in) throws IOException {
		// System.out.println("Flush buffer");
		while (true) {
			if (in.available() == 0) {
				sleep(10);
				if (in.available() == 0) {
					break;
				}
			}
			int c = in.read();
			// System.out.print((char)c);
		}
	}

	public static void printBuffer(InputStream in) {
		System.out.println("Print buffer start");
		try {
			while (true) {
				if (in.available() == 0) {
					sleep(10);
					if (in.available() == 0) {
						break;
					}
				}
				int c = in.read();
				System.out.print((char) c);
			}
			System.out.println("Print buffer end");
		} catch (IOException e) {
			System.out.println("Print buffer error: " + e.getMessage());
		}
	}

	public static void flushBufferQuiet(InputStream in) {
		try {
			flushBuffer(in);
		} catch (IOException e) {
			// ignore errors
		}
	}

	protected void markSuccess() {
		this.status = Constants.SUCCESS;
	}

	protected void markFailure() {
		this.status = Constants.FAILURE;
	}

	protected abstract void write(int c) throws SshSessionlException;

	protected void write(String str) throws SshSessionlException {
		for (int i = 0; i < str.length(); i++) {
			write(str.charAt(i));
		}
	}

	protected abstract void writeCommand(String str) throws SshSessionlException;

	private static final class InactivityCheck extends TimerTask {
		private SshInputReader reader;
		private long lastActivityTime = System.currentTimeMillis();

		public InactivityCheck(SshInputReader reader) {
			super();
			this.reader = reader;
		}

		@Override
		public void run() {
			long timeout = lastActivityTime + reader.cmd.getMaxInactivityTimeMs();
			if (System.currentTimeMillis() > timeout) {
				String msg = "Inactivity Timeout: Command '" + reader.cmd.getCommandLine() + "' did not write anything to console in the last "
						+ Utils.msToNamedTimeInterval(reader.cmd.getMaxInactivityTimeMs());
				reader.setError(msg);
				sleep(10);
				reader.session.close();
			}
		}

		public void setLastActivityTime(long lastActivityTime) {
			this.lastActivityTime = lastActivityTime;
		}

	}

	private static final class MaxExecutionTimeCheck extends TimerTask {
		private SshInputReader reader;

		public MaxExecutionTimeCheck(SshInputReader reader) {
			super();
			this.reader = reader;
		}

		@Override
		public void run() {
			String msg = "Max Exection Timeout: Command '" + reader.cmd.getCommandLine() + "' did not complete in " + Utils.msToNamedTimeInterval(reader.cmd.getMaxInactivityTimeMs());
			reader.setError(msg);
			sleep(10);
			reader.session.close();
		}
	}
}
