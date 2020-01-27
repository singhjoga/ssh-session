package com.thetechnovator.ssh;

import static com.thetechnovator.ssh.Constants.SUCCESS;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.client.scp.ScpClient;
import org.apache.sshd.client.scp.ScpClientCreator;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates a SSH Session using Apache SSHD
 * 
 * @author JogaSingh
 *
 */
public class SshSession {

	private static final Logger LOG = LoggerFactory.getLogger(SshSession.class);
	private String host;
	private int port = 22;
	private String username;
	private String password = null;
	private String sshKey = null;
	private PasswordProvider sudoPasswordProvider = null;
	private PasswordProvider suPasswordProvider = null;

	private boolean sudoVerified = false;
	private SshClient client;
	private ChannelShell channel;
	private ClientSession session;
	private boolean isOpen = false;

	private SshSession() {

	}

	/**
	 * Connects to the remote host and authenticates
	 * 
	 * @throws SshSessionlException
	 */
	public synchronized void connect() throws SshSessionlException {
		if (isOpen) {
			LOG.info("Session is already open. Doing nothing");
			return;
		}
		client = SshClient.setUpDefaultClient();
		client.start();
		try {
			LOG.info("Connecting to " + host + " using username: " + username);
			session = client.connect(username, host, port).verify(7L, TimeUnit.SECONDS).getSession();
		} catch (IOException e) {
			throw new SshSessionlException("Connection Error: " + e.getMessage(), e);
		}
		if (password != null) {
			session.addPasswordIdentity(password);
		} else if (sshKey != null) {
			session.addPublicKeyIdentity(Utils.privateKey2KeyPair(sshKey));
		}
		try {
			LOG.info("Authenticating " + (password != null ? " using password" : (sshKey != null ? " using private key" : "")));
			session.auth().verify(7L, TimeUnit.SECONDS);
		} catch (IOException e) {
			throw new SshSessionlException("Authentication Error: " + e.getMessage(), e);
		}

		try {
			LOG.info("Creating shell channel");
			channel = session.createShellChannel();
			channel.open().verify(9L, TimeUnit.SECONDS);
			sleep(1000);
			SshInputReader.flushBuffer(channel.getInvertedOut());
			LOG.info("Ready to accept shell commands");
			isOpen = true;
		} catch (IOException e) {
			throw new SshSessionlException("Channel Error: " + e.getMessage(), e);
		}

	}

	/**
	 * Executes the given command writes the output to Standard Output.
	 * 
	 * Command can also include 'sudo' and 'su' commands. Needed password for these
	 * commands is taken from the respective PasswordProvider.
	 * 
	 * @param command
	 * @return exit status. On successful execution '0' is returned, otherwise the
	 *         exit code from the command.
	 */
	public int exec(String command) throws SshSessionlException {
		// execute any other command
		SshCommand sshCommand = new SshCommand(command);
		return exec(sshCommand);
	}

	public int exec(SshCommand sshCommand) throws SshSessionlException {
		if (!isOpen) {
			throw new SshSessionlException("Session is not connected. Call the 'connect' method first");
		}
		String cmd = sshCommand.getCommandLine().trim();
		LOG.debug("Executing command: " + cmd);
		CommandAttribs cmdAttribs = getCommandAttribs(cmd);
		if (cmdAttribs.sudo && !sudoVerified) {
			doSudoCheck();
			sudoVerified = true;
		}
		if (cmdAttribs.su) {
			doSwitchUser(cmd);
			return 0;
		} else if (cmdAttribs.exit) {
			doExit(cmd);
			return 0;
		}
		CommandExecutor executor = new CommandExecutor(this);
		int statusCode = executor.exec(sshCommand);
		LOG.debug("Status Code: " + statusCode);
		return statusCode;
	}

	public synchronized void close() {
		if (!isOpen) {
			LOG.info("Session is not open. Doing nothing");
			return;
		}
		isOpen = false;
		sudoVerified = false;
		channel.close(true);
		session.close(true);
		client.close(true);
	}

