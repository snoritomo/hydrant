package com.kikisoftware.hydrant.server;

import java.nio.channels.SocketChannel;
import java.util.ArrayList;

import com.kikisoftware.hydrant.log.Access;
import com.kikisoftware.hydrant.ua.UserAgent;


public class RequestResponsePair {
	private SocketChannel channel;
	private Request request;
	private ArrayList<Byte> response;
	private UserAgent useragent;
	private RequestProcessor reqp;
	/**
	 * RequestResponserで生成したRRPのうち何番目のRRPであるかを示す(1-origin)
	 */
	private int _order = -1;

	/**
	 * @return the logger
	 */
	public Access getLogger() {
		return logger;
	}

	private Access logger;

	private int responseLength;
	private String responseType;
	/**
	 * @return the useragent
	 */
	public UserAgent getUseragent() {
		return useragent;
	}
	/**
	 * @param useragent the useragent to set
	 */
	public void setUseragent(UserAgent useragent) {
		this.useragent = useragent;
	}
	public RequestResponsePair(SocketChannel ch, Request req, UserAgent ua, RequestProcessor reqproc){
		channel = ch;
		request = req;
		useragent = ua;
		reqp = reqproc;
		logger = new Access();
		logger.setRcvReqStartTime(System.currentTimeMillis());
	}
	public SocketChannel getChannel() {
		return channel;
	}
	public void setChannel(SocketChannel channel) {
		this.channel = channel;
	}
	public Request getRequest() {
		return request;
	}
	public void setRequest(Request request) {
		this.request = request;
	}
	public ArrayList<Byte> getResponse() {
		return response;
	}
	public void setResponse(ArrayList<Byte> response) {
		this.response = response;
	}
	public boolean getConnectionCloseFlag() {return reqp.getConnectionCloseFlag();}

	public int getOrder() {
		return _order;
	}
	public void setOrder(int order) {
		_order = order;
	}

	private String file;

	public String getFile() {
		return file;
	}
	public void setFile(String file) {
		this.file = file;
	}
	public int getResponseLength() {
		return responseLength;
	}
	public String getResponseType() {
		return responseType;
	}
	public void setResponseLength(int responseLength) {
		this.responseLength = responseLength;
	}
	public void setResponseType(String responseType) {
		this.responseType = responseType;
	}
}
