package com.kikisoftware.hydrant.server;

import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;

import com.kikisoftware.hydrant.Consts;
import com.kikisoftware.hydrant.Utils;


public class RawRequest extends Request {
	protected String _method;
	protected byte[] _requestURI; //日本語が含まれる可能性があるためbyte配列
	protected String _version;
	protected LinkedHashMap<Integer, Entry<String, String>> _requestHeader; // (key,value)=(String,String)
	protected InputStream _body; //bodyが無い場合はnull
	protected byte[] extraBytes;//送信すべきボディの一部
	
	public byte[] getExtraBytes() {
		return extraBytes;
	}
	public void setExtraBytes(byte[] extraBytes) {
		this.extraBytes = extraBytes;
	}

	protected String _ipaddr;
	public String getIPAddress(){return this._ipaddr;}
	public void setIPAddress(String ip){this._ipaddr = ip;}

	protected int _ipport;
	public int getIPPort(){return this._ipport;}
	public void setIPPort(int port){this._ipport = port;}

	public RawRequest() {
		parameterInit();
	}

	public RawRequest(RawRequest src) {
		this._method = src._method;
		if (src._requestURI != null) {
			this._requestURI = new byte[src._requestURI.length];
			System.arraycopy(src._requestURI,0,this._requestURI,0,this._requestURI.length);
		}
		else {
			this._requestURI = null;
		}
		this._version = src._version;
		this._requestHeader = new LinkedHashMap<Integer, Entry<String, String>>(src._requestHeader);
	}

	private void parameterInit() {
		this._method = null;
		this._requestURI = null;
		this._version = null;
//		this._body = null;
		if (this._requestHeader == null)
			this._requestHeader = new LinkedHashMap<Integer, Entry<String, String>>();    
		else
			this._requestHeader.clear();
	}

	public String getMethod() {return this._method;}
	public byte[] getRequestURI() {return this._requestURI;}
	public String getRequestURLString() {
		return Utils.getRequestString(this._requestURI);
	}
	public String getHostPort(){
		String re = getHeaderValue(Consts.REAL_HOST_NAME);
		if(re.isEmpty())re = _ipaddr+":"+_ipport;
		return re;
	}
	public String getVersion() {return this._version;}
	public Map<Integer, Entry<String, String>> getRequestHeader() {return this._requestHeader;}

	public InputStream getBody() {return this._body;}

	public void setMethod(String method) {this._method = method;}
	public void setRequestURI(byte[] requestURI) {this._requestURI = requestURI;}
	public void setVersion(String version) {this._version = version;}

	/**
	 * メッセージボディを設定する<br>
	 * この関数でbodyを設定しておくと、getBytesする際に設定されたbodyの長さを
	 * "Content-Length"ヘッダに設定すした結果を出力する｡
	 * ボディを空にしたい場合はnullを設定する｡
	 * @param セットするコンテンツボディ
	 */
	public void setBody(InputStream is){this._body = is;}

	/**
	 * ヘッダ操作系
	 * レスポンスヘッダの名前の部分は
	 *「先頭文字と"-"の後の文字が大文字でそれ以外は小文字」
	 * というように正規化して格納する。
	 * get,removeで指定するnameは *正規化されたもの* を指定する｡
	 * @param name httpヘッダ名
	 * @return httpヘッダ値
	 */
	@Override
	public String getHeaderValue(String name) {
		String wkname = normalizeHeaderName(name);
		String re = "";
		for(Iterator<Integer> i = this._requestHeader.keySet().iterator();i.hasNext();){
			Entry<String, String> e = this._requestHeader.get(i.next());
			if(e.getKey().equals(wkname)){
				re = e.getValue();
				break;
			}
		}
		return re;
	}
	/**
	 * 
	 * @param name  httpヘッダ名
	 * @return ???
	 */
	public String removeHeader(String name) {
		String wkname = normalizeHeaderName(name);
		String re = "";
		synchronized (this._requestHeader) {
			for (Iterator<Integer> i = this._requestHeader.keySet().iterator(); i.hasNext();) {
				Entry<String, String> e = this._requestHeader.get(i.next());
				if (e.getKey().equals(wkname)) {
					re = e.getValue();
					i.remove();
					break;
				}
			}
		}
		return re;
	}
	/** 
	 * @param name httpヘッダ名
	 * @param value httpヘッダ値
	 * @return ???
	 */
	public void setHeader(String name,String value) {
		String wkname = normalizeHeaderName(name);
		SimpleEntry<String, String> e = new SimpleEntry<String, String>(wkname, value);
		this._requestHeader.put(this._requestHeader.size(), e);
	}

	/**
	 * リクエストヘッダを解釈し「メソッド」「URI」「HTTPVersion」を
	 * Stringの配列に格納して返す｡
	 * HTTPVersionに関しては省略された場合"HTTP/1.0"と解釈する
	 */
	public void parseRequestLine(byte[] buffer) {
		int length = buffer.length;
		this._requestURI = null;
		this._version = "HTTP/1.0";

		int i,j;
		/*
		 * メソッドの確定
		 * スペースが見つからない場合はmethodが変なものになり結果として
		 * 501 Method Not Implemented
		 * になるだろう
		 */
		for(i=0 ; i<length ; i++)
			if (buffer[i]==' ') break;
		this._method = new String(buffer,0,i);

		// 連続する空白文字を読み飛ばす
		i++;
		while(i<length && buffer[i]==' ') i++;  
		if (i == length) return;

		// リクエストURIの確定
		for(j=i ; j<length ; j++)
			if (buffer[j]==' ') break;
		this._requestURI = new byte[j-i];
		System.arraycopy(buffer,i,this._requestURI,0,j-i);

		// 連続する空白文字を読み飛ばす
		j++;
		while(j<length && buffer[j]==' ') j++;

		if (j != length) 
			this._version = new String(buffer,j,length-j);
	}

	/**
	 * 与えられたbyte[]の中で最初に現れる':'より前と
	 * ':'より後に最初に現れる空白文字以外の文字以降を返す｡
	 * 
	 * @return String[2]を返す。String[0]は':'より前の文字列であり、
	 * String[1]は':'より後に最初に現れる空白文字以外の文字以降の文字列である｡
	 * ':'が見つからない場合はString[0]==null,String[1]==引数を返す。
	 * LWSとは連続した空白のことである
	 * 先頭文字と"-"の後の文字を大文字に正規化してしまう
	 * @param buffer リクエストバイト配列
	 * @return リクエストヘッダストリング配列
	 */
	public static String[] parseRequestHeader(byte[] buffer) {
		String[] result = new String[2];
		int length = buffer.length;
		int position;

		// ':'の出現位置を探す
		for(position=0 ; position<length ; position++) {
			if (buffer[position]==':') break;
		}
		// ':'が見つからなかった場合
		if (position==length) {
			result[0] = null;
			result[1] = new String(buffer);
			return result;
		}
		result[0] = new String(buffer,0,position);

		// SPACEを読み飛ばす
		for(position++ ; position<length ; position++) {
			if (buffer[position] != ' ') break;
		}

		result[1] = new String(buffer,position,length-position);
		return result;
	}

	/**
	 * リクエストの"User-Agent"ヘッダを返す
	 * @return UserAgent
	 */
	@Override
	public String getUserAgentHeader() {
		return getHeaderValue(Consts.USER_AGENT_KEY_NAME);
	}
	private byte[] _rawBytes = null;	
	public void setRawBytes(byte[] rawBytes) {
		this._rawBytes = rawBytes;
	}
	public byte[] getRawBytes() {
		return this._rawBytes;
	}
}
