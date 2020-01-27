package com.thetechnovator.ssh;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SshHelper {
	public static final Charset UTF8 = Charset.forName("UTF-8");
	public static final String FILE_TIME = "yyyy-MM-dd HH:mm:ss.SSSSSSSSS";
	public static final String TIMESTAMP = "yyyyMMddHHmmssSSS";
	private static final SimpleDateFormat timestampFormat = new SimpleDateFormat(TIMESTAMP);
	private static final SimpleDateFormat fileTimeFormat = new SimpleDateFormat(FILE_TIME);
	private static final String FILE_NOT_FOUND_MSG = "No such file or directory";
	private static final Logger LOG = LoggerFactory.getLogger(SshHelper.class);
	private String hostname;
	private String username;
	private String password;
	private String key;
	private SshSession sshSession;
	private boolean connectUserRequiresSudo;
	private String suUsername;
	private String suPassword;
	private boolean switchUserRequiresSudo;
	private boolean doSudo;
	public SshHelper(String hostname, String username, String password, String key, boolean connectUserRequiresSudo, String suUsername, String suPassword, boolean switchUserRequiresSudo) {
		super();
		this.hostname = hostname;
		this.username = username;
		this.password = password;
		this.key = key;
		this.connectUserRequiresSudo = connectUserRequiresSudo;
		this.suPassword = suPassword;
		this.suUsername = suUsername;
		this.switchUserRequiresSudo = switchUserRequiresSudo;
	}

	public void connect() throws SshSessionlException {
		if (sshSession != null) {
			return;
		}
		SshSession.Builder builder = SshSession.Builder.getInstance().host(hostname).username(username);
		LOG.info("Opening session");
		if (StringUtils.isEmpty(key)) {
			builder.password(password);
		}else {	
			builder.sshKey(key);
		}
		if (StringUtils.isEmpty(suPassword)) {
			builder.switchUserPasswordProvider(new PasswordProvider() {
				@Override
				public String getPassword() {
					return suPassword;
				}
			});
		}
		sshSession = builder.build();
		sshSession.connect();
		doSudo=connectUserRequiresSudo;
		if (StringUtils.isNotEmpty(suUsername)) {
			switchUser();
		}
	
	}
	
	public void disconnect() {
		if (sshSession != null) {
			LOG.info("Closing session");
			sshSession.close();
		}
	}
	private void switchUser() throws SshSessionlException{
		LOG.info("Switching user to "+suUsername);
		String cmd=null;
		if (this.connectUserRequiresSudo) {
			cmd="sudo su - "+username;
		}else {
			cmd="su - "+username;
		}
		sshSession.exec(cmd);
		doSudo=switchUserRequiresSudo;
	}
	public ExecResult execute(String command) throws SshSessionlException {
		return execute(command, false,-1L,-1L);
	}
	public ExecResult execute(String command, long maxExecutionTimeout) throws SshSessionlException {
		return execute(command, false,maxExecutionTimeout,-1L);
	}
	public ExecResult execute(String command, boolean returnOutput) throws SshSessionlException {
		return execute(command, returnOutput, -1, -1);
	}

	public ExecResult execute(String command, boolean returnOutput, long maxExecutionTimeout,long inactivityTimeout) throws SshSessionlException {
		OutputStream os = null;
		String cmdLine=command.trim();
		if (doSudo) {
			if (!cmdLine.startsWith("sudo")) {
				cmdLine = "sudo "+cmdLine;
			}
		}
		SshCommand sshCmd = new SshCommand(cmdLine);
		
		if (maxExecutionTimeout != -1) {
			sshCmd.setMaxExecutionTimeMs(maxExecutionTimeout);
		}
		if (inactivityTimeout != -1) {
			sshCmd.setMaxInactivityTimeMs(inactivityTimeout);
		}
		
		File outFile = null;
		try {
			if (returnOutput) {
				if (outFile == null) {
					outFile = File.createTempFile("remote", ".out");
				}
				os = new FileOutputStream(outFile);
				sshCmd.setOutputStream(os);
				sshCmd.setWriteOutputToConsole(false);
			}
			int exitCode=sshSession.exec(sshCmd);
			connect();
			String output=null;
			if (returnOutput) {
				output = FileUtils.readFileToString(outFile, UTF8);
			}
			ExecResult result= new ExecResult(exitCode,output);
			//LOG.info("Exit Status="+sshResult.getExitStatus()+", Exit Status 2="+result.getExitStatus());
			return result;
		} catch (IOException e) {
			throw new SshSessionlException(e.getMessage(), e);
		}finally {
			Utils.closeQuietly(os);
			if (returnOutput) {
				//A temp file was created. Delete it
				outFile.delete();
			}
		}
	}

	public void upload(File fromLocalFile, String toRemoteFilePath) throws SshSessionlException{
		sshSession.upload(fromLocalFile, toRemoteFilePath);
	}
	public void download(String fromRemoteFilePath, File toLocalFile) throws SshSessionlException{
		sshSession.download(toLocalFile, fromRemoteFilePath);
	}
	public String readFile(String remoteFilePath) throws SshSessionlException{
		String command = "cat "+remoteFilePath;
		ExecResult result = execute(command, true);
		if (result.isFailed()) {
			throw new SshSessionlException("Error reading file: "+result.getOutputLastLine());
		}else {
			return result.getOutput();
		}
	}
	public String grep(String searchToken, String remoteFilePath) throws SshSessionlException{
		String command = "grep "+searchToken+" "+remoteFilePath;
		ExecResult result = execute(command, true);
		if (result.isFailed()) {
			throw new SshSessionlException("Error reading file: "+result.getOutputLastLine());
		}else {
			return result.getOutput();
		}
	}
	public String grepLast(String searchToken, String remoteFilePath) throws SshSessionlException{
		String command = "tac "+remoteFilePath+" | grep -m 1 "+searchToken;
		ExecResult result = execute(command, true);
		if (result.isFailed()) {
			throw new SshSessionlException("Error reading file: "+result.getOutputLastLine());
		}else {
			return result.getOutput();
		}
	}
	public String tail(String remoteFilePath, int lineCount) throws SshSessionlException{
		String command = "tail -n "+lineCount+" "+remoteFilePath;
		ExecResult result = execute(command, true);
		if (result.isFailed()) {
			throw new SshSessionlException("Error reading file: "+result.getOutputLastLine());
		}else {
			return result.getOutput();
		}
	}
	public void createDir(String remoteFilePath) throws SshSessionlException{
		String command = "mkdir -p "+remoteFilePath;		
		ExecResult result = execute(command);
		if (result.isFailed()) {
			throw new SshSessionlException("Error creating directory: "+result.getOutputLastLine());
		}
	}
	public String createTempDir() throws SshSessionlException{
		String dir = "/tmp/remote_session_"+timestampFormat.format(new Date());
		String command = "mkdir -p "+dir;		
		ExecResult result = execute(command);
		if (result.isFailed()) {
			throw new SshSessionlException("Error creating directory: "+result.getOutputLastLine());
		}
		command = "chmod 777 "+dir;		
		result = execute(command);
		if (result.isFailed()) {
			throw new SshSessionlException("Error setting directory permissions: "+result.getOutputLastLine());
		}
		
		return dir;
	}
	public List<RemoteFile> findFiles(String remotePath, String nameExpr, String options) throws SshSessionlException{
		//First check if files are returned at all
		String findCmd="find "+remotePath+" -name "+nameExpr+" "+options;
		String command = findCmd+" | xargs -r ls -l --time-style=full-iso";
		ExecResult result = execute(command, true);
		if (result.isFailed()) {
			throw new SshSessionlException("Error reading file: "+result.getOutputLastLine());
		}
		return parseFiles(result.getOutput());
		
	}
	public List<RemoteFile> listFiles(String remotePathExpr) throws SshSessionlException{
		String command = "ls -l --time-style=full-iso "+remotePathExpr;
		ExecResult result = execute(command, true);
		if (result.isFailed()) {
			throw new SshSessionlException("Error reading file: "+result.getOutputLastLine());
		}
		return parseFiles(result.getOutput());
		
	}
	private List<RemoteFile> parseFiles(String str) throws SshSessionlException{
		/*
		 * Parse output in the below format
-rw-r--r-- 1 twix16 twix16 10151867 2019-03-14 12:32:51.000000000 +0100 /app/twix16/tibco/ae/tra/domain/DBE_AE_001/application/logs/asl-ndm-abrp-asl-ndm-abrp_1.log
-rw-r--r-- 1 twix16 twix16 30715133 2018-11-13 11:41:22.000000000 +0100 /app/twix16/tibco/ae/tra/domain/DBE_AE_001/application/logs/asl-ndm-abrp-asl-ndm-abrp_1.log.1
		 */
		List<RemoteFile> list = new ArrayList<>();
		if (StringUtils.isEmpty(str)) {
			return list;
		}
		String[] lines = StringUtils.split(str,(char)10);
		for (String line: lines) {
			LOG.debug("Parsing line: "+line);
			if (line.startsWith("total")) {
				continue;
			}
			String[] tokens = StringUtils.split(line," ");
			if (tokens.length < 9) {
				throw new SshSessionlException("Cannot parse remote list file output: "+line+". Tokens: "+StringUtils.join(tokens,","));
			}
			try {
				boolean isDir = tokens[0].charAt(0)=='d';
				RemoteFile file = new RemoteFile(tokens[8], Long.parseLong(tokens[4]), fileTimeFormat.parse(tokens[5]+" "+tokens[6]),isDir);
				list.add(file);
			} catch (NumberFormatException e) {
				throw new SshSessionlException(e);
			} catch (ParseException e) {
				throw new SshSessionlException(e);
			}
		}
		
		return list;
	}
	public void removeFile(String remoteFilePath) throws SshSessionlException{
		String command;
		command = "rm "+remoteFilePath;
		ExecResult result = execute(command);
		if (result.isFailed()) {
			throw new SshSessionlException("Error removing file: "+result.getOutputLastLine());
		}
	}
	public void removeDir(String remoteFilePath, boolean recursive) throws SshSessionlException{
		String command;
		if (recursive) {
			command = "rm -rf "+remoteFilePath;
		}else {
			command = "rmdir "+remoteFilePath;
		}
		ExecResult result = execute(command);
		if (result.isFailed()) {
			throw new SshSessionlException("Error removing directory: "+result.getOutputLastLine());
		}
	}
	public boolean fileExists(String remoteFilePath) throws SshSessionlException{
		String command = "ls "+remoteFilePath;
		ExecResult result = execute(command, false);
		if (result.isFailed()) {
			if (result.getOutputLastLine().contains(FILE_NOT_FOUND_MSG)) {
				return false;
			}
			throw new SshSessionlException("Error : "+result.getOutputLastLine());
		}else {
			return true;
		}
	}
	public class ExecResult {
		private int exitCode;
		private String output;
		public ExecResult(int exitCode, String output) {
			super();
			this.exitCode = exitCode;
			this.output = output;
		}

		public int getExitStatus() {
			return exitCode;
		}

		public String getOutput() {
			return output;
		}

		public void setOutput(String output) {
			this.output = output;
		}
		
		public boolean isFailed() {
			return exitCode != 0;
		}
		public String getOutputLastLine() {
			if (output==null) {
				return "";
			}
			String line = StringUtils.substringAfterLast(output, "\n");
			if (StringUtils.isEmpty(line)) {
				line=output;
			}
			
			return line;
		}
	}
}
