package com.dbsystel.devops.remoteutils;

public class RemoteUtilException extends Exception {
	private static final long serialVersionUID = 7785427480256816966L;

	public RemoteUtilException(String message, Throwable throwable) {
		super(message, throwable);
	}

	public RemoteUtilException(String message) {
		super(message);
	}

	public RemoteUtilException(Throwable throwable) {
		super(throwable);
	}

}
