package com.kikisoftware.hydrant;

import java.util.LinkedList;

import org.apache.log4j.Logger;

import com.kikisoftware.hydrant.server.ResponseProcessor;

public class ThreadExecuter {
	protected static final Logger log_ = Logger.getLogger(ThreadExecuter.class);

	private final int _threadCount;
	protected final LinkedList<Runnable> _queue;
	public LinkedList<Runnable> getQueue() {
		return _queue;
	}

	private final PoolWorker[] _threads;

	public ThreadExecuter(int threadCount, int priority) {
		this._threadCount = threadCount;
		this._queue = new LinkedList<Runnable>();
		this._threads = new PoolWorker[this._threadCount];

		for (int i=0 ; i<this._threadCount ; i++) {
			this._threads[i] = new PoolWorker();
			this._threads[i].setPriority(priority);
			this._threads[i].start();
		}
	}

	public void execute(Runnable runnable) {
		synchronized(this._queue) {
			this._queue.addLast(runnable);
			this._queue.notify();
		}
	}

	private class PoolWorker extends Thread {
		protected final Logger mylog_ = Logger.getLogger(PoolWorker.class);
		@Override
		public void run() {
			Runnable runnable;

			while (true) {
				synchronized(ThreadExecuter.this._queue) {
					while (ThreadExecuter.this._queue.isEmpty()) {
						try {
							ThreadExecuter.this._queue.wait();
						} catch (InterruptedException IE) {
							// 無視する
						}
					}

					runnable = ThreadExecuter.this._queue.removeFirst();
				}

				try {
					runnable.run();
				} catch (Throwable RE) {
					mylog_.warn("PoolWorker:" +
							runnable.getClass().getName() + ":" +
							Utils.getStackTrace(RE));
					if(runnable instanceof ResponseProcessor){//リクエスト・レスポンス処理なら強制的にレスポンスを返す
						ResponseProcessor rp = (ResponseProcessor)runnable;
						rp.forceResponse();
					}
				}
			}
		}
	}
}
