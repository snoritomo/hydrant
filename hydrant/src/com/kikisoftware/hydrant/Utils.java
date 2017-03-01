package com.kikisoftware.hydrant;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Category;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.kikisoftware.hydrant.rewrite.RewriteManager;
import com.kikisoftware.hydrant.server.ResponseBuilderFactory;

public class Utils {
	private static final Logger log_ = Logger.getLogger(Utils.class);
	private static java.util.ResourceBundle _RB;
	private static Map<String, String> setting;

	public static void reload() {
		java.util.ResourceBundle.clearCache();
		_RB = java.util.ResourceBundle.getBundle("hydrant");
		setting = new HashMap<String, String>();
		commandables = null;
		ResponseBuilderFactory.reload();
		RewriteManager.reload();
	}
	static {
		reload();
	}
	public static String getResourceString(String key, String pre, String aft){
		if(setting.containsKey(key))return setting.get(key);
		String wk = "";
		try {
			wk = _RB.getString(key);
			if(pre!=null)wk = wk.replace(pre, aft);
		} catch (Exception e) {
		}
		setting.put(key, wk);
		return wk;
	}
	public static String getResourceString(String key) {
		return getResourceString(key, null, null);
	}
	public static String getResourceString(String key, String def, boolean outMSG){
		String wk = getResourceString(key);
		if(wk==null || wk.equals("")){
			if(outMSG)log_.warn(key+" setting is illegal. use default value ["+def+"]");
			wk = def;
			setting.put(key, def);
		}
		return wk;
	}
	public static Level getLoggerLevel(Category logger) {
		Category wklogger = logger;
		while(wklogger!=null && wklogger.getLevel()==null){
			wklogger = wklogger.getParent();
		}
		return(wklogger==null ? Level.OFF : wklogger.getLevel());
	}

	public static String getRequestRule() {return getResourceString("requestRule");}
	public static Map<String, Integer> getHostPorts(String prop) {
		String[] wk = getResourceString(prop).split(",");
		Map<String, Integer> re = new HashMap<String, Integer>();
		try {
			for(int i = 0; i < wk.length; i++){
				if(wk[i].equals(""))continue;
				String[] hp = wk[i].split("\\/");
				int port = 80;
				if(hp.length>1 && !hp[1].equals("") && hp[1].matches("[0-9]*"))
					port = Integer.parseInt(hp[1]);
				re.put(i+"="+hp[0], port);
			}
		} catch (NumberFormatException e) {
			log_.error(prop + " could not parse. it has number exception\n"+getStackTrace(e));
			System.exit(-1);
		}
		return re;
	}

	public static Map<String, Integer> getAppHostPorts() {return getHostPorts("hostPort");}
	public static boolean getlocalDNSResolver() {return getResourceString("localDNSResolver").equals(Boolean.toString(true));}
	public static String getHttpDefaultPort() {return getResourceString("httpDefaultPort", "80", true);}
	public static String getHttpsDefaultPort() {return getResourceString("httpsDefaultPort", "443", true);}

	public static String getRawHttpHeaderEncode() {return getResourceString("rawHttpHeaderEncode", "", false);}
	public static boolean getRemoveGzipAcceptEncode() {return getResourceString("removeGzipAcceptEncode", "false", false).equals(Boolean.toString(true));}

	public static int getDownloadSpeed() {return Integer.parseInt(getResourceString("downloadSpeed", "0", true));}

	public static int getRequestTimeout(){return Integer.parseInt(getResourceString("requestTimeout", "30000", true));}
	public static int getTimeoutCheckInterval(){return Integer.parseInt(getResourceString("timeoutCheckInterval", "200", true));}
	public static int getWriteRetryInterval(){return Integer.parseInt(getResourceString("writeRetryInterval", "5", true));}
	public static int getThroughIOBufferSize(){return Integer.parseInt(getResourceString("throughIOBufferSize", "1024", true));}
	public static int getContentsSocketTimeout() {return Integer.parseInt(getResourceString("contentsSocketTimeout", "500", true));}
	public static String getContentsSocketRetryLimit(){return getResourceString("contentsSocketRetryLimit", "10", true);}

	public static int getKeepAliveTimeout() {return Integer.parseInt(getResourceString("keepAliveTimeout", "5000", true));}
	public static int getKeepAliveMaxCount(){return Integer.parseInt(getResourceString("keepAliveMaxCount", "100", true));}

	public static int getThreadPoolSize(){return Integer.parseInt(getResourceString("threadPoolSize", "100", true));}

