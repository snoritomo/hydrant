package com.kikisoftware.hydrant.server;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.AbstractMap.SimpleEntry;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.kikisoftware.hydrant.Consts;
import com.kikisoftware.hydrant.Utils;
import com.kikisoftware.hydrant.rewrite.RewriteManager;

public class RewroteRequest extends Request {
	private static final Logger log_ = Logger.getLogger(RewriteManager.class);
	protected String _method; // 大文字に正規化(GET/POST)
	protected URL _requestURL; //日本語が含まれる可能性があるためbyte配列
	protected String _version;
	// 同じHeaderは送られてこないものとする(よってmapでよい)
	protected LinkedHashMap<Integer, Entry<String, String>> _requestHeader; // (key,value)=(String,String)

	protected RewroteRequest() {}

	public RewroteRequest(RawRequest src){
		_method = src.getMethod();
		String realhostport = src.getHeaderValue(Consts.REAL_HOST_NAME);
		String[] hp = realhostport.split(Consts.URL_HOSTPORT_SEPARATOR);
		String host = realhostport.equals("") ? src.getIPAddress() : hp[0];
		int port = (realhostport.equals("") || hp.length <= 1) ? src.getIPPort() : Integer.parseInt(hp[1]);
		try {
			_requestURL = new URL(src.isSSL()?Consts.HTTPS_SCHEME_NO_SEPARATOR:Consts.HTTP_SCHEME_NO_SEPARATOR, host, port, src.getRequestURLString());
		} catch (MalformedURLException e) {
			log_.error("Invalid Request:"+src.getIPAddress()+":"+src.getIPPort()+"/"+src.getRequestURLString()+"\n"+Utils.getStackTrace(e));
		}
		_version = src.getVersion();
		_requestHeader = new LinkedHashMap<Integer, Entry<String, String>>(src.getRequestHeader());
	}

	public String getMethod() {return _method;}
	public URL getRequestURI() {return _requestURL;}
	public String getVersion() {return _version;}
	public Map<Integer, Entry<String, String>> getRequestHeader() {return _requestHeader;}

	public void setMethod(String method) {_method = method.toUpperCase();}
	public void setRequestURI(URL requestURL) {_requestURL = requestURL;}
	public void setVersion(String version) {_version = version;}

	/* ヘッダ操作系 */
	@Override
	public String getHeaderValue(String name) {
		String wkname = normalizeHeaderName(name);
		String re = "";
		synchronized (this._requestHeader) {
			for (Iterator<Integer> i = this._requestHeader.keySet().iterator(); i.hasNext();) {
				Entry<String, String> e = this._requestHeader.get(i.next());
				if (e.getKey().equals(wkname)) {
					re = e.getValue();
					break;
				}
			}
		}
		return re;
	}
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
	public void setHeader(String name,String value) {
		String wkname = normalizeHeaderName(name);
		synchronized (this._requestHeader) {
			for (Iterator<Integer> i = this._requestHeader.keySet().iterator(); i.hasNext();) {
				int ky = i.next();
				Entry<String, String> e = this._requestHeader.get(ky);
				if (e.getKey().equals(wkname)) {
					SimpleEntry<String, String> vl = new SimpleEntry<String, String>(wkname, value);
					this._requestHeader.put(ky, vl);
					break;
				}
			}
		}
	}

	/**
	 * リクエストの"User-Agent"ヘッダを返す
	 */
	@Override
	public String getUserAgentHeader() {
		return getHeaderValue("User-Agent");
	}

//	/** ヘッダをMapごと設定 */
//	public void setRequestHeader(Map<Integer, Entry<String, String>> RequestHeader) {
//		_requestHeader = new LinkedHashMap<Integer, Entry<String, String>>(RequestHeader);
//
//		//ID:2006-04-26-01
//		_requestHeader.remove("If-Modified-Since");
//	}

	public byte[] getHeaderBytes() {
		StringBuffer result = new StringBuffer();

		result.append(_method);
		result.append(" ");
		result.append(_requestURL.getFile());
		result.append(" ");
		result.append(_version);
		result.append(Consts.CRLF);
		boolean removegzip = Utils.getRemoveGzipAcceptEncode();
		for(Iterator<Integer> I=_requestHeader.keySet().iterator() ; I.hasNext() ; ) {
			Map.Entry<String, String> entry = _requestHeader.get(I.next());
			String ky = entry.getKey();
			result.append(ky);
			result.append(Consts.HEADER_SEPARATOR);
			if(removegzip && ky.equals(Consts.REAL_ACCEPT_ENCODING_NAME)){
				String wk = entry.getValue().replaceAll(Consts.REAL_GZIP, "").replaceAll("^[, ]*", "").replaceAll("[, ]*$", "");
				result.append(wk);
			}
			else
				result.append(entry.getValue());
			result.append(Consts.CRLF);
		}
		result.append(Consts.CRLF);

		return Utils.getRequestBytes(result.toString());
	}
}
