package com.kikisoftware.hydrant.server;

import java.io.IOException;
import java.net.SocketException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;

import org.apache.log4j.Logger;

import com.kikisoftware.hydrant.ThreadExecuter;
import com.kikisoftware.hydrant.Utils;

public class SSLEngineManager {
	/**
	 * 
	 */
	public static final Logger log_ = Logger.getLogger(SSLEngineManager.class);
	/**
	 * 
	 */
	private static int getSSLThreadPoolSize(){return Utils.getSslThreadCount();}
	
	public boolean usable(){return (_taskrunner.getQueue().size()==0);}
	private String id;
	private SocketChannel channel;
	private SSLEngine engine;
	private ByteBuffer netSendBuffer;
	private ByteBuffer appSendBuffer;
	private ByteBuffer appRecvBuffer;
	private ByteBuffer netRecvBuffer;
	private SSLEngineResult engineResult = null;
	private boolean started = false;
	public boolean needHandshake(){return !started;}
	private static ThreadExecuter _taskrunner = new ThreadExecuter(getSSLThreadPoolSize(), Thread.NORM_PRIORITY-1);
	private boolean processHandshake(boolean returnOnNeedTask) throws IOException{
		log_.info(channel.hashCode()+":"+id+" ses:"+new String(engine.getSession().getId())+"["+engine.getSession().getCreationTime()+"]"+" processHandshake hand:"+engineResult.getHandshakeStatus().toString() + " stat:" + engineResult.getStatus());
		switch(engineResult.getHandshakeStatus()){
		case NEED_TASK:
			runDelegatedTasks(returnOnNeedTask);
			return returnOnNeedTask;
		case NEED_UNWRAP:
			if(!engine.isInboundDone())channel.read(netRecvBuffer);
			netRecvBuffer.flip();
			try {
				engineResult = engine.unwrap(netRecvBuffer, appRecvBuffer);
			} catch (SSLHandshakeException e) {
				log_.info(channel.hashCode()+":"+id+" ses:"+new String(engine.getSession().getId())+"["+engine.getSession().getCreationTime()+"]"+" unwrap bad hand:"+engineResult.getHandshakeStatus().toString() + " stat:" + engineResult.getStatus() + "\n" + Utils.getStackTrace(e));
				if(returnOnNeedTask)
					return true;
				else
					throw e;
			}
			log_.info(channel.hashCode()+":"+id+" ses:"+new String(engine.getSession().getId())+"["+engine.getSession().getCreationTime()+"]"+" unwrap ok hand:"+engineResult.getHandshakeStatus().toString() + " stat:" + engineResult.getStatus());
			netRecvBuffer.compact();
			break;
		case NEED_WRAP:
			appSendBuffer.flip();
			try {
				engineResult = engine.wrap(appSendBuffer, netSendBuffer);
			} catch (SSLHandshakeException e) {
				log_.info(channel.hashCode()+":"+id+" ses:"+new String(engine.getSession().getId())+"["+engine.getSession().getCreationTime()+"]"+" wrap bad hand:"+engineResult.getHandshakeStatus().toString() + " stat:" + engineResult.getStatus() + "\n" + Utils.getStackTrace(e));
				if(returnOnNeedTask)
					return true;
				else
					throw e;
			}
			log_.info(channel.hashCode()+":"+id+" ses:"+new String(engine.getSession().getId())+"["+engine.getSession().getCreationTime()+"]"+" wrap ok hand:"+engineResult.getHandshakeStatus().toString() + " stat:" + engineResult.getStatus());
			appSendBuffer.compact();
			if(engineResult.getStatus() == SSLEngineResult.Status.CLOSED){
				try{
					flush();
				}
				catch(SocketException exc){
					exc.printStackTrace();
				}
			}
			else{
				flush();
			}
			break;
		case FINISHED:
		case NOT_HANDSHAKING:
			started = true;
			return false;
		}
		switch(engineResult.getStatus()){
		case BUFFER_UNDERFLOW:
//			if(!started)return true;
		case BUFFER_OVERFLOW:
			return false;
		case CLOSED:
			if(engine.isOutboundDone()){
				channel.socket().shutdownOutput();
			}
			return false;
		case OK:
			break;
		}
		return true;
	}
	protected void runDelegatedTasks(boolean singleThread){
		Runnable task;
		while((task = engine.getDelegatedTask()) !=null){
			if(singleThread)
				task.run();
			else
				_taskrunner.execute(task);
		}
		engineResult = new SSLEngineResult(engineResult.getStatus(), engine.getHandshakeStatus(), engineResult.bytesProduced(), engineResult.bytesConsumed());
	}
	public static SSLEngineManager getInstance(SSLContext sslCtx, SocketChannel channel){
		SSLEngine ex = sslCtx.createSSLEngine(channel.socket().getInetAddress().getHostName(), channel.socket().getPort());
		ex.setUseClientMode(false);
		SSLEngineManager re = new SSLEngineManager(channel, ex);
		return re;
	}
	private SSLEngineManager(SocketChannel channel, SSLEngine sslengine){
		this.id = channel.socket().getInetAddress().getHostName() + ":" + channel.socket().getPort();
		this.channel = channel;
		engine = sslengine;
		SSLSession session = engine.getSession();
		int netBufferSize = session.getPacketBufferSize();
		int appBufferSize = session.getApplicationBufferSize();
		this.appSendBuffer = ByteBuffer.allocate(appBufferSize);
		this.netSendBuffer = ByteBuffer.allocate(netBufferSize);
		this.appRecvBuffer = ByteBuffer.allocate(appBufferSize);
		this.netRecvBuffer = ByteBuffer.allocate(netBufferSize);
		log_.info("making sslMan."+id);
	}
	public int read() throws IOException, SSLException{
		if(engine.isInboundDone()){
			return -1;
		}
		int pos = appRecvBuffer.position();
		int count = channel.read(netRecvBuffer);
		netRecvBuffer.flip();
		engineResult = engine.unwrap(netRecvBuffer, appRecvBuffer);
		netRecvBuffer.compact();
		log_.info(channel.hashCode()+":"+id+" ses:"+new String(engine.getSession().getId())+"["+engine.getSession().getCreationTime()+"]"+" read:"+count+" dec:"+(appRecvBuffer.position()-pos)+" restat:"+engineResult.getStatus()+" handshake:" + started + " stat:"+engineResult.getHandshakeStatus().toString());
		switch(engineResult.getStatus()){
		case BUFFER_UNDERFLOW:
			return 0;
		case BUFFER_OVERFLOW:
			throw new BufferOverflowException();
		case CLOSED:
			channel.socket().shutdownInput();
//			return -1;
			break;
		case OK:
			break;
		}
		while(processHandshake(!started));
		if(count == -1){
			engine.closeInbound();
		}
		if(engine.isInboundDone()){
			return -1;
		}
		count = appRecvBuffer.position()-pos;
		return count;
	}
	public int write() throws IOException, SSLException{
		int pos = appSendBuffer.position();
		netSendBuffer.clear();
		appSendBuffer.flip();
		engineResult = engine.wrap(appSendBuffer, netSendBuffer);
		appSendBuffer.compact();
		switch(engineResult.getStatus()){
		case BUFFER_UNDERFLOW:
			throw new BufferUnderflowException();
		case BUFFER_OVERFLOW:
			throw new BufferOverflowException();
		case CLOSED:
			throw new SSLException("SSLEngine is CLOSED");
		case OK:
			break;
		}
		while(processHandshake(!started));
		flush();
		return pos-appSendBuffer.position();
	}
	public int write(byte[] raw) throws IOException, SSLException{
		int re = 0;
		int f = appSendBuffer.position();
		int c = raw.length + f;
		while(true){
			int blim = appSendBuffer.limit()-appSendBuffer.position();
			int rrel = raw.length-re;
			appSendBuffer.put(raw, re, blim>rrel?rrel:blim);
			re += write();
			if(c<=re)break;
		}
		return re;
	}
	public int flush() throws IOException{
		netSendBuffer.flip();
		int count = channel.write(netSendBuffer);
		netSendBuffer.compact();
		return count;
	}
	public void close() throws IOException, SSLException{
		try{
			flush();
			if(!engine.isOutboundDone()){
				engine.closeOutbound();
				while(processHandshake(false));
			}
			else if(!engine.isInboundDone()){
				engine.closeInbound();
				processHandshake(false);
			}
		}
		finally{
			channel.close();
		}
	}
	public void closeOutbound() throws IOException, SSLException{
		flush();
		if(!engine.isOutboundDone()){
			engine.closeOutbound();
			while(processHandshake(false));
		}
	}
	public boolean isClosed(){
		return engine.isOutboundDone();
	}
	public ByteBuffer getAppRecvBuffer(){
		return appRecvBuffer;
	}
	public ByteBuffer getAppSendBuffer(){
		return appSendBuffer;
	}
	public SSLEngine getEngine(){
		return engine;
	}
}
