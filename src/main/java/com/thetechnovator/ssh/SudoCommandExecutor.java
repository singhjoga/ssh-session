package com.thetechnovator.ssh;

public class SudoCommandExecutor extends PasswordInputCommand{
	private static final String SUDO_CMD = getCommandWithEndMarker("sudo echo OK");
	public SudoCommandExecutor(SshSession session, PasswordProvider passwordProvider) {
		
		//Command returns OK on success (we are echoing OK above in the command)
		super(session, "[sudo] password for ", passwordProvider, "OK", null);
	}
	public int exec() throws SshSessionlException {
		SshCommand cmd = new SshCommand(SUDO_CMD);
		int status= super.exec(cmd);
		return status;
	}
	
}
