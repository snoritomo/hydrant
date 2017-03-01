package com.kikisoftware.hydrant.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.kikisoftware.hydrant.Utils;
import com.kikisoftware.hydrant.log.Access;
import com.kikisoftware.hydrant.log.Stats;
import com.kikisoftware.hydrant.rewrite.Rewrite;
import com.kikisoftware.hydrant.ua.UserAgent;


public abstract class RequestController implements ResponseBuilder {
	protected int headerSize = 0;
	@Override
	public int headerSize() {
		return headerSize;
	}
	protected int buffedBody = 0;
	@Override
	public int buffedBodyCount() {
		return buffedBody;
	}
	protected static Logger log_;

	protected RequestProcessor _recproc;

	/**
	 * 端末から来たリクエストを保存しておくもの。
	 * 対外形式のURLが格納されている。
	 * 参照のみ行うこととする｡
	 */
	protected RawRequest _originalRequest;

	/**
	 * クライアントの機種情報
	 */
	protected UserAgent _userAgent;

	/**
	 * 修正を加えたリクエスト
	 * 実際のコンテンツへのリクエストを含んでいる｡
	 */
	protected RewroteRequest _arrangedRequest;

	/**
	 * 適用したコンテンツ取得先ルールを格納する
	 */
	protected Rewrite _rule;

	/**
	 *
	 */
	protected Access _logger;

	/**
	 * スルーレスポンスモードの際にここにインスタンスが入る。普段はnull
	 */
	protected SocketChannel bodyChannel;

	/**
	 * デフォルトコンストラクタは外部に非公開
	 */
	protected RequestController(){}

	/**
	 *
	 * @param originalRequest
	 * @param userAgent
	 */
	public RequestController(RequestProcessor proc, RawRequest originalRequest,UserAgent userAgent,Access logger) {
		_recproc = proc;
		_originalRequest = originalRequest;
		_userAgent = userAgent;
		_logger = logger;
	}

	/**
	 *
	 * @return
	 */
	public RawRequest getOriginalRequest() {return _originalRequest;}

	/**
	 *
	 * @return
	 */
	public RewroteRequest getArrangedRequest() {return _arrangedRequest;}

	/**
	 *
	 * @return
	 */
	public UserAgent getUserAgent() {return _userAgent;}

	/**
	 *
	 * @return
	 */
	public Rewrite getRule() {return _rule;}

	/**
	 * スルーレスポンスモードではインスタンスが返る。その他はnull
	 * @return the bodyChannel
	 */
	public SocketChannel getBodyChannel() {
		return bodyChannel;
	}

	/**
	 * リトライ回数 contentsSocketRetryLimitで指定
	 */
	public static int RETRY_LIMIT(){return Integer.parseInt(Utils.getContentsSocketRetryLimit());}

	/**
	 *
	 * @param request
	 */
	public void setArrangedRequest(RewroteRequest request) {
		_arrangedRequest = request;
	}

	/**
	 *
	 * @param rule
	 */
	public void setRule(Rewrite rule) {
		_rule = rule;
		rule.applyToRequest(this._arrangedRequest);
	}

	private SocketChannel opened = null;
	public SocketChannel getWebSocketChannel(){
		if(opened!=null)return opened;
		Stats lg = Stats.getInstance();
		URL BUI = _arrangedRequest.getRequestURI();//リクエストオブジェクトから接続先情報を取得
		String host = new String(BUI.getHost());//接続先ホスト
		int port = BUI.getPort();//接続先ポート

		_logger.setToUrl(BUI.getProtocol()+"://"+BUI.getAuthority()+BUI.getFile());//ログに接続先を書き込む準備をする

		int retry = 0;
		while(retry<RETRY_LIMIT()){//接続失敗時のリトライを設定値分行う
			InetSocketAddress ia = new InetSocketAddress(host, port);//アドレス・ポート
			try {
				opened = SocketChannel.open(ia);//接続
				break;
			} catch (IOException e) {//全ての例外を拾っているが、本当のターゲットは BindException
				//リクエストリトライ数
				_logger.setSndReqRetry(++retry);
			} catch (UnresolvedAddressException e){
				lg.countUpConnectWebServerFailure();
				log_.error("connecting to web-server["+host+":"+port+"] is failure. give up get contents.");
				return null;
			}
		}
		if(retry>=RETRY_LIMIT()){
			log_.error("Connecting to web server was tried "+retry+" times. It was over setting [contentsSocketRetryLimit].");
			lg.countUpConnectWebServerFailure();
		}
		return opened;
	}

	/**
	 * コンテンツを取得しながら変換する。最後に Content-Length と
	 * Keep-Alive をコントロールしてレスポンスバイト配列を返す
	 * @param closeConnection キープアライブを終了する場合は true
	 * @return レスポンス
	 */
	public ArrayList<Byte> getContents(boolean closeConnection){
		Stats lg = Stats.getInstance();
		RewroteRequest RewroteRequest = this._arrangedRequest;//リクエストオブジェクト

		ArrayList<Byte> bb = new ArrayList<Byte>();

		SocketChannel channel = getWebSocketChannel();

		if(channel == null || !channel.isOpen() || !channel.isConnected() || channel.socket().isClosed()){
			if(channel==null){
				log_.error("Web Server connection was lost");
			}
			else{
				log_.error("Web Server channel was invalid isOpen:"+channel.isOpen()+" isConnected:"+channel.isConnected());
			}
			return null;//接続が無効なら null を返す
		}

		try {
			byte[] buf = RewroteRequest.getHeaderBytes();
			channel.write(ByteBuffer.wrap(buf));
			lg.addWebUp(buf.length);
			_logger.setRcvReqHeaderLength(buf.length);
			log_.debug(new String(buf));//デバッグログ
			//リクエスト完了時刻
			_logger.setSndReqEndTime(System.currentTimeMillis());
			bb = getContents(closeConnection, channel);
		} catch (IOException e) {
			//リクエスト書き込みエラー
			lg.countUpRequestWriteError();
			_logger.setErrorName(e.getClass().getSimpleName()+":"+e.getMessage());
			log_.error(Utils.getStackTrace(e));
		}
		finally {
			try {
				if(bodyChannel==null && channel != null && channel.isOpen())
					channel.close();
			} catch (IOException e) {
				log_.error(Utils.getStackTrace(e));
			}
		}
		return bb;//コンテンツを返す
	}

	protected int responseLength = 0;
	protected String contentType = "";

	/**
	 * @return the responseLength
	 */
	public int getResponseLength() {
		return responseLength;
	}
	/**
	 * @return the contentType
	 */
	public String getResponseContentType() {
		return contentType;
	}
}