	public static int getHttpThreadPriority(){return Integer.parseInt(getResourceString("httpThreadPriority", Integer.toString(Thread.NORM_PRIORITY), true));}
	public static int getHttpsThreadPriority(){return Integer.parseInt(getResourceString("httpsThreadPriority", Integer.toString(Thread.NORM_PRIORITY), true));}
	public static int getRequestThreadPriority(){return Integer.parseInt(getResourceString("requestThreadPriority", Integer.toString(Thread.NORM_PRIORITY), true));}
	public static int getAccessLogThreadPriority(){return Integer.parseInt(getResourceString("accessLogThreadPriority", Integer.toString(Thread.NORM_PRIORITY), true));}
	public static int getStatsLogThreadPriority(){return Integer.parseInt(getResourceString("statsLogThreadPriority", Integer.toString(Thread.NORM_PRIORITY), true));}

	public static String getResonseBuilder() {return getResourceString("resonseBuilder");}

	public static int getStatsInterval() {return Integer.parseInt(getResourceString("statsInterval", "3600000", true));}
	
	private static Map<String, String> errorRes;
	private static String getErrorResString(String key, String def){
		String wk = getErrorResString(key);
		if(wk==null || wk.equals("")){
			log_.warn("internalErrorResponseFile: " + key+" setting is illegal. use default value ["+def+"]");
			wk = def;
			errorRes.put(key, def);
		}
		return wk;
	}
	private static void prepareErrorRes(String file){
		errorRes = new HashMap<String, String>();
		FileInputStream fi = null;
		try {
			fi = new FileInputStream(file);
			BinaryStreamReader br = new BinaryStreamReader(fi);
			int hdi = 0;
			while(true){
				String l = getString(br.readLine());
				if(l.equals(""))break;
				String[] dats = l.split(":", 2);
				if(dats[0].trim().toLowerCase().equals("hd"))
					errorRes.put(Integer.toString(hdi++), dats[1].trim());
				else
					errorRes.put(dats[0].trim(), dats[1].trim());
			}
			ArrayList<Byte> bl = br.readAll();
			errorRes.put("body", getString(bl, getErrorResCharset()));
		} catch (FileNotFoundException e) {
			log_.error("error response file could not load.\n"+getStackTrace(e));
			System.exit(-1);
		} catch (IOException e) {
			log_.error("i/o error occured while reading error response file.\n"+getStackTrace(e));
			System.exit(-1);
		} catch (Exception e) {
			log_.error("error response file is illegal.\n"+getStackTrace(e));
			System.exit(-1);
		} finally {
			if(fi!=null){
				try {
					fi.close();
				} catch (IOException e) {
				}
			}
		}
	}
	private static String getErrorResString(String key){
		if(errorRes==null){
			prepareErrorRes(getResourceString("internalErrorResponseFile"));
		}
		return errorRes.get(key);
	}
	public static String getErrorResHTML(String tag){
		String html = "";
		synchronized(errorRes){
			for(Entry<String, String> e : errorRes.entrySet()){
				html += "<"+tag+">" + e.getKey() + " -> " + e.getValue() + "</" + tag + ">";
			}
		}
		return html;
	}
	public static String getErrorResResponseCode() {return getErrorResString("responseCode", "404 Not Found");}
	public static String getErrorResContentType() {return getErrorResString("contentType", "text/html");}
	public static String getErrorResCharset() {return getErrorResString("charset", "UTF-8");}
	public static String getErrorResServerName() {return getErrorResString("serverName", "xelion");}
	public static String getErrorResContent() {return getErrorResString("body");}
	public static String getErrorResHeaders() {
		String re = "";
		for(int i = 0; true; i++){
			String key = Integer.toString(i);
			if(!errorRes.containsKey(key))break;
			re += errorRes.get(key)+"\r\n";
		}
		return re;
	}

	public static String getAccessLogFormat() {return getResourceString("accessLogFormat");}
	public static String getStatsLogFormat() {return getResourceString("statsLogFormat");}

	private static List<String> commandables = null;
	public static boolean isCommandableIP(InetAddress adr){
		String ip = adr.getHostAddress();
		if(commandables==null){
			commandables = new ArrayList<String>();
			String[] wk = getCommandableIP().split(",");
			for(String s : wk){
				try {
					InetAddress wkip = InetAddress.getByName(s);
					commandables.add(wkip.getHostAddress());
				} catch (UnknownHostException e) {
					log_.warn("Illegal commandableIP is setted. ["+s+"]");
				}
			}
		}
		return commandables.contains(ip);
	}
	public static String getCommandableIP() {return getResourceString("commandableIP");}
	public static String getResetCommand() {return getResourceString("resetCommand");}
	public static String getStatsCommand() {return getResourceString("getStatsCommand");}
	public static String getSettingsCommand() {return getResourceString("getSettingsCommand");}

