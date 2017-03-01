package com.kikisoftware.hydrant.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.kikisoftware.hydrant.Consts;
import com.kikisoftware.hydrant.Utils;
import com.kikisoftware.hydrant.log.Access;
import com.kikisoftware.hydrant.log.Stats;
import com.kikisoftware.hydrant.ua.UserAgent;

/**
 * 指定されたリクエストでコンテンツを取得し、必要な変換をかけて
 * レスポンスバイト配列を返すクラス
 */
public class WebRequest extends RequestController {
	static{
		log_ = Logger.getLogger(WebRequest.class);
	}
	/**
	 *
	 * @param originalRequest
	 * @param userAgent
	 */
	public WebRequest(RequestProcessor proc, RawRequest originalRequest,UserAgent userAgent,Access logger) {
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
		Stats lg = Stats.getInstance();
		RewroteRequest sendreq = this._arrangedRequest;//リクエストオブジェクト
		RawRequest rcvreq = this._originalRequest;
		URL BUI = sendreq.getRequestURI();//リクエストオブジェクトから接続先情報を取得
		ArrayList<Byte> bb = new ArrayList<Byte>();
		int clen = 0;
		boolean locationReplaced = false;

		bodyChannel = channel;
		ByteArrayOutputStream hline = new ByteArrayOutputStream();
		try {
			ByteBuffer buf = ByteBuffer.allocate(Utils.getThroughIOBufferSize());//ワーク領域
			byte b;
			boolean start = false;//レスポンスbodyの始まりを告げるフラグ
			int l = 0;//コピー長
			long readtime = System.currentTimeMillis();//タイムアウトを計測
			CONTENTS_GET:
			while(true){
				_recproc.update();
				buf.clear();//.compact();//
				l = channel.read(buf);//読み込み
				buf.flip();
				if(l<=0){//読めなかったとき
					if((readtime + Utils.getContentsSocketTimeout()) > System.currentTimeMillis()){}//まだタイムアウト前ならもう一度トライ
					else{//タイムアウトしたらログを出して読み込みをあきらめる
						log_.warn(rcvreq.getRequestURLString() + "->" + BUI.getFile() + " getting content is timed out...");
						int limit = buf.limit();
						for (int i = 0; i < limit; i++) {
							bb.add(buf.get());
						}
						headerSize = bb.size();
						break;
					}
					continue;
				}
				else{//読み込めたらタイムスタンプを更新
					readtime = System.currentTimeMillis();
				}
				int limit = buf.limit();
				for (int i = 0; i < limit; i++) {
					int mx = bb.size()-1;//現在の読み込まれたデータ長
					b = buf.get();
					if(contentType.equals("") || clen==0 || !locationReplaced){
						if(b == 0x0D || b == 0x0A){// 改行なら
							String hdl = hline.toString().toLowerCase();//ヘッダ部を１行取得
							if(hdl.startsWith(Consts.LOWER_CONTENT_TYPE_FOR_CHECK)){//content-type なら
								contentType = Utils.getRequestHeaderValue(hdl);
							}
							else if(hdl.startsWith(Consts.LOWER_CONTENT_LENGTH_FOR_CHECK)){//content-length なら
								responseLength = Integer.parseInt(Utils.getRequestHeaderValue(hdl));//通信の終わりを取得する
							}
							else if(hdl.startsWith(Consts.LOWER_TRANSFER_ENCODING_FOR_CHECK)){//コンテンツの大きさを取得
								if(Utils.getRequestHeaderValue(hdl).equals(Consts.REAL_CHUNKED)){
									responseLength = Integer.MAX_VALUE;
								}
							}
							else if(hdl.startsWith(Consts.LOWER_LOCATION_FOR_CHECK)){//Locationヘッダはhost:portを当サーバーに変更
								int hlen = hline.size();
								for(int x = 0; x < hlen; x++)
									bb.remove(bb.size()-1);
								String newh = hline.toString().replace((BUI.getPort()==Integer.parseInt(Utils.getHttpDefaultPort())?BUI.getHost():BUI.getAuthority()), rcvreq.getHostPort());
								for(byte hb : Utils.getRequestBytes(newh)){
									bb.add(hb);
								}
								locationReplaced = true;
							}
							hline.reset();//一度操作したらヘッダを消去
						}
						else//ヘッダ部をバッファリング
							hline.write(b);
					}
					if(bb.size()>=3){//ヘッダ部の終わり確認（CRLF）
						if(bb.get(mx-2)==0x0D && bb.get(mx-1)==0x0A && bb.get(mx)==0x0D && b==0x0A){// cr lf cr lf
							start = true;
						}
					}
					if(bb.size()>=1){//ヘッダ部の終わり確認（CRとLF）
						if(bb.get(mx)==0x0A && b==0x0A){// lf lf
							start = true;
						}
						else if(bb.get(mx)==0x0D && b==0x0D){// cr cr
							start = true;
						}
					}
					if(start){
						bb.add(b);//読み込んだバッファもレスポンスする
						headerSize = bb.size();
						buffedBody = limit - i - 1;
						for (i++; i < limit; i++) {
							bb.add(buf.get());
						}
						if(bb.size()<=4){
							boolean crlf = true;
							for(byte cb : bb){//改行のみであればログを出力する
								if(cb != 0x0D && cb != 0x0A){
									crlf = false;
									break;
								}
							}
							if(crlf)log_.warn(new String(BUI.getFile()) + " 0 byte response.");
						}
						break CONTENTS_GET;
					}
					bb.add(b);
				}
				if(channel.socket().isInputShutdown())break;//ソケットに異常があれば終了
			}
		} catch (NumberFormatException ne){
			//数値エラー
			lg.countUpResopnseReadError();
			_logger.setErrorName(ne.getClass().getSimpleName()+":"+ne.getMessage());
			log_.warn(new String(BUI.getFile()) + " This Content-Length was illegal. The system failed parsing a number.");
		} catch (IOException e2) {
			//レスポンス読み込みエラー
			lg.countUpResopnseReadError();
			_logger.setErrorName(e2.getClass().getSimpleName()+":"+e2.getMessage());
			log_.error(Utils.getStackTrace(e2));
		} catch (Exception ec) {
			//コンテンツ取得で発生した認識していないエラー
			lg.countUpUnKnownErrorWithWeb();
			_logger.setErrorName(ec.getClass().getSimpleName()+":"+ec.getMessage());
			log_.error(Utils.getStackTrace(ec));
		}
		finally {
			try {
				if(bodyChannel==null && channel != null && channel.isOpen())
					channel.close();
			} catch (IOException e) {
				log_.error(Utils.getStackTrace(e));
			}
		}
		//レスポンスヘッダ受信完了時刻
		_logger.setRcvResHeadTime(System.currentTimeMillis());
		_logger.setSndResHeaderLength(headerSize);
		_logger.setSndContentType(contentType);
		return bb;
//		lg.addWebDown(dllen);
//		//レスポンス受け取り＆一次変換完了時刻
//		_logger.setRcvResAndTransEndTime(System.currentTimeMillis());
//
//		if(img!=null){//この条件は画像ということ
//			ImageConverter sc;
//			byte[] reimg;
//			try {//画像変換クラスを動的に呼び出して画像を変換する
//				sc = (ImageConverter)imageConverter.getConstructor(CONSTRUCTOR_ARGS).newInstance(new Object[]{img, ctype});
//				reimg = sc.getConverted(_originalRequest.getRequestURLString());
//			} catch (Exception e) {
//				//画像変換エラー
//				lg.countUpTranslateImageError();
//				_logger.setErrorName(e.getClass().getSimpleName()+":"+e.getMessage());
//				log_.warn("Loading ImageConverter error occur." + ctype + "\n" + Helper.getStackTrace(e));
//				reimg = img;
//			}
//			for(byte ib : reimg)
//				bb.add(ib);//変換後の画像バイト配列をバッファに書き込む
//		}
//		else
//			bb = hc.convertHeadJSCSS(bb);//タグテキストならＣＳＳ、ＪＳを指定箇所に書き込む
//		//２次変換完了時刻
//		_logger.setTransEndTime(System.currentTimeMillis());
//
//		responseLength = bb.size()-hsize;//変換後のコンテンツの大きさ
//		contentType = ctype;//ＭＩＭＥタイプ
//
//		//以下、content-length と connection の値を変更する為のループ
//		ArrayList<Byte> bbb = new ArrayList<Byte>();
//		hline = new ByteArrayOutputStream();
//		boolean start = false;//responseボディの始まりを告げるフラグ
//		boolean nol = true;//content-length未処理フラグ
//		boolean noc = true;//connection未処理フラグ
//		for(Byte b : bb){
//			if(!start) {//ヘッダ部は処理続行
//				if(b == 0x0D || b == 0x0A){
//					String hdl = hline.toString().toLowerCase();
//					if(textExchange && hc.getCharset()!=null && hdl.startsWith(Utils.LOWER_CONTENT_TYPE_FOR_CHECK)){//ＨＴＭＬテキスト変換で、content-type なら
//						ctype = Utils.getRequestHeaderValue(hline);
//						if(!ctype.contains(Utils.LOWER_CHARSET_FOR_CHECK)){//文字コードがない場合
//							String charset = "; charset="+hc.getCharset();
//							for(byte w : charset.getBytes()){//新しい値を書き込む
//								bbb.add(w);
//							}
//						}
//					}
//					if(nol && hdl.startsWith(Utils.LOWER_CONTENT_LENGTH_FOR_CHECK)){//変換によって変更されたコンテンツの大きさを記載しなおす
//						while(bbb.size()>0 && bbb.get(bbb.size()-1)!=0x0A && bbb.get(bbb.size()-1)!=0x0D){//値の始まりまでバッファを削除
//							bbb.remove(bbb.size()-1);
//						}
//						String rellen = Utils.REAL_CONTENT_LENGTH+Integer.toString(responseLength);
//						for(byte w : rellen.getBytes()){//新しい値を書き込む
//							bbb.add(w);
//						}
//						nol = false;//content-length 終了
//					}
//					if(noc && hdl.startsWith(Utils.LOWER_CONNECTION_FOR_CHECK)){//connection ヘッダを変更する
//						while(bbb.size()>0 && bbb.get(bbb.size()-1)!=0x0A && bbb.get(bbb.size()-1)!=0x0D){//値の始まりまでバッファを削除
//							bbb.remove(bbb.size()-1);
//						}
//						String rellen = Utils.REAL_CONNECTION + (closeConnection ? Utils.REAL_CLOSE : Utils.REAL_KEEP_ALIVE);//新しい値を指示により変える
//						for(byte w : rellen.getBytes()){//新しい値を書き込む
//							bbb.add(w);
//						}
//						noc = false;//connection 終了
//					}
//					if(hdl.startsWith(Utils.LOWER_TRANSFER_ENCODING_FOR_CHECK) && hdl.contains(Utils.REAL_CHUNKED)){//エンコーディングはしない（chunkedではなくなる為の対応）
//						while(bbb.size()>0 && bbb.get(bbb.size()-1)!=0x0A && bbb.get(bbb.size()-1)!=0x0D){//値の始まりまでバッファを削除
//							bbb.remove(bbb.size()-1);
//						}
//						boolean a = false;
//						boolean d = false;
//						while(bbb.size()>0){
//							Byte lst = bbb.get(bbb.size()-1);
//							if(lst==0x0A){
//								if(a)d=true;
//								a = true;
//							}
//							else if(lst==0x0D){
//								if(d)a=true;
//								d = true;
//							}
//							else
//								break;
//							bbb.remove(bbb.size()-1);//改行分バッファを削除
//							if(a && d)break;
//						}
//					}
//					int mx = bbb.size()-1;
//					if(bbb.size()>=3){//ヘッダ部の終わり確認（CRLF）
//						if(bbb.get(mx-2)==0x0D && bbb.get(mx-1)==0x0A && bbb.get(mx)==0x0D && b==0x0A){
//							if(nol){
//								bbb.remove(bbb.size()-1);//ヘッダの最後にContent-Lengthを埋め込むため0x0Dを削除
//								String contlen = Utils.REAL_CONTENT_LENGTH+Integer.toString(responseLength);
//								for(byte w : contlen.getBytes()){//新しい値を書き込む
//									bbb.add(w);
//								}
//								bbb.add((byte)0x0D);
//								bbb.add((byte)0x0A);
//								bbb.add((byte)0x0D);
//								nol = false;//content-length 終了
//							}
//							start = true;
//						}
//					}
//					if(bbb.size()>=2){//ヘッダ部の終わり確認（CRとLF）。HTTP的には許されていないが、もしものための処理
//						if(bbb.get(mx)==0x0A && b==0x0A){
//							if(nol){
//								String contlen = Utils.REAL_CONTENT_LENGTH+Integer.toString(responseLength);
//								for(byte w : contlen.getBytes()){//新しい値を書き込む
//									bbb.add(w);
//								}
//								bbb.add((byte)0x0A);
//								nol = false;//content-length 終了
//							}
//							start = true;
//						}
//						else if(bbb.get(mx)==0x0D && b==0x0D){
//							if(nol){
//								String contlen = Utils.REAL_CONTENT_LENGTH+Integer.toString(responseLength);
//								for(byte w : contlen.getBytes()){//新しい値を書き込む
//									bbb.add(w);
//								}
//								bbb.add((byte)0x0D);
//								nol = false;//content-length 終了
//							}
//							start = true;
//						}
//					}
//					hline.reset();
//				}
//				else
//					hline.write(b);//ヘッダ部をバッファリング
//			}
//			bbb.add(b);
//		}
//		_logger.setSndResContentLength(responseLength);
//		return bbb;//コンテンツを返す
	}
}




