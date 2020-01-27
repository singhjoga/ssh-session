package com.thetechnovator.ssh;

public class SshSessionlException extends Exception {
	private static final long serialVersionUID = 7785427480256816966L;

	public SshSessionlException(String message, Throwable throwable) {
		super(message, throwable);
	}

	public SshSessionlException(String message) {
		super(message);
	}

	public SshSessionlException(Throwable throwable) {
		super(throwable);
	}

}
