package com.kikisoftware.hydrant.server;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;

import javax.net.ssl.SSLException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.kikisoftware.hydrant.Consts;
import com.kikisoftware.hydrant.ThreadExecuter;
import com.kikisoftware.hydrant.Utils;
import com.kikisoftware.hydrant.log.Access;
import com.kikisoftware.hydrant.log.Stats;
import com.kikisoftware.hydrant.ua.UserAgent;

public class RequestProcessor {
	private static final Logger log_ = Logger.getLogger(RequestProcessor.class);
	private static final boolean isDebug = Level.DEBUG.isGreaterOrEqual(Utils.getLoggerLevel(log_));

	private int _id;
	public int getId() {
		return _id;
	}
	private static Object nextRPIdLockObject_ = new Object();
	private static int nextRPId_ = 0;
	public static RequestProcessor getInstance(TcpServer acceptor,
			SelectionKey selectionKey, SSLEngineManager sslman) {
		if (acceptor==null || selectionKey == null){
			throw new IllegalArgumentException();
		}

		int id;
		synchronized(nextRPIdLockObject_) {
			id = nextRPId_++;
		}

		RequestProcessor RP = new RequestProcessor(id, acceptor, selectionKey, sslman);

		return RP;
	}
	@Override
	public boolean equals(Object object) {
		return (object instanceof RequestProcessor &&
				((RequestProcessor)object)._id == this._id);
	}
	public static ThreadExecuter threadExecuter_ =
		new ThreadExecuter(Utils.getThreadPoolSize(), Utils.getRequestThreadPriority());
	private SSLEngineManager secureMan;
	public SSLEngineManager getSSLManager(){return secureMan;}
	private SocketChannel _channel;
	private ArrayList<Integer> responseQueue = new ArrayList<Integer>();
	private Integer _queuedCount = 0;
	private Integer processing = 0;
	public Integer getProcessing() {
		return processing;
	}
	private Integer _processed = 0;
	private boolean _rejectNewRequestFlag = false;
	public boolean getConnectionCloseFlag() {return _rejectNewRequestFlag;}
	private TcpServer _acceptor;
	private SelectionKey _selectionKey;
	private long _lastAccessTime;
	public long getLastAccessTime() {
		return this._lastAccessTime;
	}
	private RawRequestFactory _requestFactory = new RawRequestFactory();
	public synchronized void update() {
//		long current = System.currentTimeMillis();
//		if (this._lastAccessTime < current){
			this._lastAccessTime = System.currentTimeMillis();
//		}
		if(!processShouldBeOver())this._rejectNewRequestFlag = false;
	}
	public boolean processShouldBeOver(){
		return (_processed >= Utils.getKeepAliveMaxCount() || System.currentTimeMillis() >= (_lastAccessTime + Utils.getKeepAliveTimeout())) && processing<=0 && !this._requestFactory.requesting();
	}
	public void rejectNewRequest() {
		if(!processShouldBeOver())return;
		this._rejectNewRequestFlag = true;
		if (processing == 0){
			connectionClose();
		}
	}
	private ByteBuffer _byteBuffer = ByteBuffer.allocate(Utils.getThroughIOBufferSize());
	public ByteBuffer getByteBuffer() {
		return this._byteBuffer;
	}

	private RequestProcessor(int id,
			TcpServer acceptor,
			SelectionKey selectionKey,
			SSLEngineManager sslman) {
		this._id = id;
		update();
		this._acceptor = acceptor;
		this._selectionKey = selectionKey;
		this._channel = (SocketChannel)selectionKey.channel();
		this._requestFactory.setIPAddress(_channel.socket().getInetAddress());
		secureMan = sslman;
	}

