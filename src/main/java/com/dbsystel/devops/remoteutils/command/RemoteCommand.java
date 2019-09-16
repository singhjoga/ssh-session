package com.dbsystel.devops.remoteutils.command;

import com.dbsystel.devops.remoteutils.RemoteSession;
import com.dbsystel.devops.remoteutils.RemoteSession.RemoteResult;

public abstract class RemoteCommand {
	private RemoteResult result;
	private boolean ignoreOnFailure;
	protected RemoteSession session;
	
	
	public RemoteCommand(RemoteSession session) {
		super();
		this.session = session;
	}

	public RemoteResult getResult() {
		return result;
	}

	public void setResult(RemoteResult result) {
		this.result = result;
	}

	public boolean isIgnoreOnFailure() {
		return ignoreOnFailure;
	}

	public void setIgnoreOnFailure(boolean ignoreOnFailure) {
		this.ignoreOnFailure = ignoreOnFailure;
	}
	
	protected abstract void execute();
}
