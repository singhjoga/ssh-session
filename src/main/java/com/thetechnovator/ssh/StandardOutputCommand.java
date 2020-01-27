package com.thetechnovator.ssh;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public  class StandardOutputCommand extends AbstractCommandExecutor {
	private static final Logger LOG = LoggerFactory.getLogger(StandardOutputCommand.class);
	private String response;
	private boolean isSuccessMatching;

	public StandardOutputCommand(SshSession session, String successResponse, String failureResponse) {
		super(session);
		this.response=successResponse != null?successResponse:failureResponse;
		this.isSuccessMatching=successResponse != null;
	}

	public int exec(SshCommand cmd) throws SshSessionlException {
		StringBuilder sb = new StringBuilder();
		SshInputReader reader = new SshInputReader(cmd, getSession()) {
			@Override
			public void writeCommand(String str) throws SshSessionlException {
			}

			@Override
			public void write(int c) throws SshSessionlException {
				if (isNewLine(c)) {
					return;
				}
				sb.append((char) c);
				String buf = sb.toString();
				if (response != null && buf.length() == response.length()) {
					sb.delete(0, sb.length());
					if (buf.equals(response)) {
						setStatus(isSuccessMatching?Constants.SUCCESS:Constants.FAILURE);
						LOG.debug("Response matched");
					} else {
						markFailure();
						LOG.debug("Response did not match");
					}
				}
			}
		};
		String command=cmd.getCommandLine();
		sendCommand(command);
		return reader.readCommandOutput(command);
	}

}
