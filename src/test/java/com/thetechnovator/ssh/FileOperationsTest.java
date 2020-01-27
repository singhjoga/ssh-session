package com.thetechnovator.ssh;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import com.thetechnovator.ssh.RemoteFile;
import com.thetechnovator.ssh.SshHelper;
import com.thetechnovator.ssh.SshSessionlException;

public class FileOperationsTest extends TestBase {
	static {
		loadProperties();
	}
	
	@Ignore
	public void readFileTest() throws SshSessionlException{
		SshHelper rs = new SshHelper(HOST, USERNAME, PASSWORD, null, true,SU_USERNAME, SU_PASSWORD,false);
		rs.connect();
		try {
			String contents = rs.readFile("/tmppp/test.log");
			println("Contents: "+contents);
		} catch (SshSessionlException e) {
			System.out.println("Error: "+e.getMessage());
		}
	}
	@Ignore
	public void fileExistsTest() throws SshSessionlException{
		SshHelper rs = new SshHelper(HOST, USERNAME, PASSWORD, null, true,SU_USERNAME, SU_PASSWORD,false);
		rs.connect();
		try {
			boolean exists = rs.fileExists("/tmp/test.log");
			if (exists) {
				println("File exists");
			}else {
				println("File does not exists");				
			}
		} catch (SshSessionlException e) {
			System.out.println("Error: "+e.getMessage());
		}
	}
	@Ignore
	public void tailTest()throws SshSessionlException {
		SshHelper rs = new SshHelper(HOST, USERNAME, PASSWORD, null, true,SU_USERNAME, SU_PASSWORD,false);
		rs.connect();
		try {
			String contents = rs.tail("/var/log/nearlyallmessages", 10);
			println("Contents: "+contents);
		} catch (SshSessionlException e) {
			System.out.println("Error: "+e.getMessage());
		}
	}
	@Ignore
	public void grepTest() throws SshSessionlException{
		SshHelper rs = new SshHelper(HOST, USERNAME, PASSWORD, null, true,SU_USERNAME, SU_PASSWORD,false);
		rs.connect();
		try {
			String contents = rs.grep("LDAP","/var/log/nearlyallmessages");
			println("Contents: "+contents);
		} catch (SshSessionlException e) {
			System.out.println("Error: "+e.getMessage());
		}
	}
	@Ignore
	public void grepLastTest() throws SshSessionlException{
		SshHelper rs = new SshHelper(HOST, USERNAME, PASSWORD, null, true,SU_USERNAME, SU_PASSWORD,false);
		rs.connect();
		try {
			String contents = rs.grepLast("LDAP","/var/log/nearlyallmessages");
			println("Contents: "+contents);
		} catch (SshSessionlException e) {
			System.out.println("Error: "+e.getMessage());
		}
	}
	@Test
	public void listFilesTest() throws SshSessionlException {
		SshHelper rs = new SshHelper(HOST, USERNAME, PASSWORD, null, true,SU_USERNAME, SU_PASSWORD,false);
		rs.connect();
		try {
			List<RemoteFile> files = rs.listFiles("/var/log");
			println(files.size()+" files found");
			for (RemoteFile file: files) {
				println(file.toString());
			}
		} catch (SshSessionlException e) {
			System.out.println("Error: "+e.getMessage());
		}
	}
	
}
