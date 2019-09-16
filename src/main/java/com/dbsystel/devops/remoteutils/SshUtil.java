package com.dbsystel.devops.remoteutils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dbsystel.devops.java.common.utils.DateUtil;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class SshUtil {
	private static final Logger LOG = LoggerFactory.getLogger(SshUtil.class);
	public static final Charset UTF8 = Charset.forName("UTF-8");
	private static Set<String> passwordTokens= new HashSet<>();
	static {
		passwordTokens.add("-secret");
		passwordTokens.add("-p");
		passwordTokens.add("-pw");
		passwordTokens.add("-password");	
		passwordTokens.add("-pass");	
		passwordTokens.add("-pwd");
		passwordTokens.add("-Dpw");	
		passwordTokens.add("-Dpassword");	
		passwordTokens.add("-Dpass");	
		passwordTokens.add("-Dpwd");	
		passwordTokens.add("-Dsecret");	
	}
	public static Session createSSHsession(String hostname, String username, String password, Boolean isPasswordSshKey)
			throws RemoteUtilException {
		try {

			JSch jsch = new JSch();
			JSch.setLogger(new MyLogger());
			if (isPasswordSshKey) {
				LOG.info("Authenticating user " + username + " using key ");
				if (LOG.isDebugEnabled())
					LOG.info("SSH KEY=" + password);
				jsch.getIdentityRepository().add(password.getBytes(StandardCharsets.UTF_8));
			}

			LOG.info("Connecting to " + hostname);
			Session session = jsch.getSession(username, hostname, 22);

			if (!isPasswordSshKey) {
				LOG.info("Authenticating user " + username + " using password");
				session.setPassword(password);
			}
			Properties config = new Properties();
			config.put("StrictHostKeyChecking", "no");
			session.setConfig(config);
			session.setDaemonThread(true);
			session.connect();

			return session;

		} catch (JSchException e) {
			throw new RemoteUtilException(e.getMessage(), e);
		}
	}

	public static void scpFrom(Session session, File localFilePath, String remoteFilePath) throws RemoteUtilException {
		FileOutputStream fos = null;
		Channel channel = null;
		LOG.info("Copying file " + remoteFilePath + " to " + localFilePath.getAbsolutePath());

		try {

			String prefix = null;
			if (localFilePath.isDirectory()) {
				prefix = localFilePath + File.separator;
			}

			// exec 'scp -f rfile' remotely
			remoteFilePath = remoteFilePath.replace("'", "'\"'\"'");
			remoteFilePath = "'" + remoteFilePath + "'";
			String command = "scp -f " + remoteFilePath;
			channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand(command);

			// get I/O streams for remote scp
			OutputStream out = channel.getOutputStream();
			InputStream in = channel.getInputStream();

			channel.connect();

			byte[] buf = new byte[1024];

			// send '\0'
			buf[0] = 0;
			out.write(buf, 0, 1);
			out.flush();

			while (true) {
				int c = checkAck(in);
				if (c != 'C') {
					break;
				}

				// read '0644 '
				in.read(buf, 0, 5);

				long filesize = 0L;
				while (true) {
					if (in.read(buf, 0, 1) < 0) {
						// error
						break;
					}
					if (buf[0] == ' ')
						break;
					filesize = filesize * 10L + (long) (buf[0] - '0');
				}

				String file = null;
				for (int i = 0;; i++) {
					in.read(buf, i, 1);
					if (buf[i] == (byte) 0x0a) {
						file = new String(buf, 0, i);
						break;
					}
				}

				// send '\0'
				buf[0] = 0;
				out.write(buf, 0, 1);
				out.flush();

				// read a content of lfile
				fos = new FileOutputStream(prefix == null ? localFilePath.getAbsolutePath() : prefix + file);
				int foo;
				while (true) {
					if (buf.length < filesize)
						foo = buf.length;
					else
						foo = (int) filesize;
					foo = in.read(buf, 0, foo);
					if (foo < 0) {
						// error
						break;
					}
					fos.write(buf, 0, foo);
					filesize -= foo;
					if (filesize == 0L)
						break;
				}
				fos.close();
				fos = null;

				if (checkAck(in) != 0) {
					throw new RemoteUtilException("Error receiving file from remote machine");
				}

				// send '\0'
				buf[0] = 0;
				out.write(buf, 0, 1);
				out.flush();
			}

		} catch (JSchException e) {
			throw new RemoteUtilException(e.getMessage(), e);
		} catch (IOException e) {
			throw new RemoteUtilException(e.getMessage(), e);
		} finally {
			if (channel != null) {
				channel.disconnect();
			}
		}
	}

	public static void scpTo(Session session, File filePath, String remoteFilePath) throws RemoteUtilException {

		LOG.info("Copying file " + filePath.getAbsolutePath() + " to " + remoteFilePath);
		OutputStream out = null;
		Channel channel = null;
		try {

			boolean ptimestamp = true;

			// exec 'scp -t rfile' remotely
			String command = "scp " + (ptimestamp ? "-p" : "") + " -t " + remoteFilePath;
			channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand(command);

			// get I/O streams for remote scp
			out = channel.getOutputStream();
			InputStream in = channel.getInputStream();

			channel.connect();

			if (checkAck(in) != 0) {
				throw new RemoteUtilException("Error sending files to remote machine");
			}

			if (ptimestamp) {
				command = "T" + (filePath.lastModified() / 1000) + " 0";
				// The access time should be sent here,
				// but it is not accessible with JavaAPI ;-<
				command += (" " + (filePath.lastModified() / 1000) + " 0\n");
				out.write(command.getBytes());
				out.flush();
				if (checkAck(in) != 0) {
					throw new RemoteUtilException("Error sending files to remote machine");
				}
			}

			// send "C0644 filesize filename", where filename should not include '/'
			long filesize = filePath.length();
			command = "C0644 " + filesize + " ";
			/*
			 * if (filePath.lastIndexOf('/') > 0) { command +=
			 * filePath.substring(filePath.lastIndexOf('/') + 1); } else { command +=
			 * filePath; }
			 */
			command += filePath.getName();
			command += "\n";
			out.write(command.getBytes());
			out.flush();
			if (checkAck(in) != 0) {
				throw new RemoteUtilException("Error sending files to remote machine");
			}

			// send a content of lfile
			FileInputStream fis = new FileInputStream(filePath);
			byte[] buf = new byte[1024];
			while (true) {
				int len = fis.read(buf, 0, buf.length);
				if (len <= 0)
					break;
				out.write(buf, 0, len); // out.flush();
			}
			fis.close();
			fis = null;
			// send '\0'
			buf[0] = 0;
			out.write(buf, 0, 1);
			out.flush();
			if (checkAck(in) != 0) {
				throw new RemoteUtilException("Error sending files to remote machine");
			}

		} catch (JSchException e) {
			throw new RemoteUtilException(e.getMessage(), e);
		} catch (IOException e) {
			throw new RemoteUtilException(e.getMessage(), e);
		} finally {
			if (channel != null) {
				channel.disconnect();
			}
		}
	}

	public static ExecResult exec(Session session, SshCommand sshCmd) throws RemoteUtilException {
		try {
			ChannelExec channelExec = (ChannelExec) session.openChannel("exec");
			String cmd = sshCmd.getFinalCommand();
			LOG.info("Executing command " + cmd.replaceAll("(-pw)(.*?)(-)", "$1 ***** -").replaceAll("(-password)(.*?)(-)", "$1 ***** -"));
			//channelExec.setPty(true);
			channelExec.setEnv("TERM", "xterm");
			channelExec.setInputStream(null);
			channelExec.setCommand(cmd);
			ChannelStream stream = new ChannelStream(channelExec, sshCmd);
			channelExec.connect();
			if (!StringUtils.isEmpty(sshCmd.getSuUsername()) && !StringUtils.isEmpty(sshCmd.getSuPassword())) {
				doWait(500);
				LOG.debug("Sending password");
				stream.write(sshCmd.getSuPassword());
				doWait(100);
			}
			// Wait till the command is not finished or timeout
			stream.readAndLog(sshCmd.getMaxExecutionTimeMs(), sshCmd.getInactivityTimeoutMs());

			if (stream.isTimeout()) {
				throw new RemoteUtilException(
						"Command " + sshCmd.getCommand() + " timed out. Execution Timeout: " + DateUtil.msToNamedTimeInterval(sshCmd.getMaxExecutionTimeMs())+", Inactivity Timeout: "+DateUtil.msToNamedTimeInterval(sshCmd.getInactivityTimeoutMs()));
			}
			LOG.debug("Channel exit status: " + channelExec.getExitStatus());
			if (channelExec.getExitStatus() == 125) { // su password failed
				throw new RemoteUtilException("Switch User authentication failed: "+stream.error);
			}
			int exitStatus = channelExec.getExitStatus();
			channelExec.disconnect();
			return new ExecResult(exitStatus, stream.getLastOutput(),stream.error);

		} catch (Exception e) {
			throw new RemoteUtilException(e.getMessage(), e);
		}

	}

	// Check Acknowledge from SCP command
	static int checkAck(InputStream in) throws IOException {
		int b = in.read();
		// b may be 0 for success,
		// 1 for error,
		// 2 for fatal error,
		// -1
		if (b == 0)
			return b;
		if (b == -1)
			return b;

		if (b == 1 || b == 2) {
			StringBuffer sb = new StringBuffer();
			int c;
			do {
				c = in.read();
				sb.append((char) c);
			} while (c != '\n');
			if (b == 1) { // error
				LOG.error(sb.toString());
			}
			if (b == 2) { // fatal error
				LOG.error(sb.toString());
			}
		}
		return b;
	}

	private static void println(String msg) {
		System.out.println(msg);
	}

	private static void print(String msg) {
		System.out.print(msg);
	}
	private static void waitForChannelClose(Channel channel) {
		while (!channel.isClosed()) {
			doWait(100);
		}
	}
	private static void doWait(int ms) {
		try {
			LOG.trace(Thread.currentThread().getName() + " waiting for: " + DateUtil.msToNamedTimeInterval(ms));
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			LOG.debug(Thread.currentThread().getName() + " interrupted");
		}
	}
	public static class SshCommand {
		public static final long DEFAULT_INACTIVIT_TIMEOUT = 10 * 60 * 1000; //15 minute
		public static final long DEFAULT_MAX_EXECUTION_TIME = 90 * 60 * 1000; //90 minutes
		
		private String command;
		private String suUsername;
		private String suPassword;
		private boolean doSudo;
		private long maxExecutionTimeMs=DEFAULT_MAX_EXECUTION_TIME;
		private long inactivityTimeoutMs=DEFAULT_INACTIVIT_TIMEOUT;
		private OutputStream outputStream;
		private OutputStream errorStream;
		private boolean showOutput=true;
		public SshCommand(String command) {
			this(command,null,null,false);
		}
		public SshCommand(String command, String suUsername, String suPassword, boolean doSudo) {
			super();
			this.command = command;
			this.suUsername = suUsername;
			this.suPassword = suPassword;
			this.doSudo = doSudo;
		}
		public String getCommand() {
			return command;
		}
		public void setCommand(String command) {
			this.command = command;
		}
		public String getSuUsername() {
			return suUsername;
		}
		public void setSuUsername(String suUsername) {
			this.suUsername = suUsername;
		}
		public String getSuPassword() {
			return suPassword;
		}
		public void setSuPassword(String suPassword) {
			this.suPassword = suPassword;
		}
		public boolean isDoSudo() {
			return doSudo;
		}
		public void setDoSudo(boolean doSudo) {
			this.doSudo = doSudo;
		}
		public OutputStream getOutputStream() {
			return outputStream;
		}
		public void setOutputStream(OutputStream outputStream) {
			this.outputStream = outputStream;
		}
		public OutputStream getErrorStream() {
			return errorStream;
		}
		public void setErrorStream(OutputStream errorStream) {
			this.errorStream = errorStream;
		}
		
		public long getMaxExecutionTimeMs() {
			return maxExecutionTimeMs;
		}
		public void setMaxExecutionTimeMs(long maxExecutionTimeMs) {
			this.maxExecutionTimeMs = maxExecutionTimeMs;
		}
		public long getInactivityTimeoutMs() {
			return inactivityTimeoutMs;
		}
		public void setInactivityTimeoutMs(long inactivityTimeoutMs) {
			this.inactivityTimeoutMs = inactivityTimeoutMs;
		}
		
		public boolean isShowOutput() {
			return showOutput;
		}
		public void setShowOutput(boolean showOutput) {
			this.showOutput = showOutput;
		}
		public String getFinalCommand() {
			StringBuilder sb = new StringBuilder();
			if (doSudo) {
				sb.append("sudo ");
			}
			if (!StringUtils.isEmpty(suUsername)) {
				sb.append("su - ").append(suUsername).append( " -c \"").append(command).append("\"");
			}else {
				sb.append(command);					
			}
			
			return sb.toString();
		}
	}
	private static class ChannelStream {
		private InputStream in;
		private OutputStream out;
		private ChannelExec channel;
		private List<String> outputLines = new LinkedList<>();
		private int maxLines;
		boolean isTimeout=false;
		private SshCommand cmd;
		private String error;
		private ByteArrayOutputStream errorStream;
		public ChannelStream(ChannelExec channel,SshCommand cmd) {
			this(channel, cmd, 50);
		}

		public ChannelStream(ChannelExec channel, SshCommand cmd, int maxLines) {
			super();
			try {
				this.in = channel.getInputStream();
				this.out=channel.getOutputStream();
				errorStream = new ByteArrayOutputStream();
				channel.setErrStream(errorStream);
				this.channel=channel;
				this.cmd=cmd;
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
			this.maxLines = maxLines;
		}
		public void readAndLog(long maxWaitMilliseconds, long maxInactivityTimeout) {
			long timeout = System.currentTimeMillis()+maxWaitMilliseconds;
			try {
				byte[] buf = new byte[1024];
				StringBuilder sb = new StringBuilder();
				while (System.currentTimeMillis() <= timeout) {
					//wait till there are bytes
					long inactivityTimeout = System.currentTimeMillis()+maxInactivityTimeout;
					while (in.available() == 0 && System.currentTimeMillis() <= timeout && !channel.isClosed() && System.currentTimeMillis() <= inactivityTimeout) {
						doWait(100);
						//println("eof="+channel.isEOF()+", closed="+channel.isClosed()+", exitStatus="+channel.getExitStatus());
					}
					if (System.currentTimeMillis() > inactivityTimeout) {
						LOG.error("Timeout in reading input. Max Inactivity Timeout is "+DateUtil.msToNamedTimeInterval(maxInactivityTimeout));
						isTimeout=true;
						break;						
					}
					if (System.currentTimeMillis() > timeout) {
						LOG.error("Timeout in reading input. Max Execution Timeout is "+DateUtil.msToNamedTimeInterval(maxWaitMilliseconds));
						isTimeout=true;
						break;						
					}
					int i = in.read(buf, 0, 1024);
					if (i < 0) {
						LOG.debug("End of Input");
						break;
					}
					if (cmd.getOutputStream() != null) {
						cmd.getOutputStream().write(buf, 0, i);
					}
					String recdBuf = new String(buf, 0, i);
					sb.append(recdBuf);
					if (recdBuf.contains("Password:")) {
						continue;
					}
					if (recdBuf.contains("\n")) {
						addOutput(sb);
					}
					if (cmd.isShowOutput()) {
						print(recdBuf);
					}
				}
				addOutput(sb);
				if (channel.getExitStatus() != 0 && !isTimeout) {
					waitForChannelClose(channel);
					error = errorStream.toString();
					//Password prompt is also sent to error stream. Remove it.
					if (error != null) {
						//LOG.debug("Original Error: "+error);
						error = StringUtils.replaceEachRepeatedly(error, new String[] {"tput: No value for $TERM and no -T specified","tput: unknown terminal \"unknown\""}, new String[] {"",""});
						error = StringUtils.strip(error).trim();
						error = error.replaceAll("Password: ", "");
					}
				}
			} catch (Exception e) {
				LOG.error("Exception in reader thread: " + e.getMessage(), e);
				error=e.getMessage();
				return;
			}
		}
		public void write(String str) {
			try {
				out.write((str + "\n").getBytes());
				out.flush();
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}
		private void addOutput(StringBuilder sb) {
			String output = sb.toString();
			if (output.length() > 0) {
				String lines[] = output.split("\\n");
				outputLines.addAll(Arrays.asList(lines));
				if (outputLines.size() > maxLines) {
					outputLines.remove(0);
				}
			}
			sb.delete(0, sb.length());
		}

		public String getLastOutput() {
			if (outputLines.size() > 0) {
				return outputLines.get(outputLines.size() - 1);
			} else {
				return null;
			}
		}

		public boolean isTimeout() {
			return isTimeout;
		}
	}

	public static class MyLogger implements com.jcraft.jsch.Logger {
		static Hashtable<Integer, String> name = new java.util.Hashtable<Integer, String>();
		static {
			name.put(new Integer(DEBUG), "DEBUG: ");
			name.put(new Integer(INFO), "INFO: ");
			name.put(new Integer(WARN), "WARN: ");
			name.put(new Integer(ERROR), "ERROR: ");
			name.put(new Integer(FATAL), "FATAL: ");
		}

		public boolean isEnabled(int level) {
			if (level < 2) {
				return false;
			}
			return true;
		}

		public void log(int level, String message) {
			System.err.print(name.get(new Integer(level)));
			System.err.println(message);
		}
	}
	
	public static class ExecResult {
		private int exitStatus;
		private String lastOutput;
		private String error;
		public ExecResult(int exitStatus, String lastOutput,String error) {
			super();
			this.exitStatus = exitStatus;
			this.lastOutput = lastOutput;
			this.error=error;
		}
		public int getExitStatus() {
			return exitStatus;
		}
		public String getLastOutput() {
			return lastOutput;
		}
		public String getError() {
			return error;
		}
		
	}
}
