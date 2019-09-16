package com.dbsystel.devops.remoteutils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dbsystel.devops.java.common.utils.FileUtil;
import com.dbsystel.devops.remoteutils.SshUtil.ExecResult;
import com.dbsystel.devops.remoteutils.SshUtil.SshCommand;
import com.jcraft.jsch.Session;

public class RemoteSession {
	public static final Charset UTF8 = Charset.forName("UTF-8");
	public static final String FILE_TIME = "yyyy-MM-dd HH:mm:ss.SSSSSSSSS";
	public static final String TIMESTAMP = "yyyyMMddHHmmssSSS";
	private static final SimpleDateFormat timestampFormat = new SimpleDateFormat(TIMESTAMP);
	private static final SimpleDateFormat fileTimeFormat = new SimpleDateFormat(FILE_TIME);
	private static final Logger LOG = LoggerFactory.getLogger(RemoteSession.class);
	private String hostname;
	private String username;
	private String password;
	private String key;
	private String suUsername;
	private String suPassword;
	private Session sshSession;
	private static final String FILE_NOT_FOUND_MSG = "No such file or directory";
	public RemoteSession(String hostname, String username, String password, String key, String suUsername,
			String suPassword) {
		super();
		this.hostname = hostname;
		this.username = username;
		this.password = password;
		this.key = key;
		this.suUsername = suUsername;
		this.suPassword = suPassword;
	}

	private void connect() throws RemoteUtilException {
		if (sshSession == null) {
			LOG.info("Opening session");
			if (StringUtils.isEmpty(key)) {
				sshSession = SshUtil.createSSHsession(hostname, username, password,false);				
			}else {				
				sshSession = SshUtil.createSSHsession(hostname, username, key, true);
			}
		}
	}
	public void disconnect() {
		if (sshSession != null) {
			LOG.info("Closing session");
			sshSession.disconnect();
		}
	}
	public RemoteResult execute(String command) throws RemoteUtilException {
		return execute(command, false,-1L,-1L);
	}
	public RemoteResult execute(String command, long maxExecutionTimeout) throws RemoteUtilException {
		return execute(command, false,maxExecutionTimeout,-1L);
	}
	public RemoteResult execute(String command, boolean returnOutput) throws RemoteUtilException {
		return execute(command, returnOutput, -1, -1);
	}
	public RemoteResult execute(String command, boolean returnOutput, long maxExecutionTimeout,long inactivityTimeout) throws RemoteUtilException {
		if (returnOutput) {
			try {
				File outFile = File.createTempFile("remote", ".out");
				RemoteResult result = execute(command, returnOutput, outFile,maxExecutionTimeout,inactivityTimeout);
				outFile.delete();
				return result;
			} catch (Exception e) {
				throw new RemoteUtilException(e.getMessage(), e);
			}
		} else {
			return execute(command, returnOutput, null,maxExecutionTimeout,inactivityTimeout);
		}
	}
	public RemoteResult execute(String command, boolean returnOutput, File paramOutputFile,long maxExecutionTimeout,long inactivityTimeout) throws RemoteUtilException {
		return execute(command, returnOutput, paramOutputFile, maxExecutionTimeout, inactivityTimeout,true);
	}

