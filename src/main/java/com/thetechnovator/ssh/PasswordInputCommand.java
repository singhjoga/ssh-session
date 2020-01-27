package com.thetechnovator.ssh;

import static com.thetechnovator.ssh.Constants.CR;
import static com.thetechnovator.ssh.Constants.LF;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public  abstract class PasswordInputCommand extends AbstractCommandExecutor {
	private static final Logger LOG = LoggerFactory.getLogger(PasswordInputCommand.class);
	private String passwordPrompt;
	private PasswordProvider passwordProvider;
	private String successResponse;
	private String failureResponse;

	public PasswordInputCommand(SshSession session, String passwordPrompt, PasswordProvider passwordProvider, String successResponse, String failureResponse) {
		super(session);
		this.passwordPrompt = passwordPrompt;
		this.passwordProvider = passwordProvider;
		this.successResponse = successResponse;
		this.failureResponse = failureResponse;
	}

	public int exec(SshCommand cmd) throws SshSessionlException {
		StringBuilder sb = new StringBuilder();
		SshInputReader reader = new SshInputReader(cmd, getSession()) {
			boolean passwordSent = false;

			@Override
			public void writeCommand(String str) throws SshSessionlException {
			}

			@Override
			public void write(int c) throws SshSessionlException {
				if (c == CR || c == LF) {
					return;
				}
				sb.append((char) c);
				String buf = sb.toString();
				if (passwordSent) {
					if (successResponse != null && buf.length() == successResponse.length()) {
						sb.delete(0, sb.length());
						if (buf.equals(successResponse)) {
							LOG.debug("Authentication successful");
							markSuccess();
						} else {
							LOG.error("Authentication failed");
							markFailure();
						}
					} else if (failureResponse != null && buf.length() == failureResponse.length()) {
						sb.delete(0, sb.length());
						if (!buf.equals(failureResponse)) {
							LOG.debug("Authentication successful");
							markSuccess();
						} else {
							LOG.error("Authentication failed");
							markFailure();
						}
					}
				} else {
					//check if the output we received is matching password prompt
					if (passwordPrompt.startsWith(buf)) {
						if (passwordPrompt.equals(sb.toString())) {
							if (passwordProvider == null) {
								//send some random password to complete the input
								getSession().close();
								throw new SshSessionlException("No password provider given");
							}
							flushBufferQuiet(getIn());
							sb.delete(0, sb.length());
							passwordSent = true;
							sendCommand(passwordProvider.getPassword());
							LOG.debug("Password sent");
						}
					}else {
						if (buf.length() > 1) {
							//password not needed
							LOG.debug("Athentication not needed");
							//printBuffer(getIn());
							markSuccess();
						}
					}
				}
			}
		};
		String command=cmd.getCommandLine();
		sendCommand(command);
		return reader.readCommandOutput(command);
	}

}
