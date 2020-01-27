package com.thetechnovator.ssh;

import java.io.OutputStream;

/**
 * Command line with its various options
 * 
 * This object can be constructed intuitively using the {@link Builder}.
 * 
 * @author JogaSingh
 *
 */
public class SshCommand {
	public static final long DEFAULT_INACTIVIT_TIMEOUT = 15 * 60 * 1000; //15 minute
	public static final long DEFAULT_MAX_EXECUTION_TIME = 120 * 60 * 1000; //120 minutes
	
	private String commandLine;
	private long maxExecutionTimeMs=DEFAULT_MAX_EXECUTION_TIME;
	private long maxInactivityTimeMs=DEFAULT_INACTIVIT_TIMEOUT;
	private OutputStream outputStream;
	private boolean writeOutputToConsole=true;
	/**
	 * Constructor with only 'commandLine' parameter. It sets 'writeOutputToConsole' to 'true'.
	 * 
	 * @param commandLine
	 */
	public SshCommand(String commandLine) {
		this(commandLine,null,true);
	}
	/**
	 * 	 It sets 'writeOutputToConsole' to 'true'.
	 * 
	 * @param commandLine
	 * @param outputStream
	 */
	public SshCommand(String commandLine, OutputStream outputStream) {
		this(commandLine,outputStream,true);
	}
	
	/**
	 * Constructor with commonly used parameters
	 * 
	 * @param commandLine - command to be executed-
	 * @param outputStream - output will also be written this output stream.
	 * @param writeOutputToConsole - when set to 'true', writes console output to Standard Output.
	 */
	public SshCommand(String commandLine, OutputStream outputStream, boolean writeOutputToConsole) {
		super();
		this.commandLine = commandLine;
		this.outputStream = outputStream;
		this.writeOutputToConsole = writeOutputToConsole;
	}
	public String getCommandLine() {
		return commandLine;
	}
	public void setCommandLine(String commandLine) {
		this.commandLine = commandLine;
	}
	
	/**
	 * Default value is two hours.
	 * 
	 * @return - max execution time in milliseconds.
	 * 
	 * @see #setMaxExecutionTimeMs(long)
	 */
	public long getMaxExecutionTimeMs() {
		return maxExecutionTimeMs;
	}
	
	/**
	 * Sets the maximum time the command can take to complete.
	 * 
	 * Default value is two hours. If the command does not completes within the time, command is terminated by closing the shell session.
	 * Therefore, no more commands can be executed in the same session. 
	 * 
	 * @param maxExecutionTimeMs - time in milliseconds.
	 */
	public void setMaxExecutionTimeMs(long maxExecutionTimeMs) {
		this.maxExecutionTimeMs = maxExecutionTimeMs;
	}
	
	/**
	 * Returns the maximum time the console output from the command can be inactive. Default value is 15 minutes.
	 * 
	 * @return - inactivity timeout in milliseconds.
	 * 
	 * @see #setMaxInactivityTimeMs(long)
	 */
	public long getMaxInactivityTimeMs() {
		return maxInactivityTimeMs;
	}
	
	/**
	 * Sets the maximum time the console output from a command can be inactive. 
	 * 
	 * Default value of 15 minutes should enough for most of the cases.
	 * However, it should be increased for the commands which do not emit any console output and take longer to execute. Command is
	 * terminated by closing the shell session if the command does not complete within the set time. Once a session is closed, no further
	 * commands can be executed using the same session. 
	 * 
	 * @param maxInactivityTimeMs - time in milliseconds.
	 */
	public void setMaxInactivityTimeMs(long maxInactivityTimeMs) {
		this.maxInactivityTimeMs = maxInactivityTimeMs;
	}
	
	/**
	 * Output Stream set using the {@link #setOutputStream(OutputStream)}
	 * 
	 * @return
	 *
	 * @see #setOutputStream(OutputStream)
	 */
	public OutputStream getOutputStream() {
		return outputStream;
	}
	
	/**
	 * This is used to get the command output in addition to writing to console with {@link #setWriteOutputToConsole(boolean)}
	 * 
	 * @param outputStream 
	 */
	public void setOutputStream(OutputStream outputStream) {
		this.outputStream = outputStream;
	}
	
	/**
	 * Whether to write the command output to standard output. Default is 'true'.
	 *  
	 * @return
	 * 
	 * @see #setWriteOutputToConsole(boolean)
	 * 
	 */
	public boolean isWriteOutputToConsole() {
		return writeOutputToConsole;
	}
	
	/**
	 * Sets whether to write the command output to standard output. Default is 'true'.
	 * 
	 * @param writeOutputToConsole
	 */
	public void setWriteOutputToConsole(boolean writeOutputToConsole) {
		this.writeOutputToConsole = writeOutputToConsole;
	}

	public static class Builder {
		private SshCommand cmd;
		private OutputOptions options;
		public Builder() {
			this(null);
		}
		public Builder(String cmdLine) {
			this.cmd = new SshCommand(cmdLine);
			this.options=new OutputOptions(cmd, this);
		}
		public static Builder getInstance(String cmdLine) {
			return new Builder(cmdLine);
		}
		/**
		 * @param cmdLine
		 * @return
		 * 
		 * @see SshCommand#setCommandLine(String)
		 */
		public Builder commandLine(String cmdLine) {
			cmd.setCommandLine(cmdLine);
			return this;
		}
		
		/**
		 * @param exectutionTimeoutMs
		 * @return
		 * 
		 * @see SshCommand#setMaxExecutionTimeMs(long)
		 */
		public Builder maxExecutionTimeMs(long exectutionTimeoutMs) {
			cmd.setMaxExecutionTimeMs(exectutionTimeoutMs);
			return this;
		}
		
		/**
		 * @param inactivityTimeoutMs
		 * @return
		 * 
		 * @see SshCommand#setMaxInactivityTimeMs(long)
		 */
		public Builder maxInactivityTimeMs(long inactivityTimeoutMs) {
			cmd.setMaxInactivityTimeMs(inactivityTimeoutMs);
			return this;
		}
		public OutputOptions output() {
			return options;
		}
		public SshCommand build() {
			return cmd;
		}
	}
	public static class OutputOptions {
		private SshCommand cmd;
		private Builder builder;
		public OutputOptions(SshCommand cmd, Builder builder) {
			this.cmd = cmd;
			this.builder=builder;
		}

		/**
		 * @param writeToConsole
		 * @return
		 * 
		 * @see SshCommand#setWriteOutputToConsole(boolean)
		 */
		public OutputOptions writeToConsole(boolean writeToConsole) {
			cmd.setWriteOutputToConsole(writeToConsole);
			return this;
		}
		
		/**
		 * @param out
		 * @return
		 * 
		 * @see SshCommand#setOutputStream(OutputStream)
		 */
		public OutputOptions writeToOutputStream(OutputStream out) {
			cmd.setOutputStream(out);
			return this;
		}
		public Builder builder() {
			return builder;
		}
	}
}