package com.dbsystel.devops.remoteutils;

import java.util.Date;

public class RemoteFile {
	private String absolutePath;
	private long size;
	private Date modifiedTime;
	private boolean isDirectory;
	public RemoteFile(String absolutePath, long size, Date modifiedTime,boolean isDirector) {
		super();
		this.absolutePath = absolutePath;
		this.size = size;
		this.modifiedTime = modifiedTime;
		this.isDirectory=isDirector;
	}
	public String getAbsolutePath() {
		return absolutePath;
	}
	public void setAbsolutePath(String absolutePath) {
		this.absolutePath = absolutePath;
	}
	public long getSize() {
		return size;
	}
	public void setSize(long size) {
		this.size = size;
	}
	public Date getModifiedTime() {
		return modifiedTime;
	}
	public void setModifiedTime(Date modifiedTime) {
		this.modifiedTime = modifiedTime;
	}
	public boolean isDirectory() {
		return isDirectory;
	}
	public void setDirectory(boolean isDirectory) {
		this.isDirectory = isDirectory;
	}
	@Override
	public String toString() {
		return "RemoteFile [absolutePath=" + absolutePath + ", size=" + size + ", modifiedTime=" + modifiedTime
				+ ", isDirectory=" + isDirectory + "]";
	}
	
}