	public synchronized void connectionClose() {
		if(secureMan!=null){
			try {
				if(!secureMan.isClosed())secureMan.close();
			} catch (SSLException e) {
				log_.warn(RequestProcessor.class.getSimpleName() + ":" + Utils.getStackTrace(e));
			} catch (IOException e) {
				log_.warn(RequestProcessor.class.getSimpleName() + ":" + Utils.getStackTrace(e));
			}
		}
		if (this._acceptor != null){
			this._acceptor.processCloseChannel(this._selectionKey);
			this._selectionKey.cancel();
		}
		this._rejectNewRequestFlag = true;
		this._acceptor = null; // _selectionKeyをnullにしてからnullにする
	}
	public synchronized void processRequestBytes(byte[] buffer, int length) {
		update();

		int start = 0;
		int uselen = length;
		if(inBody){
			start = bodylen;
			inBody = sendContentBody(buffer, length);
			if(inBody)return;
			start = bodylen - start;
			uselen = length - start;
			//リクエスト完了時刻
			forPost.getLogger().setSndReqEndTime(System.currentTimeMillis());
			sendRequest((RawRequest)forPost.getRequest(), forPost.getUseragent(), postWeb);
		}
		Stats lg = Stats.getInstance();
		lg.addClientUp(length);

		RawRequest[] requests = this._requestFactory.process(buffer, start, uselen);
		if (requests != null) {
			for(int i=0 ; i<requests.length ; i++) {
				lg.countUpRequest();

				if(Utils.isCommandableIP(_channel.socket().getInetAddress())){
					if(requests[i].getRequestURLString().equals(Utils.getStatsCommand())){
						String wk = Stats.getInstance().getString(Utils.getStatsLogFormat());
						String res = "HTTP/1.1 200 OK\r\nContent-Length:"+wk.length()+"\r\n\r\n"+wk;
						try {
							_channel.write(ByteBuffer.wrap(res.getBytes("UTF-8")));
						} catch (Exception e) {
						}
						connectionClose();
						log_.warn("execute command:" + requests[i].getRequestURLString());
						return;
					}
					else if(requests[i].getRequestURLString().equals(Utils.getResetCommand())){
						Utils.reload();
						String wk = "properties are reseted.";
						String res = "HTTP/1.1 200 OK\r\nContent-Length:"+wk.length()+"\r\n\r\n"+wk;
						try {
							_channel.write(ByteBuffer.wrap(res.getBytes("UTF-8")));
						} catch (Exception e) {
						}
						connectionClose();
						log_.warn("execute command:" + requests[i].getRequestURLString());
						return;
					}
					else if(requests[i].getRequestURLString().equals(Utils.getSettingsCommand())){
						String wk = "<div><p>Proxy Settings</p><ul>";
						wk += "<li>AccessLogFormat: " + Utils.getAccessLogFormat() + "</li>";
						wk += "<li>AccessLogThreadPriority: " + Utils.getAccessLogThreadPriority() + "</li>";
						wk += "<li>AppHostPorts: " + Utils.getAppHostPorts().toString() + "</li>";
						wk += "<li>CommandableIP: " + Utils.getCommandableIP() + "</li>";
						wk += "<li>ContentsSocketRetryLimit: " + Utils.getContentsSocketRetryLimit() + "</li>";
						wk += "<li>ContentsSocketTimeout: " + Utils.getContentsSocketTimeout() + "</li>";
						wk += "<li>DownloadSpeed: " + Utils.getDownloadSpeed() + "</li>";
						wk += "<li>GetParameterEncode: " + Utils.getRawHttpHeaderEncode() + "</li>";
						wk += "<li>HttpDefaultPort: " + Utils.getHttpDefaultPort() + "</li>";
						wk += "<li>HttpsDefaultPort: " + Utils.getHttpsDefaultPort() + "</li>";
						wk += "<li>HttpsThreadPriority: " + Utils.getHttpsThreadPriority() + "</li>";
						wk += "<li>HttpThreadPriority: " + Utils.getHttpThreadPriority() + "</li>";
						wk += "<li>LocalDNSResolver: " + Utils.getlocalDNSResolver() + "</li>";
						wk += "<li>RemoveGzipAcceptEncode: " + Utils.getRemoveGzipAcceptEncode() + "</li>";
						wk += "<li>RequestRule: " + Utils.getRequestRule() + "</li>";
						wk += "<li>RequestThreadPriority: " + Utils.getRequestThreadPriority() + "</li>";
						wk += "<li>RequestTimeout: " + Utils.getRequestTimeout() + "</li>";
						wk += "<li>ResetCommand: " + Utils.getResetCommand() + "</li>";
						wk += "<li>SettingsCommand: " + Utils.getSettingsCommand() + "</li>";
						wk += "<li>SslHostPorts: " + Utils.getSslHostPorts().toString() + "</li>";
						wk += "<li>SslKey: " + Utils.getSslKey() + "</li>";
						wk += "<li>SslKeyAlgorith: " + Utils.getSslKeyAlgorith() + "</li>";
						wk += "<li>SslKeyPass: " + Utils.getSslKeyPass() + "</li>";
						wk += "<li>SslKeyStoreType: " + Utils.getSslKeyStoreType() + "</li>";
						wk += "<li>SslThreadCount: " + Utils.getSslThreadCount() + "</li>";
						wk += "<li>SslTrust: " + Utils.getSslTrust() + "</li>";
						wk += "<li>SslTrustAlgorith: " + Utils.getSslTrustAlgorith() + "</li>";
						wk += "<li>SslTrustPass: " + Utils.getSslTrustPass() + "</li>";
						wk += "<li>SslTrustStoreType: " + Utils.getSslTrustStoreType() + "</li>";
						wk += "<li>SslType: " + Utils.getSslType() + "</li>";
						wk += "<li>StatsCommand: " + Utils.getStatsCommand() + "</li>";
						wk += "<li>StatsLogFormat: " + Utils.getStatsLogFormat() + "</li>";
						wk += "<li>ThreadPoolSize: " + Utils.getThreadPoolSize() + "</li>";
						wk += "<li>ThroughIOBufferSize: " + Utils.getThroughIOBufferSize() + "</li>";
						wk += "<li>TimeoutCheckInterval: " + Utils.getTimeoutCheckInterval() + "</li>";
						wk += "<li>WriteRetryInterval: " + Utils.getWriteRetryInterval() + "</li>";
						wk += "</ul></div>";

						wk += "<div><p>Error Response Settings</p><ul>";
						wk += "<li>ErrorResCharset: " + Utils.getErrorResCharset() + "</li>";
						wk += "<li>ErrorResContent: " + Utils.getHtmlEncode(Utils.getErrorResContent()) + "</li>";
						wk += "<li>ErrorResContentType: " + Utils.getErrorResContentType() + "</li>";
						wk += "<li>ErrorResResponseCode: " + Utils.getErrorResResponseCode() + "</li>";
						wk += "<li>ErrorResServerName: " + Utils.getErrorResServerName() + "</li>";
						wk += "</ul></div>";

						String res = "HTTP/1.1 200 OK\r\nContent-Length:"+wk.length()+"\r\n\r\n"+wk;
						try {
							_channel.write(ByteBuffer.wrap(res.getBytes("UTF-8")));
						} catch (Exception e) {
						}
						connectionClose();
						log_.warn("execute command:" + requests[i].getRequestURLString());
						return;
					}
				}
				if (this._rejectNewRequestFlag){
					lg.countUpRejectRequest();
					log_.warn("reject:" + new String(requests[i].getRequestURLString()));
					try {
						if(_channel.isConnected())_channel.close();
					} catch (IOException e1) {
					}
					return;
				}
				RawRequest request = requests[i];
				request.setIPPort(_channel.socket().getLocalPort());
				request.setSSL(secureMan!=null);
				UserAgent userAgent = new UserAgent(request);

				// リクエストの格納・処理開始
				if(request.getExtraBytes()==null){
					sendRequest(request, userAgent, null);
				}
				else{
					forPost = new RequestResponsePair(_channel, request, userAgent, this);
					forPost.getLogger().setDevName(userAgent.getDeviceOSVersion());
					forPost.getLogger().setUrl(request.getRequestURLString());
					forPost.getLogger().setUserAgent(request.getUserAgentHeader());

					//リクエストボディ
					String clen = request.getHeaderValue(Consts.REAL_CONTENT_LENGTH_NAME);
					forPost.getLogger().setRcvReqContentLength(clen==null?-1:Integer.parseInt(clen));

					postWeb = ResponseBuilderFactory.getResponseBuilder(this, request, userAgent, forPost.getLogger());
					forPost.setFile(new String(postWeb.getArrangedRequest().getRequestURI().getFile()));

					URL BUI = postWeb.getArrangedRequest().getRequestURI();
					forPost.getLogger().setToUrl(BUI.getProtocol()+"://"+BUI.getAuthority()+BUI.getFile());//ログに接続先を書き込む準備をする

//					postWeb.setDeviceInfoHeader();
					byte[] body = request.getExtraBytes();
					toWeb = postWeb.getWebSocketChannel();
					if(toWeb==null){
						return;
					}
					try {
						byte[] buf = postWeb.getArrangedRequest().getHeaderBytes();
						toWeb.write(ByteBuffer.wrap(buf));
						log_.debug(new String(buf));//デバッグログ
						forPost.getLogger().setRcvReqHeaderLength(buf.length);
						lg.addWebUp(buf.length);
					} catch (IOException e) {
						//リクエスト書き込みエラー
						lg.countUpRequestWriteError();
						try {
							if(toWeb.isConnected())toWeb.close();
						} catch (IOException e1) {
						}
						//TODO: ４０４エラーでも返す？
						continue;
					}
					bodylen = 0;
					chunklenbuf = new ArrayList<Byte>();//
					chunkdata = false;//
					chunklen = 0;//
					chunknow = 0;//
					inBody = sendContentBody(body, body.length);
					request.setExtraBytes(null);
					if(!inBody){
						forPost.getLogger().setSndReqEndTime(System.currentTimeMillis());
						sendRequest((RawRequest)forPost.getRequest(), forPost.getUseragent(), postWeb);
					}
				}
			}
		}
	}
	private boolean inBody = false;
	private SocketChannel toWeb = null;
	private ResponseBuilder postWeb = null;
	private RequestResponsePair forPost = null;
	private int bodylen = 0;
	private ArrayList<Byte> chunklenbuf = new ArrayList<Byte>();//送られるチャンク長バッファ
	private boolean chunkdata = false;//チャンクデータ中
	private int chunklen = 0;//送られるチャンク長
	private int chunknow = 0;//現在のチャンク長
	private boolean sendContentBody(byte[] buffer, int length){
		Stats lg = Stats.getInstance();
		String transe = postWeb.getOriginalRequest().getHeaderValue(Consts.REAL_TRANSFER_ENCODING_NAME);
		boolean chunked = transe==null?false:transe.toLowerCase().equals(Consts.REAL_CHUNKED);
		int clen = 0;
		if(!chunked){
			String wkclen = postWeb.getOriginalRequest().getHeaderValue(Consts.REAL_CONTENT_LENGTH_NAME);
			if(wkclen==null){
				if(length>0)
					clen = length;
				else
					return false;
			}
			else
				clen = Integer.parseInt(wkclen);
		}

		ByteArrayInputStream http = new ByteArrayInputStream(buffer);
		byte[] b = null;
		int l = 0;
		int len = 0;
		boolean isbody = true;
		ArrayList<Byte> debug = new ArrayList<Byte>();
		while(true){
			if(len>=length)break;
			if(chunked)//以下、小さいほうをバッファサイズとしているのは、ByteArrayInputStream.readが読み込んだサイズではなく、バッファのサイズを返してしまうため
				b = new byte[chunklen==0?length:(chunklen>length?length:chunklen)];
			else
				b = new byte[Utils.getThroughIOBufferSize()>length?length:Utils.getThroughIOBufferSize()];
			try {
				l = http.read(b);
				if(l<0)break;
				if(l==0)continue;
				if(!chunked && bodylen+l>clen){//chunkedの場合の連続リクエストは下で対応
					l = clen - bodylen;
				}
				else if(chunked){//chunkedの場合は0改行改行を探してそこまでをウェブに送信する
					int z = 0;
					for(; z < l; z++){
						if(!chunkdata){//チャンクでチャンク長部分の処理
							if(b[z]==0x0A || b[z]==0x0D){//改行なら
								if(chunklenbuf.size()<=0)continue;//前回のチャンクの改行対応
								chunkdata = true;//データ部であるフラグ
								chunklen = Integer.parseInt(Utils.getString(chunklenbuf), 16);//送られるチャンク長
								chunklenbuf.clear();//バッファクリア
								if(z+1<l && b[z]==0x0D && b[z+1]==0x0A){// CRLF 対応
									z++;
								}
							}
							else{
								chunklenbuf.add(b[z]);//送られるチャンク長バッファリング
							}
							continue;
						}
						else
							chunknow++;
						if(chunked && chunkdata && chunklen==0){
							isbody = false;
							int cc = 1;
							while(true){
								if(z+cc < l && (b[z+cc]==0x0D || b[z+cc]==0x0A)){
									z++;
									cc++;
								}
								else
									break;
							}
							break;//chunked の終端
						}
						if(chunked && chunklen<=chunknow){//チャンクの終わり このチェックの前にリクエストの終了をチェックする必要があります。フラグが変わるので
							chunklen = 0;//各種値の初期化
							chunknow = 0;
							chunkdata = false;//チャンク長であるフラグ
						}
					}
					if(l==z)
						isbody = false;
					l = z;
				}
				len+=l;
			} catch (IOException e) {
				break;
			}
			try {
				toWeb.write(ByteBuffer.wrap(b, 0, l));
				if(isDebug){
					for(byte bb : b){
						debug.add(bb);
					}
				}
			} catch (IOException e) {
				//リクエスト書き込みエラー
				lg.countUpRequestWriteError();
				forPost.getLogger().setErrorName(e.getClass().getSimpleName()+":"+e.getMessage());
				return false;
			}
			bodylen+=l;//コンテントレングスチェック用
			if(!chunked && bodylen>=clen){
				isbody = false;
				break;//コンテンツレングスをみる
			}
			if(chunked && !isbody){
				break;
			}
		}
		lg.addClientUp(len);
		lg.addWebUp(len);
		if(isDebug){
			byte[] deb = new byte[len];
			for(int debi = 0; debi < len; debi++)
				deb[debi] = debug.get(debi);
			log_.debug(new String(deb));//デバッグログ
		}
		return isbody;
	}
	private void sendRequest(RawRequest request, UserAgent userAgent, ResponseBuilder getter) {
//		writeDEDE();

		RequestResponsePair RRP = getter!=null?forPost:new RequestResponsePair(_channel, request, userAgent, this);
		Access logger = RRP.getLogger();
		logger.setDevName(userAgent.getDeviceOSVersion());
		logger.setUrl(request.getRequestURLString());
		logger.setUserAgent(request.getUserAgentHeader());
		logger.setWaiting(threadExecuter_.getQueue().size());

		synchronized(responseQueue) {
			_queuedCount++;
			processing++;
			responseQueue.add(_queuedCount);//レスポンスの順番を管理
			RRP.setOrder(_queuedCount);
			_processed++;
		}
		/* リクエストログの出力 */
		log_.debug("[" + _id + "-" + RRP.getOrder() + "]" +  new String(request.getRequestURLString()));

//		// Close Requestの判別
//		String connectionHeader = request.getHeaderValue(CONNECTION_HEADER_NAME);
//		String keepAliveHeader = request.getHeaderValue(KEEP_ALIVE_HEADER_NAME);
//
//		if (_queuedCount >= MAX_KEEP_ALIVE_REQUEST() ||
//				("HTTP/1.0".equals(request.getVersion()) &&
//						(keepAliveHeader==null &&
//								!"keep-alive".equalsIgnoreCase(connectionHeader))) ||
//								("HTTP/1.1".equals(request.getVersion()) &&
//										"close".equalsIgnoreCase(connectionHeader))) {
//			rejectNewRequest();
//		}

		// 元コンテンツからコンテンツを取得している間は
		// タイムアウトが発生しないように工夫する。
		this._lastAccessTime =
			System.currentTimeMillis() + Utils.getRequestTimeout();

		threadExecuter_.execute(new ResponseProcessor(this, RRP, getter));
	}
	public void processResponse(RequestResponsePair RRP, SocketChannel web, ResponseBuilder req){
		SocketChannel channel = _channel;
		Access logger = RRP.getLogger();
		Stats lg = Stats.getInstance();
		if(channel==null){
			logger.setErrorName("channel is null. it stop responsing.");
			Access.doTask(logger);
			return;
		}

		int len = req==null?0:req.buffedBodyCount();//コンテンツの長さ
//		boolean webover = false;
		synchronized (responseQueue) {
			if (RRP.getOrder() != responseQueue.get(0)) {
				while (true) {
					try {
						/**
						 * 以下、実測で遅かったので未使用
						 * ダウンロード中にウェブコンテンツを取得しておくロジック
						if(web!=null && web.isOpen()){
							try {
								boolean chunked = RRP.getResponseLength()==Integer.MAX_VALUE;
								boolean hasClen = RRP.getResponseLength()>0;
								int clen = RRP.getResponseLength();
								if((!chunked && !hasClen) || (hasClen && len >= clen)){//ボディの無いレスポンスかもうレスポンスが終わったもの
								}
								else{
									ByteBuffer reb = ByteBuffer.allocate(Utils.getThroughIOBufferSize());//ワーク領域
									long length = 0;
									long readtime = System.currentTimeMillis();//タイムアウトを計測
									ArrayList<Byte> chlenbuf = new ArrayList<Byte>();//送られるチャンク長バッファ
									int chlen = 0;//送られるチャンク長
									String filename = ((RawRequest)RRP.getRequest()).getRequestURLString();
									int cur = 0;
									boolean endflg = false;
									while(!endflg) {
										if(endflg)break;
										reb.clear();
										length = web.write(reb);
										if(length<=0){//読めなかったとき
											break;//通信が混雑しているなら後方に処理を譲る
//											if(length<0)break;
//											if(len>=clen)break;//なぜかどうしてもbreakできないので
//											//chunked 終了のチェックはここでは行う必要はない。∵読み込まれた時に解る事だから
//											if((readtime + Utils.getContentsSocketTimeout()) > System.currentTimeMillis()){//まだタイムアウト前ならもう一度トライ
//											}
//											else{//タイムアウトしたらログを出して読み込みをあきらめる
//												log_.warn(filename + " -> " + RRP.getFile() + " getting content is time out...");
//												break;
//											}
//											continue;
										}
										else{//読み込めたらタイムスタンプを更新
											readtime = System.currentTimeMillis();
										}
										for(int z = 0; z < length; z++)
											RRP.getResponse().add(reb.get(z));
										len+=length;
										if(chunked){
											while(cur<length){
												byte b = reb.get(cur++);
												if(b==0x0A || b==0x0D){//改行なら
													if(chlenbuf.size()<=0)continue;//前回のチャンクの改行対応
													chlen = Integer.parseInt(Utils.getString(chlenbuf).trim(), 16);//送られるチャンク長
													chlenbuf.clear();//バッファクリア
													if(chlen==0){
														endflg = true;
														break;
													}
													if(b==0x0D){// CRLF 対応
														b = reb.get(cur++);
														if(b!=0x0A)cur--;
													}
													cur += chlen;
												}
												else{
													chlenbuf.add(b);//送られるチャンク長バッファリング
												}
											}
											cur -= length;
										}
										else{
											if(len>=clen)endflg = true;
										}
									}
									lg.addWebDown(len);
									//レスポンス受け取り＆一次変換完了時刻
									logger.setRcvResAndTransEndTime(System.currentTimeMillis());
									//２次変換完了時刻
									logger.setTransEndTime(System.currentTimeMillis());
									//ダウンロードコンテンツバイトをセット
									logger.setSndResContentLength(RRP.getResponse().size());
									//ボディ分がbb.lengthに含まれない為、lenを足しておく
									lg.addClientDown(len);
									webover = true;
									if(web.isOpen())
										web.close();
								}
							} catch (IOException e) {
								try {
									if(web.isOpen())
										web.close();
								} catch (IOException ioe) {
								}
								log_.warn(RequestProcessor.class.getSimpleName() + ":" + Utils.getStackTrace(e));
							}
						}
						 **/
						responseQueue.wait();
					} catch (InterruptedException e) {
						logger.setErrorName(e.getClass().getSimpleName()+":"+e.getMessage());
					}
					if (responseQueue.get(0) == RRP.getOrder())
						break;
				}
			}
		}
		update();
		ArrayList<Byte> response = RRP.getResponse();
		if (response == null || response.size()==0){
			//エラー等でレスポンスがない
			lg.countUpMakeErrorResponseByInner();
			processing--;
			try {
				String resp = Utils.getErrorResponse(((RawRequest)RRP.getRequest()).getRequestURLString());
				byte[] respb = resp.getBytes(Utils.getErrorResCharset());
				channel.write(ByteBuffer.wrap(respb));
				lg.addClientDown(respb.length);
				if(isDebug)log_.debug(new String(respb));//デバッグログ
			} catch (IOException e) {
				//エラーレスポンスの書き込みエラー
				lg.countUpErrorResponseWriteError();
				logger.setErrorName(e.getClass().getSimpleName()+":"+e.getMessage());
				log_.error(Utils.getStackTrace(e));
				try {
					if(channel.isConnected())channel.close();
				} catch (IOException e1) {
				}
			}
			if(processing<=0 && _rejectNewRequestFlag){
				connectionClose();
			}
			synchronized (responseQueue) {
				responseQueue.remove(0);
				responseQueue.notifyAll();
			}
			Access.doTask(logger);
			return;
		}
		try {
			byte[] bb = new byte[response.size()];
			int i = 0;
			for(Byte b : response){
				bb[i] = b;
				i++;
			}

			if(secureMan!=null){
				secureMan.write(bb);
			}
			else{
				ByteBuffer reb = ByteBuffer.wrap(bb);
				long length = 0;
				while(true) {
					update();
					if(channel.isOpen()){
						length = channel.write(reb);
					}
					else
						break;
					if (!reb.hasRemaining()){
						break;
					}
					try {
						if (length == 0) {
							Thread.sleep(Utils.getWriteRetryInterval());
						}
					} catch (InterruptedException e) {
						log_.warn(RequestProcessor.class.getSimpleName() + ":" + Utils.getStackTrace(e));
						logger.setErrorName(e.getClass().getSimpleName()+":"+e.getMessage());
					}
				}
			}
			if(isDebug)log_.debug(Utils.getString(response));//デバッグログ
			if(web!=null){// && !webover){//スルーレスポンスではヘッダまでしかバッファしていないので、残りを流す
				int speed = Utils.getDownloadSpeed();
				ByteBuffer buf = ByteBuffer.allocate(Utils.getThroughIOBufferSize());//ワーク領域
				boolean chunked = RRP.getResponseLength()==Integer.MAX_VALUE;
				boolean hasClen = RRP.getResponseLength()>0;
				int clen = RRP.getResponseLength();
				int l = 0;//コピー長
				int dllen = bb.length;
				long readtime = System.currentTimeMillis();//タイムアウトを計測
				ArrayList<Byte> chlenbuf = new ArrayList<Byte>();//送られるチャンク長バッファ
				int chlen = 0;//送られるチャンク長
				String filename = ((RawRequest)RRP.getRequest()).getRequestURLString();
				int cur = dllen;
				if((!chunked && !hasClen) || (hasClen && len >= clen)){//ボディの無いレスポンスかもうレスポンスが終わったもの
					try {
						if(web.isOpen())
							web.close();
					} catch (IOException ioe) {
					}
					logger.setSndResContentLength(dllen);
					//ダウンロードコンテンツバイトをセット
					logger.setSndResContentLength(clen);
				}
				else{
					boolean endflg = false;
					boolean resetBuffer = false;
					while(true){
						update();
						if(endflg)break;
						if(resetBuffer){
							buf = ByteBuffer.allocate(Utils.getThroughIOBufferSize());
							resetBuffer = false;
						}
						else
							buf.clear();
						if(chunked && dllen > req.headerSize() && len==req.buffedBodyCount()){
							//ここに入るパターンがまだエラーになっている可能性がある
							len = req.headerSize();
							buf = ByteBuffer.wrap(bb, len, dllen - len);
							buf.compact();
							buf.flip();
							cur = 0;
							l = dllen - len;
							dllen = len;
							resetBuffer = true;
						}
						else
							l = web.read(buf);//１バイト読み込み
						if(l<=0){//読めなかったとき
//							if(l<0)break;//なぜかどうしてもbreakできないので
//							if(len>=clen)break;//なぜかどうしてもbreakできないので
							//chunked 終了のチェックはここでは行う必要はない。∵読み込まれた時に解る事だから
							if((readtime + Utils.getContentsSocketTimeout()) > System.currentTimeMillis()){//まだタイムアウト前ならもう一度トライ
							}
							else{//タイムアウトしたらログを出して読み込みをあきらめる
								log_.warn(filename + " -> " + RRP.getFile() + " getting content is time out...");
								break;
							}
							continue;
						}
						else{//読み込めたらタイムスタンプを更新
							readtime = System.currentTimeMillis();
						}
						len+=l;
						dllen+=l;
						if(chunked){
							if(cur<l){
								boolean inchlen = false;
								while(cur < l){
									byte b = buf.get(cur++);
									if(b==0x0A || b==0x0D){//改行なら
										if(!inchlen)continue;
										if(chlenbuf.size()<=0)continue;//前回のチャンクの改行対応
										chlen = Integer.parseInt(Utils.getString(chlenbuf).trim(), 16);//送られるチャンク長
										chlenbuf.clear();//バッファクリア
										if(chlen==0){
											endflg = true;
											break;
										}
										if(b==0x0D){// CRLF 対応
											b = buf.get(cur++);
											if(b!=0x0A)cur--;
										}
										cur += chlen;
										inchlen = false;
									}
									else{
										chlenbuf.add(b);//送られるチャンク長バッファリング
										inchlen = true;
									}
								}
							}
							cur -= l;
						}
						else{
							if(len>=clen)endflg = true;
						}
						if(resetBuffer)continue;
						if(isDebug)log_.debug(new String(buf.array(), 0, l));//デバッグログ
						int length = 0;
						if(speed<=0){
							buf.flip();
							while(true) {
								length = 0;
								if(channel.isOpen()){
									length = channel.write(buf);
								}
								else
									break;
								if(!buf.hasRemaining()){
									break;
								}
								update();
								try {
									if (length == 0) {
										Thread.sleep(Utils.getWriteRetryInterval());
									}
								} catch (InterruptedException e) {
									log_.warn(RequestProcessor.class.getSimpleName() + ":" + Utils.getStackTrace(e));
									logger.setErrorName(e.getClass().getSimpleName()+":"+e.getMessage());
								}
							}
							update();
						}
						else{
							ByteBuffer limited = ByteBuffer.allocate(l<=speed?l:speed);
							int size = 0;
							long btm = System.currentTimeMillis();
							byte[] raw = buf.array();
							while(true) {
								limited.clear();
								int lim = (l<=speed?l:((size+speed)>=l?l-size:speed));
								if(lim+size>l)lim = l-size;
								limited.put(raw, size, lim);
								limited.flip();
								limited.limit(lim);
								length = 0;
								while (true) {
									if (channel.isOpen()) {
										length += channel.write(limited);
									} else
										break;
									if(!limited.hasRemaining())break;
									update();
								}
								update();
								if((size+length)>=l){
									break;
								}
								try {
									if (length == 0) {
										Thread.sleep(Utils.getWriteRetryInterval());
									}
								} catch (InterruptedException e) {
									log_.warn(RequestProcessor.class.getSimpleName() + ":" + Utils.getStackTrace(e));
									logger.setErrorName(e.getClass().getSimpleName()+":"+e.getMessage());
								}
								long tm = System.currentTimeMillis() - btm;
								size += length;
								if(tm >= 1 && (size/tm)>=speed){
									long waittm = Math.round(size / speed) - tm;
									try {
										Thread.sleep(waittm);
									} catch (InterruptedException e) {
										log_.warn(RequestProcessor.class.getSimpleName() + ":" + Utils.getStackTrace(e));
										logger.setErrorName(e.getClass().getSimpleName()+":"+e.getMessage());
									}
								}
							}
						}
					}
				}
				lg.addWebDown(dllen);
				//ダウンロードコンテンツバイトをセット
				logger.setSndResContentLength(dllen);
				//ボディ分がbb.lengthに含まれない為、lenを足しておく
				lg.addClientDown(len);
			}
			else{
				logger.setSndResContentLength(response.size());
				//ダウンロードコンテンツバイトをセット
				logger.setSndResContentLength(0);
			}
			lg.addClientDown(bb.length);
			//正常レスポンス数
			lg.countUpFineResponse();
		} catch (IOException e) {
			lg.countUpResponseWriteError();
			log_.warn(RequestProcessor.class.getSimpleName() + ":" + Utils.getStackTrace(e));
			logger.setErrorName(e.getClass().getSimpleName()+":"+e.getMessage());
			connectionClose();
		}
		finally{
			try {
				if(web != null && web.isOpen())
					web.close();
			} catch (IOException e) {
			}
		}
		synchronized (responseQueue) {
			responseQueue.remove(0);
			responseQueue.notifyAll();
		}
		processing--;
		if(processing<=0 && this._rejectNewRequestFlag && !this._requestFactory.requesting()){
			connectionClose();
		}
		//レスポンス完了時刻
		logger.setSndResEndTime(System.currentTimeMillis());
		Access.doTask(logger);
	}
}
