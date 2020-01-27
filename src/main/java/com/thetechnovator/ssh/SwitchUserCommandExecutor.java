package com.thetechnovator.ssh;

public class SwitchUserCommandExecutor extends PasswordInputCommand{

	public SwitchUserCommandExecutor(SshSession session, PasswordProvider passwordProvider) {
		//if shell prompts back 'su: xxxx'. It is failure. On success nothing is returned
		super(session, "Password:", passwordProvider, null, "su:");
	}

}
