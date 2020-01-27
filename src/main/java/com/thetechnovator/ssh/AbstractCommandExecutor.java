package com.thetechnovator.ssh;

import static com.thetechnovator.ssh.Constants.CMD_SUFFIX;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class AbstractCommandExecutor {
	private OutputStream out;
	private InputStream in;
	private SshSession session;
	public AbstractCommandExecutor(SshSession session) {
		super();
		this.session=session;
		this.out = session.getShell().getInvertedIn();
		this.in = session.getShell().getInvertedOut();
	}

	protected void sendCommand(String cmd) throws SshSessionlException {
		try {
			out.write((cmd + "\n").getBytes());
			out.flush();
		} catch (IOException e) {
			throw new SshSessionlException(e);
		}
	}

	public SshSession getSession() {
		return session;
	}

	public OutputStream getOut() {
		return out;
	}

	public InputStream getIn() {
		return in;
	}

	protected static String getCommandWithEndMarker(String cmd) {
		return cmd+CMD_SUFFIX;
	}
	protected static void sleep(int ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			// on existing case
		}
	}
}
