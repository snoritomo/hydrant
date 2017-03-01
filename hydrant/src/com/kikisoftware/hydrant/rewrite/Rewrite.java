package com.kikisoftware.hydrant.rewrite;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.kikisoftware.hydrant.Consts;
import com.kikisoftware.hydrant.Utils;
import com.kikisoftware.hydrant.server.RewroteRequest;

public class Rewrite {
	protected static final Logger log_ = Logger.getLogger(Rewrite.class);
	private Pattern scheme ;
	private Pattern host;
	private Pattern port;
	private Pattern path;
	private Pattern file;
	private Pattern ext;
	private Pattern param;
	private Map<String, Pattern> headers;
	private Pattern body;
	private Pattern uastring;

	private String scheme_to;
	private String host_to;
	private String port_to;
	private String path_to;
	private String file_to;
	private String ext_to;
	private String param_to;
	private Map<String, String> headers_to;
	private String body_to;
	private boolean gateway;

	public void applyToRequest(RewroteRequest req){
		String lurl = req.getRequestURI().getFile();
		if(gateway){
			URL rew = req.getRequestURI();
			try {
				rew = new URL(lurl);
			} catch (MalformedURLException e) {
				log_.error(Rewrite.class.getSimpleName() + ":request url is invalid." +
						Utils.getStackTrace(e));
			}
			req.setRequestURI(rew);
			return;
		}
		URL r = req.getRequestURI();
		String lschm = r.getProtocol() + Consts.SCHEME_HOST_SEPARATOR;
		String lhost = r.getHost();
		String lport = String.valueOf(r.getPort());
		String lpath = r.getPath();
		String[] ldirs = lpath.split(Consts.URL_PATH_SEPARATOR);
		boolean lastisfile = ldirs.length<=0?false:(ldirs[ldirs.length-1].contains(Consts.URL_EXTENSION_SEPARATOR));
		String ldir = !lastisfile?lpath:lpath.substring(0, lpath.lastIndexOf(Consts.URL_PATH_SEPARATOR)+1);
		String lfile = !lastisfile?"":lpath.substring(lpath.lastIndexOf(Consts.URL_PATH_SEPARATOR)+1);
		String q = r.getQuery();
		String ref = r.getRef();
		String lparams = ((q==null || q.isEmpty())?"":q)+((ref==null || ref.isEmpty())?"":Consts.URL_REF_SEPARATOR+r.getRef());
		if(scheme_to!=null){
			req.setSSL(scheme_to.equals("ssl"));
			lschm = Consts.HTTPS_SCHEME;
		}
		if(host_to!=null){
			lhost = host_to;
			req.setHeader("Host", host_to);
		}
		if(port_to!=null){
			lport = port_to;
		}
		if(file_to!=null){
			lfile = file_to;
		}
		if(ext_to!=null){
			lfile = lfile.substring(0, lfile.lastIndexOf(Consts.URL_EXTENSION_SEPARATOR)) + "." + ext_to;
		}
		if(path_to!=null){
			ldir = (path==null?path_to:(ldir.equals("")?Consts.URL_PATH_SEPARATOR:ldir.replaceFirst(path.pattern(), path_to)));
		}
		if(param_to!=null){
			lparams = (param==null?param_to:(lparams.equals("")?"":lparams.replaceFirst(param.pattern(), param_to)));
		}
		for(Iterator<Entry<String, String>> i = headers_to.entrySet().iterator();i.hasNext();){
			Entry<String, String> e = i.next();
			req.setHeader(e.getKey(), e.getValue());
		}
		try {
			req.setRequestURI(new URL(lschm+lhost+Consts.URL_HOSTPORT_SEPARATOR+lport+ldir+lfile+((lparams==null || lparams.isEmpty())?"":Consts.URL_QUERY_SEPARATOR+lparams)));
		} catch (MalformedURLException e) {
			log_.fatal(Rewrite.class.getSimpleName() + ":Rewrite setting is invalid." +
					Utils.getStackTrace(e) + "\n" +
					"shutdown");
			System.exit(-1);
		}
	}
	public Rewrite(){
		scheme = null;
		host = null;
		port = null;
		path = null;
		ext = null;
		param = null;
		headers = new HashMap<String, Pattern>();
		body = null;
		uastring = null;

		scheme_to = null;
		host_to = null;
		port_to = null;
		path_to = null;
		ext_to = null;
		param_to = null;
		headers_to = new HashMap<String, String>();
		body_to = null;
		gateway = false;
	}
	/**
	 * @return the scheme
	 */
	public Pattern getScheme() {
		return scheme;
	}
	/**
	 * @param scheme the scheme to set
	 */
	public void setScheme(Pattern scheme) {
		this.scheme = scheme;
	}
	/**
	 * @return the host
	 */
	public Pattern getHost() {
		return host;
	}
	/**
	 * @param host the host to set
	 */
	public void setHost(Pattern host) {
		this.host = host;
	}
	/**
	 * @return the port
	 */
	public Pattern getPort() {
		return port;
	}
	/**
	 * @param port the port to set
	 */
	public void setPort(Pattern port) {
		this.port = port;
	}
	/**
	 * @return the path
	 */
	public Pattern getPath() {
		return path;
	}
	/**
	 * @param the path to set
	 */
	public void setPath(Pattern path) {
		this.path = path;
	}
	/**
	 * @param the file
	 */
	public Pattern getFile() {
		return file;
	}
	/**
	 * @param the file to set
	 */
	public void setFile(Pattern file) {
		this.file = file;
	}
	/**
	 * @return the ext
	 */
	public Pattern getExt() {
		return ext;
	}
	/**
	 * @param ext the ext to set
	 */
	public void setExt(Pattern ext) {
		this.ext = ext;
	}
	/**
	 * @return the param
	 */
	public Pattern getParam() {
		return param;
	}
	/**
	 * @param param the param to set
	 */
	public void setParam(Pattern param) {
		this.param = param;
	}
	/**
	 * @return the headers
	 */
	public Map<String, Pattern> getHeaders() {
		return headers;
	}
	/**
	 * @param headers the headers to set
	 */
	public void setHeaders(Map<String, Pattern> headers) {
		this.headers = headers;
	}
	/**
	 * @return the body
	 */
	public Pattern getBody() {
		return body;
	}
	/**
	 * @param body the body to set
	 */
	public void setBody(Pattern body) {
		this.body = body;
	}
	/**
	 * @return
	 */
	public Pattern getUastring() {
		return uastring;
	}
	/**
	 * @param uastring
	 */
	public void setUastring(Pattern uastring) {
		this.uastring = uastring;
	}
	/**
	 * @param gateway the gateway to set
	 */
	public void setGateway(boolean gateway) {
		this.gateway = gateway;
	}
	/**
	 * @return the scheme_to
	 */
	public String getScheme_to() {
		return scheme_to;
	}
	/**
	 * @param schemeTo the scheme_to to set
	 */
	public void setScheme_to(String schemeTo) {
		scheme_to = schemeTo;
	}
	/**
	 * @return the host_to
	 */
	public String getHost_to() {
		return host_to;
	}
	/**
	 * @param hostTo the host_to to set
	 */
	public void setHost_to(String hostTo) {
		host_to = hostTo;
	}
	/**
	 * @return the port_to
	 */
	public String getPort_to() {
		return port_to;
	}
	/**
	 * @param portTo the port_to to set
	 */
	public void setPort_to(String portTo) {
		port_to = portTo;
	}
	/**
	 * @return the path_to
	 */
	public String getPath_to() {
		return path_to;
	}
	/**
	 * @param pathTo the path_to to set
	 */
	public void setPath_to(String pathTo) {
		path_to = pathTo;
	}
	/**
	 * @return the file_to
	 */
	public String getFile_to() {
		return file_to;
	}
	/**
	 * @param fileTo the file_to to set
	 */
	public void setFile_to(String file_to) {
		this.file_to = file_to;
	}
	/**
	 * @return the ext_to
	 */
	public String getExt_to() {
		return ext_to;
	}
	/**
	 * @param extTo the ext_to to set
	 */
	public void setExt_to(String extTo) {
		ext_to = extTo;
	}
	/**
	 * @return the param_to
	 */
	public String getParam_to() {
		return param_to;
	}
	/**
	 * @param paramTo the param_to to set
	 */
	public void setParam_to(String paramTo) {
		param_to = paramTo;
	}
	/**
	 * @return the headers_to
	 */
	public Map<String, String> getHeaders_to() {
		return headers_to;
	}
	/**
	 * @param headersTo the headers_to to set
	 */
	public void setHeaders_to(Map<String, String> headersTo) {
		headers_to = headersTo;
	}
	/**
	 * @return the body_to
	 */
	public String getBody_to() {
		return body_to;
	}
	/**
	 * @param bodyTo the body_to to set
	 */
	public void setBody_to(String bodyTo) {
		body_to = bodyTo;
	}

}
