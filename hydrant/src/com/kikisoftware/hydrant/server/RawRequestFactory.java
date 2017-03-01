package com.kikisoftware.hydrant.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.LinkedList;


import org.apache.log4j.Logger;

import com.kikisoftware.hydrant.Consts;
import com.kikisoftware.hydrant.Utils;

public class RawRequestFactory {
	private static final Logger log_ = Logger.getLogger(RawRequestFactory.class);
	final LinkedList<RawRequest> _completedRequestList = new LinkedList<RawRequest>();
	RawRequest _processingRequest;
	protected InetAddress _ipaddr;
	public InetAddress getIPAddress(){return this._ipaddr;}
	public void setIPAddress(InetAddress ip){this._ipaddr = ip;}
	public RawRequestFactory() {
		this._processingRequest = new RawRequest();
	}
	public Request[] process(byte[] buffer) {
		return process(buffer, 0, buffer.length);
	}

	private ArrayList<Byte> bbb = new ArrayList<Byte>();
	private ByteArrayOutputStream hline = new ByteArrayOutputStream();
	private boolean nol = true;//content-length未処理フラグ
	private int clen = 0;//content-length
	private boolean chunked = false;//

	public synchronized boolean requesting(){
		if(bbb.size()>0 || _completedRequestList.size()>0){
			return true;
		}
		return false;
	}
	
	/**
	 *
	 * @param buffer
	 * @param offset
	 * @param length
	 * @return
	 */
	public RawRequest[] process(byte[] buffer, int start, int length) {
		ByteArrayInputStream http = new ByteArrayInputStream(buffer, start, length);
		byte[] b = new byte[1];
		int l = 0;
		int len = 0;
		try {
			LOOP:
			while(true){
				while(true){
					if(len>=length)break LOOP;
					l = http.read(b);//１バイト読み込み
					len++;
					if(l<0){//読めなかったとき
						break LOOP;
					}
					if(bbb.size()==0 && b[0]==0x00)continue;
					if(b[0] == 0x0D || b[0] == 0x0A){
						String hd = hline.toString();
						if(nol && hd.toLowerCase().startsWith(Consts.LOWER_CONTENT_LENGTH_FOR_CHECK)){//コンテンツの大きさを取得
							try {
								clen = Integer.parseInt(hd.toLowerCase().replace(Consts.LOWER_CONTENT_LENGTH, ""));//通信の終わりを取得する
								nol = false;
							} catch (NumberFormatException e) {
								log_.warn(RawRequestFactory.class.getSimpleName() + ":" + Utils.getStackTrace(e));
							}
						}
						else if(hd.toLowerCase().startsWith(Consts.LOWER_TRANSFER_ENCODING_FOR_CHECK)){//コンテンツの大きさを取得
							if(hd.toLowerCase().replace(Consts.LOWER_TRANSFER_ENCODING, "").equals(Consts.REAL_CHUNKED)){
								chunked = true;//チャンク
								clen = Integer.MAX_VALUE;
							}
						}
						int mx = bbb.size()-1;
						if(bbb.size()>=4){//ヘッダ部の終わり確認（CRLF）
							if(bbb.get(mx-2)==0x0D && bbb.get(mx-1)==0x0A && bbb.get(mx)==0x0D && b[0]==0x0A){
								break;
							}
						}
						if(bbb.size()>=2){//ヘッダ部の終わり確認（CRとLF）
							if(bbb.get(mx)==0x0A && b[0]==0x0A){
								break;
							}
							else if(bbb.get(mx)==0x0D && b[0]==0x0D){
								break;
							}
						}
						if(!hd.equals("") && this._processingRequest.getVersion()==null){
							try {
								this._processingRequest.parseRequestLine(hline.toByteArray());
							}catch(Exception e) {
							}
						}
						else{
							String[] headers = new String(hline.toByteArray()).split(":", 2);
							String key = (headers[0]==null?"":headers[0].trim());
							if(!key.equals("")){
								String val = (headers.length<=1||headers[1]==null?"":headers[1].trim());
								this._processingRequest.setHeader(key, val);
							}
						}
						hline.reset();
					}
					else
						hline.write(b[0]);//ヘッダ部をバッファリング
					bbb.add(b[0]);
				}

				if(l>=0 && (clen > 0 || chunked)){
					boolean chunkdata = false;//チャンクデータ中
					ArrayList<Byte> chunklenbuf = new ArrayList<Byte>();//送られるチャンク長バッファ
					int chunklen = 0;//送られるチャンク長
					int chunknow = 0;//現在のチャンク長
					ArrayList<Byte> wk = new ArrayList<Byte>();
					while(0<(l = http.read(b))){
						len++;
						wk.add(b[0]);//ここではchunkedのまま送信する
						bbb.add(b[0]);
						if(chunked && !chunkdata){//チャンクでチャンク長部分の処理
							if(b[0]==0x0A || b[0]==0x0D){//改行なら
								if(chunklenbuf.size()<=0)continue;//前回のチャンクの改行対応
								chunkdata = true;//データ部であるフラグ
								chunklen = Integer.parseInt(Utils.getString(chunklenbuf), 16);//送られるチャンク長
								chunklenbuf.clear();//バッファクリア
								if(chunklen==0)break;//ボディの終了
								if(b[0]==0x0D){// CRLF 対応
									l = http.read(b);
									wk.add(b[0]);//chunked のまま送信する
									if(b[0]==0x0A)continue;
								}
							}
							else{
								chunklenbuf.add(b[0]);//送られるチャンク長バッファリング
								continue;
							}
						}
						chunknow++;//チャンクカーソル
						if(!chunked && wk.size()>=clen)break;//コンテンツレングスをみる
						if(chunked && chunkdata && chunklen==0)break;//chunked の終端
						if(chunked && chunklen<=chunknow){//チャンクの終わり
							chunklen = 0;//各種値の初期化
							chunknow = 0;
							chunkdata = false;//チャンク長であるフラグ
						}
					}
					clen = 0;
					nol = true;
					chunked = false;
					byte[] body = new byte[wk.size()];
					for(int i = 0; i < wk.size(); i++){
						body[i] = wk.get(i);
					}
					this._processingRequest.setExtraBytes(body);
					this._completedRequestList.addLast(this._processingRequest);
					this._processingRequest = new RawRequest();
					bbb.clear();
					if(l>0)continue;
					break;
				}
				this._completedRequestList.addLast(this._processingRequest);
				this._processingRequest = new RawRequest();
				bbb.clear();
				hline.close();
				nol = true;
				clen = 0;
				chunked = false;
			}
			http.close();
		} catch (IOException e) {
			//throws したくないので、catchするが、発生の可能性はほとんど無い（メモリが壊れたとか？）
			log_.warn(RawRequestFactory.class.getSimpleName() + ":" + Utils.getStackTrace(e));
		}
		/* 戻り値作成 */
		RawRequest[] returnValue = null;
		if (this._completedRequestList.size() > 0) {
			returnValue = new RawRequest[this._completedRequestList.size()];
			for(int i=0 ; i<this._completedRequestList.size() ; i++) {
				returnValue[i] = this._completedRequestList.get(i);
				returnValue[i].setIPAddress(this._ipaddr.getHostAddress());
			}
			this._completedRequestList.clear();
		}
		return returnValue;
	}
}
