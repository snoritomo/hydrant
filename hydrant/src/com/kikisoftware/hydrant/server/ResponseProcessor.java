package com.kikisoftware.hydrant.server;

import java.nio.channels.SocketChannel;
import java.util.ArrayList;

public class ResponseProcessor implements Runnable{
	private RequestProcessor _RP;
	private RequestResponsePair _RRP;
	private boolean sendRequest;
	private ResponseBuilder _getter;
	public ResponseProcessor(RequestProcessor starter, RequestResponsePair data, ResponseBuilder getter){
		_RP = starter;
		_RRP = data;
		_getter = getter;
		sendRequest = getter==null;
	}
	@Override
	public void run() {
		RawRequest raw = (RawRequest)_RRP.getRequest();
		SocketChannel web = null;
		if(sendRequest){
			ResponseBuilder req = ResponseBuilderFactory.getResponseBuilder(_RP, raw, _RRP.getUseragent(), _RRP.getLogger());
			_RRP.setFile(new String(req.getArrangedRequest().getRequestURI().getFile()));
			ArrayList<Byte> response = req.getContents(_RRP.getConnectionCloseFlag());
			_RRP.setResponse(response);
			_getter = req;
		}
		else{
			SocketChannel gc = _getter.getWebSocketChannel();
			if(gc==null)return;
			ArrayList<Byte> response = _getter.getContents(_RRP.getConnectionCloseFlag(), gc);
			_RRP.setResponse(response);
		}
		web = _getter.getBodyChannel();
		_RRP.setResponseLength(_getter.getResponseLength());
		_RRP.setResponseType(_getter.getResponseContentType());
		_RP.processResponse(_RRP, web, _getter);
	}
	public void forceResponse(){
		_RP.processResponse(_RRP, null, null);
	}
}
