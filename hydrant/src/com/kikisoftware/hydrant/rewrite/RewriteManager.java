package com.kikisoftware.hydrant.rewrite;

import java.util.LinkedList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.kikisoftware.hydrant.Consts;
import com.kikisoftware.hydrant.Utils;
import com.kikisoftware.hydrant.server.RewroteRequest;
import com.kikisoftware.hydrant.ua.UserAgent;

public class RewriteManager {
	private static final Logger log_ = Logger.getLogger(RewriteManager.class);
	private static LinkedList<Rewrite> rules = new LinkedList<Rewrite>();
	public static void reload(){
		String val = "";
		val = Utils.getRequestRule();
		if(val==null || val.equals("")){
			log_.fatal("hydrant.properties#requestRule must be set.");
			System.exit(-1);
		}
		String[] wk = val.split(";;");
		for(String w : wk){
			if(w.equals(""))continue;
			String[] ft = w.split("::");
			String[] from = ft[0].split("\\|");
			Rewrite r = new Rewrite();
			for(String f : from){
				String[] kv = f.split("=");
				if(kv.length<2)continue;
				Pattern p = Pattern.compile(kv[1]);
				if(kv[0].equals(Consts.REQUEST_KEY_SCHEME)){
					r.setScheme(p);
				}
				else if(kv[0].equals(Consts.REQUEST_KEY_HOST)){
					r.setHost(p);
				}
				else if(kv[0].equals(Consts.REQUEST_KEY_PORT)){
					r.setPort(p);
				}
				else if(kv[0].equals(Consts.REQUEST_KEY_PATH)){
					r.setPath(p);
				}
				else if(kv[0].equals(Consts.REQUEST_KEY_FILE)){
					r.setFile(p);
				}
				else if(kv[0].equals(Consts.REQUEST_KEY_EXT)){
					r.setExt(p);
				}
				else if(kv[0].equals(Consts.REQUEST_KEY_PARAM)){
					r.setParam(p);
				}
				else if(kv[0].startsWith(Consts.REQUEST_KEY_HEAD)){
					String key = "";
					key = kv[0].replaceFirst("^"+Consts.REQUEST_KEY_HEAD, "");
					r.getHeaders().put(key, p);
				}
				else if(kv[0].equals(Consts.REQUEST_KEY_BODY)){
					r.setBody(p);
				}
				else if(kv[0].equals(Consts.REQUEST_KEY_UASTR)){
					r.setUastring(p);
				}
				else continue;
			}
			String[] to = ft[1].split("\\|");
			for(String t : to){
				String[] kv = t.split("=");
				String p = kv[1];
				if(kv[0].equals(Consts.REQUEST_KEY_MODE)){
					r.setGateway(p.toLowerCase().equals("gateway"));
				}
				else if(kv[0].equals(Consts.REQUEST_KEY_SCHEME)){
					r.setScheme_to(p);
				}
				else if(kv[0].equals(Consts.REQUEST_KEY_HOST)){
					r.setHost_to(p);
				}
				else if(kv[0].equals(Consts.REQUEST_KEY_PORT)){
					r.setPort_to(p);
				}
				else if(kv[0].equals(Consts.REQUEST_KEY_PATH)){
					r.setPath_to(p);
				}
				else if(kv[0].equals(Consts.REQUEST_KEY_FILE)){
					r.setFile_to(p);
				}
				else if(kv[0].equals(Consts.REQUEST_KEY_EXT)){
					r.setExt_to(p);
				}
				else if(kv[0].equals(Consts.REQUEST_KEY_PARAM)){
					r.setParam_to(t.substring(kv[0].length()+1));
				}
				else if(kv[0].startsWith(Consts.REQUEST_KEY_HEAD)){
					String key = "";
					key = kv[0].replaceFirst("^"+Consts.REQUEST_KEY_HEAD, "");
					r.getHeaders_to().put(key, t.substring(kv[0].length()+1));
				}
				else if(kv[0].equals(Consts.REQUEST_KEY_BODY)){
					r.setBody_to(p);
				}
				else continue;
			}
			rules.add(r);
		}
	}
	public static Rewrite detect(RewroteRequest req, UserAgent ua){
		RULE_CONTINUE:
		for(Rewrite r : rules){
			if(r.getScheme()!=null && !r.getScheme().matcher(req.isSSL()?"ssl":"http").find()){
				continue;
			}
			if(r.getHost()!=null && !r.getHost().matcher(req.getRequestURI().getHost()).find()){
				continue;
			}
			if(r.getPort()!=null && !r.getPort().matcher(String.valueOf(req.getRequestURI().getPort())).find()){
				continue;
			}
			String lurl = req.getRequestURI().getFile();
			String lpath = req.getRequestURI().getPath();
			String ldir = lpath.substring(0, lpath.lastIndexOf(Consts.URL_PATH_SEPARATOR)+1);
			String lfile = lpath.substring(lpath.lastIndexOf(Consts.URL_PATH_SEPARATOR)+1);
			String lparams = lurl.substring(lpath.lastIndexOf(Consts.URL_QUERY_SEPARATOR)+1);
			if(r.getFile()!=null && !r.getFile().matcher(lfile).find()){
				continue;
			}
			if(r.getExt()!=null && !r.getExt().matcher(lfile.substring(lfile.lastIndexOf(Consts.URL_EXTENSION_SEPARATOR)+1)).find()){
				continue;
			}
			if(r.getPath()!=null && !r.getPath().matcher(ldir).find()){
				continue;
			}
			if(r.getUastring()!=null && !r.getUastring().matcher(req.getUserAgentHeader()).find()){
				continue;
			}
			if(r.getParam()!=null){
				if(!r.getParam().matcher(lparams).find()){
					continue;
				}
			}
			for(Iterator<Entry<String, Pattern>> i = r.getHeaders().entrySet().iterator();i.hasNext();){
				Entry<String, Pattern> e = i.next();
				String val = req.getHeaderValue(e.getKey());
				if(!e.getValue().matcher(val).find())continue RULE_CONTINUE;
			}
			return r;
		}
		return null;
	}
}
