package com.kikisoftware.hydrant.server;

import java.nio.channels.SocketChannel;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.kikisoftware.hydrant.log.Access;
import com.kikisoftware.hydrant.ua.UserAgent;

/**
 * 指定されたリクエストでコンテンツを取得し、必要な変換をかけて
 * レスポンスバイト配列を返すクラス
 */
public class WebRequestThrough extends RequestController {
	static{
		log_ = Logger.getLogger(WebRequestThrough.class);
	}
	/**
	 *
	 * @param originalRequest
	 * @param userAgent
	 */
	public WebRequestThrough(RequestProcessor proc, RawRequest originalRequest,UserAgent userAgent,Access logger) {
		super(proc, originalRequest, userAgent, logger);
	}

	/**
	 * コンテンツ取得と変換を行う
	 * このメソッドが変更になる場合は、RequestProcessor#processResponseの
	 * 変更も検討する事（スルーレスポンスモードの際の処理に同じようなロジックがあるため）
	 * @param closeConnection
	 * @param channel
	 * @return
	 */
	public ArrayList<Byte> getContents(boolean closeConnection, SocketChannel channel){
		bodyChannel = channel;
		return new ArrayList<Byte>();
	}
}




