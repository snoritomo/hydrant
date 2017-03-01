package com.kikisoftware.hydrant;

import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.kikisoftware.hydrant.log.Stats;
import com.kikisoftware.hydrant.log.StatsLogger;
import com.kikisoftware.hydrant.server.TcpServer;

public class Main {

	private static final Logger log_ = Logger.getLogger(Main.class);
	public static void main(String[] args) {
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		URL url = loader.getResource("log4j.properties");
		PropertyConfigurator.configure(url);

		/* メッセージ表示 */
		log_.info("hydrant (C)2014 KikiSoftware All rights reserved.");
		log_.info("hydrant version 1.0.0");
		Stats.getInstance().setStartTime(System.currentTimeMillis());

//		System.setProperty("javax.net.debug", "all");

		/* サービスポート作成 */
		Map<String, Integer> hp = Utils.getAppHostPorts();
		Thread thread = null; // joinするためのThreadを一つ確保しておくために使用
		for(Entry<String, Integer> e : hp.entrySet()) {
			TcpServer entrance = new TcpServer(e.getKey().replaceFirst("[0-9]*=", ""), e.getValue(), false);
			thread = new Thread(entrance);
			thread.setPriority(Utils.getHttpThreadPriority());
			thread.start();
		}
		// サーバーソケット生成
		hp = Utils.getSslHostPorts();
		for(Entry<String, Integer> e : hp.entrySet()){
			TcpServer entrance = new TcpServer(e.getKey().replaceFirst("[0-9]*=", ""), e.getValue(), true);
			thread = new Thread(entrance);
			thread.setPriority(Utils.getHttpsThreadPriority());
			thread.start();
		}

		StatsLogger slog = new StatsLogger();
		
		Thread th = new Thread(slog);
		th.setPriority(Utils.getStatsLogThreadPriority());
		th.start();
		
		log_.info("Server start.");

		if(thread!=null) {
			try {
				thread.join();
			} catch (InterruptedException e) {
				log_.error("Main:Exception occur.\n" + e.getStackTrace());
			}
		} else {
			log_.error("Main:server threads not exist.\n");
		}
		log_.error("Thread stop");
	}

}
