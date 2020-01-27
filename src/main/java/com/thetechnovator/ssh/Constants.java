package com.thetechnovator.ssh;

public interface Constants {
	int SUCCESS=0;
	int FAILURE=1;
	int CR=13;
	int LF=10;
	int END_OF_TEXT_GROUP = 29;
	int END_OF_TEXT_RECORD = 30;
	int END_OF_TEXT_UNIT = 31;
	String CMD_SUFFIX = " ; printf '\\x1D\\x1E%d\\x1F' $?";
}
