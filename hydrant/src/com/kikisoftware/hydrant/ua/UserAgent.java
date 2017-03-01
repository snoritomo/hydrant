package com.kikisoftware.hydrant.ua;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.kikisoftware.hydrant.Consts;
import com.kikisoftware.hydrant.server.RawRequest;

public class UserAgent {
	private boolean ie = false;
	private boolean chrome = false;
	private boolean firefox = false;
	private boolean safari = false;
	private boolean opera = false;
	private boolean android = false;
	private boolean iPhone = false;
	private boolean iPad = false;
	private boolean iPod = false;
	private boolean pc = false;
	private boolean tablet = false;
	private boolean smart = false;
	private String uaStringRaw = "";
	private String uaString = "";
	
	public static final Pattern pie = Pattern.compile("(^|[ )(;:/\"'\\[\\]}{|\\-])msie([ )(;:/\"'\\[\\]}{|\\-]|$)");
	public static final Pattern pchrm = Pattern.compile("(^|[ )(;:/\"'\\[\\]}{|\\-])chrome([ )(;:/\"'\\[\\]}{|\\-]|$)");
	public static final Pattern pfrfx = Pattern.compile("(^|[ )(;:/\"'\\[\\]}{|\\-])firefox([ )(;:/\"'\\[\\]}{|\\-]|$)");
	public static final Pattern psfr = Pattern.compile("(^|[ )(;:/\"'\\[\\]}{|\\-])safari([ )(;:/\"'\\[\\]}{|\\-]|$)");
	public static final Pattern popr = Pattern.compile("(^|[ )(;:/\"'\\[\\]}{|\\-])opera([ )(;:/\"'\\[\\]}{|\\-]|$)");

	public static final Pattern mbl = Pattern.compile("(^|[ )(;:/\"'\\[\\]}{|\\-])mobile([ )(;:/\"'\\[\\]}{|\\-]|$)");
	public static final Pattern adr = Pattern.compile("(^|[ )(;:/\"'\\[\\]}{|\\-])android([ )(;:/\"'\\[\\]}{|\\-]|$)");
	public static final Pattern pod = Pattern.compile("(^|[ )(;:/\"'\\[\\]}{|\\-])ipod([ )(;:/\"'\\[\\]}{|\\-]|$)");
	public static final Pattern phn = Pattern.compile("(^|[ )(;:/\"'\\[\\]}{|\\-])iphone([ )(;:/\"'\\[\\]}{|\\-]|$)");
	public static final Pattern pad = Pattern.compile("(^|[ )(;:/\"'\\[\\]}{|\\-])ipad([ )(;:/\"'\\[\\]}{|\\-]|$)");
	public static final Pattern pios = Pattern.compile("(^|[ )(;:/\"'\\[\\]}{|\\-])ios([ )(;:/\"'\\[\\]}{|\\-]|$)");
	
	public UserAgent(RawRequest req){
		this.uaStringRaw = req.getHeaderValue(Consts.USER_AGENT_KEY_NAME);
		this.uaString = this.uaStringRaw.toLowerCase();

		Matcher m = pie.matcher(this.uaString);
		if (m.find()){
			this.ie = true;
			this.pc = true;
			return;
		}
		m = pchrm.matcher(this.uaString);
		if (m.find()){
			this.chrome = true;
			this.pc = true;
			return;
		}
		m = pfrfx.matcher(this.uaString);
		if (m.find()){
			this.firefox = true;
			this.pc = true;
			return;
		}
		m = psfr.matcher(this.uaString);
		if (m.find()){
			this.safari = true;
			this.pc = true;
			return;
		}
		m = popr.matcher(this.uaString);
		if (m.find()){
			this.opera = true;
			this.pc = true;
			return;
		}
		m = adr.matcher(this.uaString);
		if (m.find()){
			this.android = true;
			m = mbl.matcher(this.uaString);
			if (m.find()){
				this.smart = true;
			}
			else{
				this.tablet = true;
			}
			return;
		}
		m = pod.matcher(this.uaString);
		if (m.find()){
			this.iPod = true;
			this.smart = true;
			return;
		}
		m = phn.matcher(this.uaString);
		if (m.find()){
			this.iPhone = true;
			this.smart = true;
			return;
		}
		m = pad.matcher(this.uaString);
		if (m.find()){
			this.iPad = true;
			this.tablet = true;
			return;
		}
	}
	public String getDeviceOSVersion(){
		String osv = "";
		if(ie)
			osv += "IE";
		else if(chrome)
			osv += "Chrome";
		else if(firefox)
			osv += "Firefox";
		else if(safari)
			osv += "Safari";
		else if(opera)
			osv += "Opera";
		else if(android)
			osv += "Android";
		else if(iPhone)
			osv += "iPhone";
		else if(iPod)
			osv += "iPod";
		else if(iPad)
			osv += "iPad";
		else
			osv += "UnKnown";
		if(pc)
			osv += "/PC";
		else if(smart)
			osv += "/SmartPhone";
		else if(tablet)
			osv += "/Tablet PC";
		return osv;
	}
}
