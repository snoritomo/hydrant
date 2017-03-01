package com.kikisoftware.hydrant.log;

import com.kikisoftware.hydrant.Utils;

public class StatsLogger implements Runnable {

	@Override
	public void run() {
		Stats lg = Stats.getInstance();
		long start = System.currentTimeMillis();
		long count = 1;
		long interval = Utils.getStatsInterval();//reloadが効かない仕様にした。それより時間間隔がずれない方が大事でしょ？
		long to = start + (count++ * interval);
		long next = 0;
		while(true){
			try {
				next = to;
				to = start + (count++ * interval);
				Thread.sleep(next - System.currentTimeMillis());//次の更新時刻まで待機
			} catch (InterruptedException e) {
			}
			//stats.log 出力＆値クリア
			Stats.doTask(lg);
		}
	}

}
