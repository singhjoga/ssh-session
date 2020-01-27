package com.thetechnovator.ssh;

public class CommandExecutor extends AbstractCommandExecutor{

	public CommandExecutor(SshSession session) {
		super(session);
	}
	public int exec(SshCommand cmd) throws SshSessionlException {
		SshInput2OutputPipe out = new SshInput2OutputPipe(cmd, this.getSession());
		String realCmd=getCommandWithEndMarker(cmd.getCommandLine());
		//first flush any previous buffer
		out.flushBufferQuiet(out.getSession().getShell().getInvertedOut());
		sendCommand(realCmd);
		return out.readCommandOutput(realCmd);
	}
}
