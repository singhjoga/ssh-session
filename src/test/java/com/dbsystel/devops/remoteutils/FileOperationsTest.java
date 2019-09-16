package com.dbsystel.devops.remoteutils;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

public class FileOperationsTest extends TestBase {
	static {
		loadProperties();
	}
	
	@Ignore
	public void readFileTest() {
		RemoteSession rs = new RemoteSession(HOST, USERNAME, PASSWORD, null, SU_USERNAME, SU_PASSWORD);
		try {
			String contents = rs.readFile("/tmppp/test.log");
			println("Contents: "+contents);
		} catch (RemoteUtilException e) {
			System.out.println("Error: "+e.getMessage());
		}
	}
	@Ignore
	public void fileExistsTest() {
		RemoteSession rs = new RemoteSession(HOST, USERNAME, PASSWORD, null, SU_USERNAME, SU_PASSWORD);
		try {
			boolean exists = rs.fileExists("/tmp/test.log");
			if (exists) {
				println("File exists");
			}else {
				println("File does not exists");				
			}
		} catch (RemoteUtilException e) {
			System.out.println("Error: "+e.getMessage());
		}
	}
	@Ignore
	public void tailTest() {
		RemoteSession rs = new RemoteSession(HOST, USERNAME, PASSWORD, null, SU_USERNAME, SU_PASSWORD);
		try {
			String contents = rs.tail("/var/log/nearlyallmessages", 10);
			println("Contents: "+contents);
		} catch (RemoteUtilException e) {
			System.out.println("Error: "+e.getMessage());
		}
	}
	@Ignore
	public void grepTest() {
		RemoteSession rs = new RemoteSession(HOST, USERNAME, PASSWORD, null, SU_USERNAME, SU_PASSWORD);
		try {
			String contents = rs.grep("LDAP","/var/log/nearlyallmessages");
			println("Contents: "+contents);
		} catch (RemoteUtilException e) {
			System.out.println("Error: "+e.getMessage());
		}
	}
	@Ignore
	public void grepLastTest() {
		RemoteSession rs = new RemoteSession(HOST, USERNAME, PASSWORD, null, SU_USERNAME, SU_PASSWORD);
		try {
			String contents = rs.grepLast("LDAP","/var/log/nearlyallmessages");
			println("Contents: "+contents);
		} catch (RemoteUtilException e) {
			System.out.println("Error: "+e.getMessage());
		}
	}
	@Test
	public void listFilesTest() {
		RemoteSession rs = new RemoteSession(HOST, USERNAME, PASSWORD, null, SU_USERNAME, SU_PASSWORD);
		try {
			List<RemoteFile> files = rs.listFiles("/var/log");
			println(files.size()+" files found");
			for (RemoteFile file: files) {
				println(file.toString());
			}
		} catch (RemoteUtilException e) {
			System.out.println("Error: "+e.getMessage());
		}
	}
	
}
