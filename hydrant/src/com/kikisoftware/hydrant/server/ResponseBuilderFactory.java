package com.kikisoftware.hydrant.server;

import java.lang.reflect.InvocationTargetException;

import org.apache.log4j.Logger;

import com.kikisoftware.hydrant.Utils;
import com.kikisoftware.hydrant.log.Access;
import com.kikisoftware.hydrant.rewrite.RewriteManager;
import com.kikisoftware.hydrant.ua.UserAgent;

public class ResponseBuilderFactory {
	private static final Logger log_ = Logger.getLogger(ResponseBuilderFactory.class);
	private static final Class<?>[] CONSTRUCTOR_ARGS = new Class[] {RequestProcessor.class, RawRequest.class, UserAgent.class, Access.class};
	private static Class<ResponseBuilder> targetClass;
	@SuppressWarnings("unchecked")
	public static void reload(){
		try {
			Class<?> wk = Class.forName(Utils.getResonseBuilder());
			targetClass =  (Class<ResponseBuilder>)wk;
		} catch (ClassNotFoundException e) {
			log_.error(Utils.getStackTrace(e));
			System.exit(-1);
		}
	}
	public static ResponseBuilder getResponseBuilder(RequestProcessor proc, RawRequest req, UserAgent ua, Access log){
		ResponseBuilder re = null;
		try {
			re = targetClass.getConstructor(CONSTRUCTOR_ARGS).newInstance(new Object[]{proc, req, ua, log});
		} catch (InstantiationException e) {
			log_.error(Utils.getStackTrace(e));
			System.exit(-1);
		} catch (IllegalAccessException e) {
			log_.error(Utils.getStackTrace(e));
			System.exit(-1);
		} catch (IllegalArgumentException e) {
			log_.error(Utils.getStackTrace(e));
			System.exit(-1);
		} catch (InvocationTargetException e) {
			log_.error(Utils.getStackTrace(e));
			System.exit(-1);
		} catch (NoSuchMethodException e) {
			log_.error(Utils.getStackTrace(e));
			System.exit(-1);
		} catch (SecurityException e) {
			log_.error(Utils.getStackTrace(e));
			System.exit(-1);
		}
		RewroteRequest sender = new RewroteRequest(req);
		re.setArrangedRequest(sender);
		re.setRule(RewriteManager.detect(sender, ua));
		return re;
	}
}