	public RemoteResult execute(String command, boolean returnOutput, File paramOutputFile,long maxExecutionTimeout,long inactivityTimeout, boolean doSu) throws RemoteUtilException {
		OutputStream os = null;
		File outFile = paramOutputFile;
		SshCommand sshCmd;
		if (doSu) {
			sshCmd = new SshCommand(command, suUsername, suPassword, StringUtils.isEmpty(suPassword));
		}else {
			sshCmd = new SshCommand(command, null, null, StringUtils.isEmpty(suPassword));
		}
		if (maxExecutionTimeout != -1) {
			sshCmd.setMaxExecutionTimeMs(maxExecutionTimeout);
		}
		if (inactivityTimeout != -1) {
			sshCmd.setInactivityTimeoutMs(inactivityTimeout);
		}
		
		try {
			if (returnOutput) {
				if (outFile == null) {
					outFile = File.createTempFile("remote", ".out");
				}
				os = new FileOutputStream(outFile);
				sshCmd.setOutputStream(os);
				sshCmd.setShowOutput(false);
			}
			connect();
			ExecResult sshResult = SshUtil.exec(sshSession, sshCmd);
			FileUtil.closeQuietly(os);
			String output=null;
			if (returnOutput) {
				output = FileUtil.readFileAsString(outFile.getAbsolutePath(), UTF8);
			}
			RemoteResult result= new RemoteResult(sshResult,output);
			//LOG.info("Exit Status="+sshResult.getExitStatus()+", Exit Status 2="+result.getExitStatus());
			return result;
		} catch (IOException e) {
			throw new RemoteUtilException(e.getMessage(), e);
		}finally {
			FileUtil.closeQuietly(os);
			if (returnOutput && paramOutputFile==null) {
				//A temp file was created. Delete it
				outFile.delete();
			}
		}
	}
	private void assertSuccess(ExecResult result) throws RemoteUtilException{
		if (result.getExitStatus() != 0) {
			throw new RemoteUtilException(result.getError());
		}
	}
	public void copyTo(File fromLocalFile, String toRemoteFilePath) throws RemoteUtilException{
		SshUtil.scpTo(sshSession, fromLocalFile, toRemoteFilePath);
		//make it world writable
		execute("chmod 777 "+toRemoteFilePath, false, null, 100000, 100000, false);
	}
	public void copyFrom(String fromRemoteFilePath, File toLocalFile) throws RemoteUtilException{
		SshUtil.scpFrom(sshSession, toLocalFile, fromRemoteFilePath);
	}
	public String readFile(String remoteFilePath) throws RemoteUtilException{
		String command = "cat "+remoteFilePath;
		RemoteResult result = execute(command, true);
		if (result.isFailed()) {
			throw new RemoteUtilException("Error reading file: "+result.getPossibleError());
		}else {
			return result.getOutput();
		}
	}
	public String grep(String searchToken, String remoteFilePath) throws RemoteUtilException{
		String command = "grep "+searchToken+" "+remoteFilePath;
		RemoteResult result = execute(command, true);
		if (result.isFailed()) {
			throw new RemoteUtilException("Error reading file: "+result.getPossibleError());
		}else {
			return result.getOutput();
		}
	}
	public String grepLast(String searchToken, String remoteFilePath) throws RemoteUtilException{
		String command = "tac "+remoteFilePath+" | grep -m 1 "+searchToken;
		RemoteResult result = execute(command, true);
		if (result.isFailed()) {
			throw new RemoteUtilException("Error reading file: "+result.getPossibleError());
		}else {
			return result.getOutput();
		}
	}
	public String tail(String remoteFilePath, int lineCount) throws RemoteUtilException{
		String command = "tail -n "+lineCount+" "+remoteFilePath;
		RemoteResult result = execute(command, true);
		if (result.isFailed()) {
			throw new RemoteUtilException("Error reading file: "+result.getPossibleError());
		}else {
			return result.getOutput();
		}
	}
	public void createDir(String remoteFilePath) throws RemoteUtilException{
		String command = "mkdir -p "+remoteFilePath;		
		RemoteResult result = execute(command);
		if (result.isFailed()) {
			throw new RemoteUtilException("Error creating directory: "+result.getPossibleError());
		}
		command = "chmod 777 "+remoteFilePath;		
		result = execute(command);
		if (result.isFailed()) {
			throw new RemoteUtilException("Error setting directory permissions: "+result.getPossibleError());
		}
	}
	public String createTempDir() throws RemoteUtilException{
		String dir = "/tmp/remote_session_"+timestampFormat.format(new Date());
		String command = "mkdir -p "+dir;		
		RemoteResult result = execute(command);
		if (result.isFailed()) {
			throw new RemoteUtilException("Error creating directory: "+result.getPossibleError());
		}
		command = "chmod 777 "+dir;		
		result = execute(command);
		if (result.isFailed()) {
			throw new RemoteUtilException("Error setting directory permissions: "+result.getPossibleError());
		}
		
		return dir;
	}
	public List<RemoteFile> findFiles(String remotePath, String nameExpr, String options) throws RemoteUtilException{
		//First check if files are returned at all
		String findCmd="find "+remotePath+" -name "+nameExpr+" "+options;
		String command = findCmd+" | xargs -r ls -l --time-style=full-iso";
		RemoteResult result = execute(command, true);
		if (result.isFailed()) {
			throw new RemoteUtilException("Error reading file: "+result.getPossibleError());
		}
		return parseFiles(result.getOutput());
		
	}
	public List<RemoteFile> listFiles(String remotePathExpr) throws RemoteUtilException{
		String command = "ls -l --time-style=full-iso "+remotePathExpr;
		RemoteResult result = execute(command, true);
		if (result.isFailed()) {
			throw new RemoteUtilException("Error reading file: "+result.getPossibleError());
		}
		return parseFiles(result.getOutput());
		
	}
	private List<RemoteFile> parseFiles(String str) throws RemoteUtilException{
		/*
		 * Parse output in the below format
-rw-r--r-- 1 twix16 twix16 10151867 2019-03-14 12:32:51.000000000 +0100 /app/twix16/tibco/ae/tra/domain/DBE_AE_001/application/logs/asl-ndm-abrp-asl-ndm-abrp_1.log
-rw-r--r-- 1 twix16 twix16 30715133 2018-11-13 11:41:22.000000000 +0100 /app/twix16/tibco/ae/tra/domain/DBE_AE_001/application/logs/asl-ndm-abrp-asl-ndm-abrp_1.log.1
		 */
		List<RemoteFile> list = new ArrayList<>();
		if (StringUtils.isEmpty(str)) {
			return list;
		}
/*		
		for (int i=0;i < str.length();i++) {
			int c = str.charAt(i);
			System.out.print(str.substring(i, i+1)+"=");
			System.out.print(c+" ");
		}
	*/	
		String[] lines = StringUtils.split(str,(char)10);
		for (String line: lines) {
			LOG.debug("Parsing line: "+line);
			if (line.startsWith("total")) {
				continue;
			}
			String[] tokens = StringUtils.split(line," ");
			if (tokens.length < 9) {
				throw new RemoteUtilException("Cannot parse remote list file output: "+line+". Tokens: "+StringUtils.join(tokens,","));
			}
			try {
				boolean isDir = tokens[0].charAt(0)=='d';
				RemoteFile file = new RemoteFile(tokens[8], Long.parseLong(tokens[4]), fileTimeFormat.parse(tokens[5]+" "+tokens[6]),isDir);
				list.add(file);
			} catch (NumberFormatException e) {
				throw new RemoteUtilException(e);
			} catch (ParseException e) {
				throw new RemoteUtilException(e);
			}
		}
		
		return list;
	}
	public void removeFile(String remoteFilePath) throws RemoteUtilException{
		String command;
		command = "rm "+remoteFilePath;
		RemoteResult result = execute(command);
		if (result.isFailed()) {
			throw new RemoteUtilException("Error removing file: "+result.getPossibleError());
		}
	}
	public void removeDir(String remoteFilePath, boolean recursive) throws RemoteUtilException{
		String command;
		if (recursive) {
			command = "rm -rf "+remoteFilePath;
		}else {
			command = "rmdir "+remoteFilePath;
		}
		RemoteResult result = execute(command);
		if (result.isFailed()) {
			throw new RemoteUtilException("Error removing directory: "+result.getPossibleError());
		}
	}
	public boolean fileExists(String remoteFilePath) throws RemoteUtilException{
		String command = "ls "+remoteFilePath;
		RemoteResult result = execute(command, false);
		if (result.isFailed()) {
			if (result.getPossibleError().contains(FILE_NOT_FOUND_MSG)) {
				return false;
			}
			throw new RemoteUtilException("Error : "+result.getPossibleError());
		}else {
			return true;
		}
	}
	public class RemoteResult {
		private int exitStatus;
		private String possibleError;
		private String output;
		public RemoteResult(ExecResult execResult, String output) {
			super();
			this.exitStatus = execResult.getExitStatus();
			this.possibleError = StringUtils.isEmpty(execResult.getError())? execResult.getLastOutput():execResult.getError();
			this.output = output;
		}

		public int getExitStatus() {
			return exitStatus;
		}

		public String getPossibleError() {
			return possibleError;
		}

		public String getOutput() {
			return output;
		}

		public void setOutput(String output) {
			this.output = output;
		}
		
		public boolean isFailed() {
			return exitStatus != 0;
		}
	}
}
