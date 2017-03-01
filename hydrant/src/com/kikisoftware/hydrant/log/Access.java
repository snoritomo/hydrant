package com.kikisoftware.hydrant.log;

import java.text.DateFormat;
import java.util.Date;


import org.apache.log4j.Logger;

import com.kikisoftware.hydrant.Consts;
import com.kikisoftware.hydrant.ThreadExecuter;
import com.kikisoftware.hydrant.Utils;

public class Access implements Runnable {
	/**
	 * 
	 */
	private static final Logger log_ = Logger.getLogger(Access.class);
	
	/**
	 * 
	 */
	static {
		/* メッセージ表示 */
		log_.info("access start.");
		exe = new ThreadExecuter(1, Utils.getAccessLogThreadPriority());
	}

	private static ThreadExecuter exe;
	
	public static void doTask(Access task){
		exe.execute(task);
	}
	private long rcvReqStartTime = 0;//{0}
	private String devName = "";//{1}
	private String url = "";//{2}
//	private int rcvRequestLength = 0;//{3}
	private int sndReqRetry = 0;//{4}
	private long sndReqEndTime = 0;//{5}
	private long rcvResHeadTime = 0;//{6}
	private long sndResEndTime = 0;//{7}{11}
	private String mimeType = "";//{8}
//	private int sndResponseLength = 0;//{9}
	private String errorName = "";//{10}
	private String toUrl = "";//{12}
	private String userAgent = "";//{13}
	private int waiting = 0;//{14}
	private int rcvReqHeaderLength = 0;//{15}
	private int rcvReqContentLength = 0;//{16}
	private int sndResHeaderLength = 0;//{17}
	private int sndResContentLength = 0;//{18}
	
	public String getString(String format){
		String wk = format.replace("{0}", DateFormat.getDateTimeInstance().format(new Date(rcvReqStartTime)));
		wk = wk.replace("{1}", devName==null?"":devName);
		wk = wk.replace("{2}", url==null?"":url);
		wk = wk.replace("{3}", Integer.toString(rcvReqHeaderLength + rcvReqContentLength));
		wk = wk.replace("{4}", Integer.toString(sndReqRetry));
		wk = wk.replace("{5}", sndReqEndTime>0?Long.toString(sndReqEndTime-rcvReqStartTime):"-");
		wk = wk.replace("{6}", rcvResHeadTime>0?Long.toString(rcvResHeadTime-sndReqEndTime):"-");
		wk = wk.replace("{7}", sndResEndTime>0?Long.toString(sndResEndTime-rcvResHeadTime):"-");
		wk = wk.replace("{8}", mimeType==null?"":(mimeType.indexOf(Consts.CONTENT_TYPE_CHARSET_SEPARATOR)<0?mimeType:mimeType.substring(0, mimeType.indexOf(Consts.CONTENT_TYPE_CHARSET_SEPARATOR))));
		wk = wk.replace("{9}", Integer.toString(sndResHeaderLength + sndResContentLength));
		wk = wk.replace("{10}", errorName==null?"":errorName);
		wk = wk.replace("{11}", sndResEndTime>0?DateFormat.getTimeInstance().format(new Date(sndResEndTime)):"-");
		wk = wk.replace("{12}", toUrl==null?"":toUrl);
		wk = wk.replace("{13}", userAgent==null?"":userAgent);
		wk = wk.replace("{14}", Integer.toString(waiting));
		wk = wk.replace("{15}", Integer.toString(rcvReqHeaderLength));
		wk = wk.replace("{16}", Integer.toString(rcvReqContentLength));
		wk = wk.replace("{17}", Integer.toString(sndResHeaderLength));
		wk = wk.replace("{18}", Integer.toString(sndResContentLength));
		return wk;
	}

	@Override
	public void run() {
		String wk = getString(Utils.getAccessLogFormat());
		if(wk.equals(""))return;
		log_.info(wk);
	}

	public void setRcvReqStartTime(long rcvReqStartTime) {
		this.rcvReqStartTime = rcvReqStartTime;
	}
	public void setDevName(String devName) {
		this.devName = devName;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public void setRcvReqContentLength(int rcvReqContentLength) {
		this.rcvReqContentLength = rcvReqContentLength;
	}
	public void setSndReqRetry(int sndReqRetry) {
		this.sndReqRetry = sndReqRetry;
	}
	public void setSndReqEndTime(long sndReqEndTime) {
		this.sndReqEndTime = sndReqEndTime;
	}
	public void setRcvResHeadTime(long rcvResAndTransEndTime) {
		this.rcvResHeadTime = rcvResAndTransEndTime;
	}
	public void setSndResEndTime(long sndResEndTime) {
		this.sndResEndTime = sndResEndTime;
	}
	public void setSndContentType(String mimeType) {
		this.mimeType = mimeType;
	}
	public void setSndResContentLength(int sndResContentLength) {
		this.sndResContentLength = sndResContentLength;
	}
	public void setErrorName(String errorName) {
		if(!this.errorName.equals(""))return;
		this.errorName = errorName;
	}
	public void setToUrl(String toUrl) {
		this.toUrl = toUrl;
	}
	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}
	public void setWaiting(int waiting) {
		this.waiting = waiting;
	}
	public void setRcvReqHeaderLength(int rcvReqHeaderLength) {
		this.rcvReqHeaderLength = rcvReqHeaderLength;
	}
	public void setSndResHeaderLength(int sndResHeaderLength) {
		this.sndResHeaderLength = sndResHeaderLength;
	}
}
