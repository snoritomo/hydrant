package com.kikisoftware.hydrant.server;

import java.nio.channels.SocketChannel;
import java.util.ArrayList;

import com.kikisoftware.hydrant.rewrite.Rewrite;

public interface ResponseBuilder {
	SocketChannel getBodyChannel();
	SocketChannel getWebSocketChannel();
	RewroteRequest getArrangedRequest();
	void setArrangedRequest(RewroteRequest request);
	RawRequest getOriginalRequest();
	void setRule(Rewrite rule);
	ArrayList<Byte> getContents(boolean closeConnection);
	ArrayList<Byte> getContents(boolean closeConnection, SocketChannel channel);
	int getResponseLength();
	String getResponseContentType();
	int buffedBodyCount();
	int headerSize();
}
