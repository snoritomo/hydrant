package com.kikisoftware.hydrant.server;

public abstract class Request {
	boolean isSSL = false;
	public boolean isSSL() {
		return isSSL;
	}
	public void setSSL(boolean isSSL) {
		this.isSSL = isSSL;
	}
	abstract String getUserAgentHeader();
	abstract String getHeaderValue(String name);

	public static String normalizeHeaderName(String name) {
		if (name == null) return null;
		if (name.length()==0) return "";
		StringBuffer SB = new StringBuffer();
		char[] buffer = name.toLowerCase().toCharArray();
		SB.append(Character.toUpperCase(buffer[0]));
		for(int i=1 ; i<buffer.length ; i++) {
			SB.append(buffer[i]);
			if (buffer[i] == '-') {
				if ((i+1) < buffer.length) {
					SB.append(Character.toUpperCase(buffer[i+1]));
					i++;
				}
			}
		}
		return SB.toString();
	}
}