	public static Map<String, Integer> getSslHostPorts() {return getHostPorts("sslHostPort");}
	public static String getSslKey() {return getResourceString("sslKey", File.separator.equals("/")?"\\":"/", File.separator);}
	public static String getSslTrust() {return getResourceString("sslTrust", File.separator.equals("/")?"\\":"/", File.separator);}
	public static String getSslKeyStoreType() {return getResourceString("sslKeyStoreType");}
	public static String getSslTrustStoreType() {return getResourceString("sslTrustStoreType");}
	public static String getSslKeyPass() {return getResourceString("sslKeyPass");}
	public static String getSslTrustPass() {return getResourceString("sslTrustPass");}
	public static String getSslKeyAlgorith() {return getResourceString("sslKeyAlgorithm");}
	public static String getSslTrustAlgorith() {return getResourceString("sslTrustAlgorithm");}
	public static String getSslType() {return getResourceString("sslType");}
	public static int getSslThreadCount(){return Integer.parseInt(getResourceString("threadPoolSizeSSL", "3", true));}

	public static String getStackTrace(Throwable t) {
		StringWriter writer = new StringWriter();
		t.printStackTrace(new PrintWriter(writer));
		return writer.toString();
	}
	public static String getStackTrace(Exception e) {
		StringWriter writer = new StringWriter();
		e.printStackTrace(new PrintWriter(writer));
		return writer.toString();
	}
	/**
	 * リクエストの処理に使用する文字コードを考慮してバイト配列に変換する
	 * @param str
	 * @return
	 */
	public static byte[] getRequestBytes(String str){
		String enc = getRawHttpHeaderEncode();
		if(enc.equals("")){
			return str.getBytes();
		}
		else{
			byte[] re;
			try {
				re = str.getBytes(enc);
			} catch (UnsupportedEncodingException e) {
				re = str.getBytes();
			}
			return re;
		}
	}
	/**
	 * リクエストの処理に使用する文字コードを考慮して String に変換する
	 * @param str
	 * @return
	 */
	public static String getRequestString(byte[] src){
		String enc = getRawHttpHeaderEncode();
		if(enc.equals("")){
			return new String(src);
		}
		else{
			String re;
			try {
				re = new String(src, enc);
			} catch (UnsupportedEncodingException e) {
				re = new String(src);
			}
			return re;
		}
	}
	public static String getRequestHeaderValue(String src){
		int vstart = src.indexOf(Consts.HEADER_SEPARATOR_NO_SPACE) + 1;
		return src.substring(vstart).trim();
	}
	public static String getString(ArrayList<Byte> wk){
		byte[] wwk = new byte[wk.size()];
		int k = 0;
		for(Byte b : wk)
			wwk[k++] = b;
		return new String(wwk);
	}
	public static String getString(ArrayList<Byte> wk, String charset){
		byte[] wwk = new byte[wk.size()];
		int k = 0;
		for(Byte b : wk)
			wwk[k++] = b;
		try {
			return new String(wwk, charset);
		} catch (UnsupportedEncodingException e) {
			Logger.getLogger(Utils.class).error(getStackTrace(e));
			return new String(wwk);
		}
	}
	/** HTMLエンコードが必要な文字 **/
	static char[] htmlEncChar = {'&', '"', '<', '>', ' ', '\"'};
	/** HTMLエンコードした文字列 **/
	static String[] htmlEncStr = {"&amp;", "&quot;", "&lt;", "&gt;", "&nbsp;", "&quot;"};
	public static String getHtmlEncode(String strIn){
		if (strIn == null) {
			return null;
		}
		// HTMLエンコード処理
		StringBuffer strOut = new StringBuffer(strIn);
		// エンコードが必要な文字を順番に処理
		for (int i = 0; i < htmlEncChar.length; i++) {
			// エンコードが必要な文字の検索
			int idx = strOut.toString().indexOf(htmlEncChar[i]);

			while (idx != -1) {
				// エンコードが必要な文字の置換
				strOut.setCharAt(idx, htmlEncStr[i].charAt(0));
				strOut.insert(idx + 1, htmlEncStr[i].substring(1));

				// 次のエンコードが必要な文字の検索
				idx = idx + htmlEncStr[i].length();
				idx = strOut.toString().indexOf(htmlEncChar[i], idx);
			}
		}
		return strOut.toString();
	}
	public static String getErrorResponse(String url){
		String body;
		body = getErrorResContent().replace("{url}", getHtmlEncode(url));
		String clen;
		try {
			clen = Integer.toString(body.getBytes(getErrorResCharset()).length);
		} catch (UnsupportedEncodingException e) {
			clen = Integer.toString(body.getBytes().length);
		}
		return "HTTP/1.1 "+getErrorResResponseCode()+"\r\nDate: "+(DateFormat.getDateTimeInstance().format(new Date()))+"\r\nServer: "+getErrorResServerName()+"\r\n"+getErrorResHeaders()+"Content-Length: "+clen+"\r\nContent-Type: "+getErrorResContentType()+"; charset="+getErrorResCharset()+"\r\n\r\n"+body;
	}
}
