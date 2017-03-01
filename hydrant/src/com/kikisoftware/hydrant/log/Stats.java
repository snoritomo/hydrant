package com.kikisoftware.hydrant.log;


import org.apache.log4j.Logger;

import com.kikisoftware.hydrant.Utils;

public class Stats {
	private static final Logger log_ = Logger.getLogger(Stats.class);
	static {
		log_.info("stats start.");
	}

	protected Stats(){
		
	}
	private static Stats instance = null;
	public synchronized static Stats getInstance(){
		if(instance == null){
			instance = new Stats();
		}
		return instance;
	}
	
	public static void doTask(Stats task){
		String wk = task.getString(Utils.getStatsLogFormat());
		task.resetStats();
		if(wk.equals(""))return;
		log_.info(wk);
	}

	private long startTime = 0;//{0}
	private long accept = 0;//{1}
	private long request = 0;//{2}
	private int rejectRequest = 0;//{3}
	private long fineResponse = 0;//{4}
	private int makeErrorResponseByInner = 0;//{5}
	private int readSocketError = 0;//{6}
//	private int maxKeepAliveOver = 0;//{7}
	private int connectWebServerFailure = 0;//{7}
	private int requestWriteError = 0;//{8}
	private int responseReadError = 0;//{9}
	private int unKnownErrorWithWeb = 0;//{10}
	private int errorResponseWriteError = 0;//{11}
	private int responseWriteError = 0;//{12}
	private int unKnownError = 0;//{13}
	private MegaDataCounter clientUp = new MegaDataCounter();//{14}
	private MegaDataCounter webUp = new MegaDataCounter();//{15}
	private MegaDataCounter webDown = new MegaDataCounter();//{16}
	private MegaDataCounter clientDown = new MegaDataCounter();//{17}
	private void resetStats(){
//		startTime = 0;//{0}稼働時間はクリアしない
		accept = 0;//{1}
		request = 0;//{2}
		rejectRequest = 0;//{3}
		fineResponse = 0;//{4}
		makeErrorResponseByInner = 0;//{5}
		readSocketError = 0;//{6}
//		maxKeepAliveOver = 0;//{7}
		connectWebServerFailure = 0;//{7}
		requestWriteError = 0;//{8}
		responseReadError = 0;//{9}
		unKnownErrorWithWeb = 0;//{10}
		errorResponseWriteError = 0;//{11}
		responseWriteError = 0;//{12}
		unKnownError = 0;//{13}
		clientUp.init();//{14}
		webUp.init();//{15}
		webDown.init();//{16}
		clientDown.init();//{17}
	}
	public String getString(String format){
		String wk = format;
		wk = wk.replace("{0}", startTime>0?Long.toString(System.currentTimeMillis()-startTime):"-");
		wk = wk.replace("{1}", accept>0?Long.toString(accept):"-");
		wk = wk.replace("{2}", request>0?Long.toString(request):"-");
		wk = wk.replace("{3}", rejectRequest>0?Integer.toString(rejectRequest):"-");
		wk = wk.replace("{4}", fineResponse>0?Long.toString(fineResponse):"-");
		wk = wk.replace("{5}", makeErrorResponseByInner>0?Integer.toString(makeErrorResponseByInner):"-");
		wk = wk.replace("{6}", readSocketError>0?Integer.toString(readSocketError):"-");
//		wk = wk.replace("{7}", maxKeepAliveOver>0?Integer.toString(maxKeepAliveOver):"-");
		wk = wk.replace("{7}", connectWebServerFailure>0?Integer.toString(connectWebServerFailure):"-");
		wk = wk.replace("{8}", requestWriteError>0?Integer.toString(requestWriteError):"-");
		wk = wk.replace("{9}", responseReadError>0?Integer.toString(responseReadError):"-");
		wk = wk.replace("{10}", unKnownErrorWithWeb>0?Integer.toString(unKnownErrorWithWeb):"-");
		wk = wk.replace("{11}", errorResponseWriteError>0?Integer.toString(errorResponseWriteError):"-");
		wk = wk.replace("{12}", responseWriteError>0?Integer.toString(responseWriteError):"-");
		wk = wk.replace("{13}", unKnownError>0?Integer.toString(unKnownError):"-");
		wk = wk.replace("{14}", clientUp.hasData()?Long.toString(clientUp.get()):"-");
		wk = wk.replace("{14M}", clientUp.hasData()?Long.toString(clientUp.getMega()):"-");
		wk = wk.replace("{15}", webUp.hasData()?Long.toString(webUp.get()):"-");
		wk = wk.replace("{15M}", webUp.hasData()?Long.toString(webUp.getMega()):"-");
		wk = wk.replace("{16}", webDown.hasData()?Long.toString(webDown.get()):"-");
		wk = wk.replace("{16M}", webDown.hasData()?Long.toString(webDown.getMega()):"-");
		wk = wk.replace("{17}", clientDown.hasData()?Long.toString(clientDown.get()):"-");
		wk = wk.replace("{17M}", clientDown.hasData()?Long.toString(clientDown.getMega()):"-");
		return wk;
	}
	
	private class MegaDataCounter {
		private static final int dig = 1000000;
		private long mega;
		private int cnt;
		private MegaDataCounter(){
			init();
		}
		private void init(){
			synchronized (this) {
				mega = 0;
				cnt = 0;
			}
		}
		private boolean hasData(){
			return mega>0||cnt>0;
		}
		private void plus(int data){
			synchronized (this) {
				cnt += data;
				if (cnt >= dig) {
					String cns = Integer.toString(cnt);
					int dis = Integer.parseInt(cns.substring(cns.length() - 6));
					mega += (cnt - dis) / dig;
					cnt = dis;
				}
			}
		}
		private long getMega(){
			return mega;
		}
		private long get(){
			return (mega*dig)+cnt;
		}
	}
	
	/**
	 * @param startTime the startTime to set
	 */
	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}
	public synchronized void addClientUp(int byt){
		clientUp.plus(byt);
	}
	public synchronized void addClientDown(int byt){
		clientDown.plus(byt);
	}
	public synchronized void addWebUp(int byt){
		webUp.plus(byt);
	}
	public synchronized void addWebDown(int byt){
		webDown.plus(byt);
	}
	public synchronized void countUpUnKnownError(){
		unKnownError++;
	}
	public synchronized void countUpAccept(){
		accept++;
	}
	public synchronized void countUpReadSocketError(){
		readSocketError++;
	}
	public synchronized void countUpRequest(){
		request++;
	}
	public synchronized void countUpRejectRequest(){
		rejectRequest++;
	}
//	public synchronized void countUpMaxKeepAliveOver(){
//		maxKeepAliveOver++;
//	}
	public synchronized void countUpConnectWebServerFailure(){
		connectWebServerFailure++;
	}
	public synchronized void countUpRequestWriteError(){
		requestWriteError++;
	}
	public synchronized void countUpResopnseReadError(){
		responseReadError++;
	}
	public synchronized void countUpMakeErrorResponseByInner(){
		makeErrorResponseByInner++;
	}
	public synchronized void countUpErrorResponseWriteError(){
		errorResponseWriteError++;
	}
	public synchronized void countUpResponseWriteError(){
		responseWriteError++;
	}
	public synchronized void countUpFineResponse(){
		fineResponse++;
	}
	public synchronized void countUpUnKnownErrorWithWeb(){
		unKnownErrorWithWeb++;
	}
}
