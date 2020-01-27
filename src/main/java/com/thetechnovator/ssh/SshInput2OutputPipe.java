package com.thetechnovator.ssh;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Redirects the output from remote shell to the given out stream.
 * 
 * @author JogaSingh
 *
 */
public class SshInput2OutputPipe extends SshInputReader {
	private boolean write2StdOut=false;
	private OutputStream out;
	public SshInput2OutputPipe(SshCommand cmd,SshSession session) {
		super(cmd,session);
		this.write2StdOut=cmd.isWriteOutputToConsole();
		this.out=cmd.getOutputStream();
	}

	@Override
	public void write(int c) throws SshSessionlException {
		try {
			if (write2StdOut) {
				System.out.write(c);
			}
			if (out != null) {
				out.write(c);
			}
		} catch (IOException e) {
			throw new SshSessionlException(e);
		}
	}

	@Override
	public void writeCommand(String str) throws SshSessionlException {
		try {
			if (write2StdOut) {
				System.out.write(str.getBytes());
			}
			//do not write command to the output stream
		} catch (IOException e) {
			throw new SshSessionlException(e);
		}
	}

}