	public void upload(File localFile, String remotePath) throws SshSessionlException {
		try {
			ScpClientCreator creator = ScpClientCreator.instance();
			ScpClient client = creator.createScpClient(session);
			client.upload(localFile.toPath(), remotePath);
		} catch (IOException e) {
			throw new SshSessionlException(e);
		}
	}

	public void download(File localFile, String remotePath) throws SshSessionlException {
		try {
			ScpClientCreator creator = ScpClientCreator.instance();
			ScpClient client = creator.createScpClient(session);
			client.download(remotePath, localFile.toPath());
		} catch (IOException e) {
			throw new SshSessionlException(e);
		}
	}

	public boolean isOpen() {
		return isOpen;
	}

	ChannelShell getShell() {
		return channel;
	}

	private void doSwitchUser(String cmd) throws SshSessionlException {
		SwitchUserCommandExecutor cmdExecutor = new SwitchUserCommandExecutor(this, suPasswordProvider);
		SshCommand sshCmd = new SshCommand(cmd);
		int status = cmdExecutor.exec(sshCmd);
		if (status != SUCCESS) {
			throw new SshSessionlException("Switch user command failed: Authentication failure");
		}
	}

	private void doExit(String cmd) throws SshSessionlException {
		StandardOutputCommand cmdExecutor = new StandardOutputCommand(this, "logout", null);
		SshCommand sshCmd = new SshCommand(cmd);
		int status = cmdExecutor.exec(sshCmd);
		if (status != SUCCESS) {
			throw new SshSessionlException("Exit command failed");
		}
	}

	private void doSudoCheck() throws SshSessionlException {
		PasswordProvider passProvider = sudoPasswordProvider;
		if (passProvider == null) {
			// by default the password for the user
			passProvider = new PasswordProvider() {
				@Override
				public String getPassword() {
					return password;
				}
			};
		}
		SudoCommandExecutor cmdExecutor = new SudoCommandExecutor(this, passProvider);
		int status = cmdExecutor.exec();
		if (status != SUCCESS) {
			close();
			throw new SshSessionlException("Sudo command failed: Authentication failure");
		}
	}

	static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private CommandAttribs getCommandAttribs(String command) {
		CommandAttribs attribs = new CommandAttribs();
		attribs.sudo = command.startsWith("sudo ");
		// check if there is switch user command
		int startPos = attribs.sudo ? 5 : 0;
		if (command.length() > startPos + 1) {
			String subCmd = command.substring(startPos).trim();
			attribs.su = subCmd.startsWith("su ");
			attribs.exit = subCmd.startsWith("exit");
		}

		return attribs;
	}

	/**
	 * Builder for SshSession
	 * 
	 * @author JogaSingh
	 *
	 */
	public static class Builder {
		private SshSession session;
		private File keyFile = null;

		public Builder() {
			session = new SshSession();
		}

		public static Builder getInstance() {
			Builder instance = new Builder();
			return instance;
		}

		public Builder host(String host) {
			session.host = host;
			return this;
		}

		public Builder username(String username) {
			session.username = username;
			return this;
		}

		public Builder port(int port) {
			session.port = port;
			return this;
		}

		public Builder password(String password) {
			session.password = password;
			return this;
		}

		public Builder sshKey(String sshKey) {
			session.sshKey = sshKey;
			return this;
		}

		public Builder sshKeyFile(File keyFile) {
			this.keyFile = keyFile;
			return this;
		}

		public Builder sudoPasswordProvider(PasswordProvider sudoPasswordProvider) {
			session.sudoPasswordProvider = sudoPasswordProvider;
			return this;
		}

		public Builder switchUserPasswordProvider(PasswordProvider suPasswordProvider) {
			session.suPasswordProvider = suPasswordProvider;
			return this;
		}

		public SshSession build() throws SshSessionlException {
			if (keyFile != null) {
				try {
					session.sshKey = FileUtils.readFileToString(keyFile, "UTF-8");
				} catch (IOException e) {
					throw new SshSessionlException(e);
				}
			}

			return session;
		}
	}

	private static class CommandAttribs {
		private boolean sudo = false;
		private boolean su = false;
		private boolean exit = false;
	}
}
