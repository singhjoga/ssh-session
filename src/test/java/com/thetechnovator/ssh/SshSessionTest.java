package com.thetechnovator.ssh;

import org.junit.Ignore;
import org.junit.Test;

import com.thetechnovator.ssh.SshCommand.Builder;

import junit.framework.Assert;

public class SshSessionTest extends TestBase {
	static {
		loadProperties();
	}

	@Test
///	@Ignore
	public void basicTest() throws SshSessionlException {
		SshSession ses = new SshSession.Builder().host(HOST).username(USERNAME).password(PASSWORD).switchUserPasswordProvider(new PasswordProvider() {
			@Override
			public String getPassword() {
				return PASSWORD;
			}
		}).build();
		ses.connect();
		Assert.assertEquals(0, ses.exec("ls -l /tmp"));
		Assert.assertEquals(0, ses.exec("rm -rf /tmp/ndm-config.ftl"));
		Assert.assertEquals(1, ses.exec("rm  /tmp/ndm-config.ftl123"));
		ses.close();
	}
	@Test
	//@Ignore
	public void sudoTest() throws SshSessionlException {
		SshSession.Builder builder = new SshSession.Builder();
		SshSession ses =builder.host(HOST).username(USERNAME).password(PASSWORD).build();
		ses.connect();
		// this will prompt for sudo password. Since the sudo password is the same as normal password, it would succeed
		Assert.assertEquals(0, ses.exec("sudo ls /var"));
		
		//sudo is verified once per session, now if we change the password provider, it will have no efect
		ses = builder.sudoPasswordProvider(new PasswordProvider() {
			@Override
			public String getPassword() {
				return "ssssss";
			}
		}).build();
		Assert.assertEquals(0, ses.exec("sudo ls /var"));
		
		//now close session and reconnect
		ses.close();
		ses.connect();
		//since now the sudo password provider returns wrong pass, it will throw an exception
		try {
			ses.exec("sudo ls /var");
			Assert.fail("It should throw an exception");
		} catch (SshSessionlException e) {
			//all is well.
		}
		//once a sudo fails, there could be many retries, to avoid that the session is closed, it must be reconnected
		
		//reset the password provider or make it return correct password
		ses = builder.sudoPasswordProvider(null).build();
		ses.connect();
		Assert.assertEquals(0, ses.exec("sudo ls /var"));
		ses.close();
	}
	
	@Test
	//@Ignore
	public void switchUserTest() throws SshSessionlException {
		SshSession.Builder builder = new SshSession.Builder();
		SshSession ses =builder.host(HOST).username(USERNAME).password(PASSWORD).build();
		ses.connect();
		// this will not prompt for su password, since it is with sudo it will prompt for sudo password
		Assert.assertEquals(0, ses.exec("sudo su - root"));
		Assert.assertEquals(0, ses.exec("echo ~"));
		//exit su
		Assert.assertEquals(0, ses.exec("exit"));
		Assert.assertEquals(0, ses.exec("echo ~"));
		//this will throw exception because no password provider is set for su. Also close the session
		try {
			Assert.assertEquals(0, ses.exec("su - root"));
			Assert.fail("It should throw an exception");
		} catch (SshSessionlException e) {
			println("Received exception: "+e.getMessage());
		}
		//check that session is closed
		Assert.assertEquals(false, ses.isOpen());
		
		//try with wrong root password
		ses = builder.switchUserPasswordProvider(new PasswordProvider() {
			@Override
			public String getPassword() {
				return "ssssss";
			}
		}).build();
		ses.connect();
		//since it is a wrong su password, it will throw an exception again
		try {
			Assert.assertEquals(0, ses.exec("su - root"));
			Assert.fail("It should throw an exception on wrong su password");
		} catch (SshSessionlException e) {
			println("Received exception: "+e.getMessage());
		}
		//set the correct password
		ses = builder.switchUserPasswordProvider(new PasswordProvider() {
			@Override
			public String getPassword() {
				return PASSWORD;
			}
		}).build();
		//it it should succeed
		Assert.assertEquals(0, ses.exec("su - root"));
		
		ses.close();
	}
	@Test
	//@Ignore
	public void timeoutTest() throws SshSessionlException {
		SshSession ses = new SshSession.Builder().host(HOST).username(USERNAME).password(PASSWORD).build();
		ses.connect();

		// Timeout tests
		try {
			SshCommand cmd = Builder.getInstance("read ").maxExecutionTimeMs(5000).build();
			ses.exec(cmd);
			Assert.fail("It should fail with MaxExectionTime elapsed");
		} catch (SshSessionlException e) {
			println(e.getMessage());
		}
		try {
			SshCommand cmd = Builder.getInstance("read ").maxInactivityTimeMs(5000).build();
			ses.exec(cmd);
			Assert.fail("It should fail with MaxInactivityTime elapsed");
		} catch (SshSessionlException e) {
			println(e.getMessage());
		}
		ses.close();
	}
}