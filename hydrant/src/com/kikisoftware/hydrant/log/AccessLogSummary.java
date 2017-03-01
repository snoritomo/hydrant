package com.kikisoftware.hydrant.log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.lf5.util.DateFormatManager;

import com.kikisoftware.hydrant.Utils;

public class AccessLogSummary {
	private static String header_ = "from\ttotal access\tavg access\ttotal response time\tavg response time\ttotal receipt time\tavg receipt time\ttotal web-request time\tavg web-request time\ttotal download time\tavg download time\ttotal request length\tavg request length\ttotal request header length\tavg request header length\ttotal request body length\tavg request body length\ttotal response length\tavg response length\ttotal response header length\tavg response header length\ttotal response body length\ttotal response body length\ttotal retry times\tavg retry times\ttotal error times\tavgerror times\ttotal stack count\tavg stack count\t";
	//0:int 1:date 2:time 3:string
	static int[] types = new int[]{
		1,
		3,
		0,
		0,
		0,
		0,
		0,
		0,
		3,
		0,
		3,
		2,
		3,
		3,
		0
	};
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		double persecs = Double.parseDouble(args.length<=0||args[0]==null||args[0]==""?"300":args[0]);
		double permili = persecs * 1000;
		String encode = args.length<=1||args[1]==null||args[1]==""?"UTF-8":args[1];
		String output = args.length<=2||args[2]==null||args[2]==""?"accessSum.tsv":args[2];
		String target = "";
		if(args.length<=3||args[3]==null||args[3]==""){
			Enumeration<?> e = Logger.getLogger(Access.class).getAllAppenders();
			while (e.hasMoreElements()) {
				Appender appeder = (Appender) e.nextElement();
				boolean isFile = (appeder instanceof FileAppender);
				if (isFile == true) {
					FileAppender fileAppender = (FileAppender) appeder;
					if(fileAppender.getName().equals("access")){
						target = fileAppender.getFile();
						break;
					}
				}
			}
		}//行数があわねぇ
		else
			target = args[3];
		
		String format = Utils.getAccessLogFormat();
		File file = new File(target);
		BufferedReader br = null;
		OutputStreamWriter out = null;
		try {
			FileInputStream fs = new FileInputStream(file);
			InputStreamReader isr = new InputStreamReader(fs, encode);
			br = new BufferedReader(isr);
			Pattern p = Pattern.compile("\\{[0-9]+\\}");
			ArrayList<Integer> nums = new ArrayList<Integer>();
			String[] seps = format.split("\\{[0-9]+\\}");
			Matcher m = p.matcher(format);
			while(m.find()){
				String gnum = m.group();
				nums.add(Integer.parseInt(gnum.substring(1, gnum.length()-1)));
			}
			ArrayList<Map<Integer, String>> res = new ArrayList<Map<Integer, String>>();
			ArrayList<String> types = new ArrayList<String>();
			while(true){
				String l = br.readLine();
				if(l==null)break;
				StringBuffer dat = new StringBuffer();
				Map<Integer, String> line = new LinkedHashMap<Integer, String>();
				int i = 0;
				byte[] bb = l.getBytes(encode);
				for(byte b : bb){
					String d = dat.toString();
					if(d.endsWith(seps[i])){
						if(i>0){
							int idx = nums.get(i-1);
							int len = d.length()-seps[i].length();
							line.put(idx, (len<=0?"":d.substring(0, len)));
							if(idx==8){
								String ctype = line.get(idx);
								if(!types.contains(ctype))types.add(ctype);
							}
						}
						i++;
						dat = new StringBuffer();
					}
					dat.append((char)b);
				}
				res.add(line);
			}
			br.close();
			long sum0 = 0L;
			long sum3 = 0L;
			long sum4 = 0L;
			long sum5 = 0L;
			long sum6 = 0L;
			long sum7 = 0L;
			long sum9 = 0L;
			long sum10 = 0L;
			long sum14 = 0L;
			long sum15 = 0L;
			long sum16 = 0L;
			long sum17 = 0L;
			long sum18 = 0L;
			Map<String, Long> mimes = new LinkedHashMap<String, Long>();
			long count = 0L;
			long start = 0L;
			DateFormatManager dm = new DateFormatManager();
			FileOutputStream fo = new FileOutputStream(output);
			out = new OutputStreamWriter(fo, encode);
			String mimeh = "";
			for(String t : types){
				mimeh += "total " + t + " count\tavg " + t + " count\t";
			}
			out.append(header_+mimeh+"\r\n");
			for(Iterator<Map<Integer, String>> it = res.iterator(); it.hasNext();){
				Map<Integer, String> e = it.next();
				if(e.get(0)==null)continue;
				Date from = null;
				try {
					from = dm.parse(e.get(0), "yyyy/MM/dd HH:mm:ss");
				} catch (ParseException e1) {
					continue;
				}
				count++;
				if(start==0){
					start = from.getTime();
				}
				if(start + permili < from.getTime()){
					String lin = "";
					lin += dm.format(new Date(start), "yyyy/MM/dd HH:mm:ss")+"\t";
					lin += count+"\t"+(count/persecs)+"\t";
					double dcount = count;
					lin += sum0+"\t"+(sum0/dcount/1000.0D)+"\t";
					lin += sum5+"\t"+(sum5/dcount/1000.0D)+"\t";
					lin += sum6+"\t"+(sum6/dcount/1000.0D)+"\t";
					lin += sum7+"\t"+(sum7/dcount/1000.0D)+"\t";
					lin += sum3+"\t"+(sum3/dcount)+"\t";
					lin += sum15+"\t"+(sum15/dcount)+"\t";
					lin += sum16+"\t"+(sum16/dcount)+"\t";
					lin += sum9+"\t"+(sum9/dcount)+"\t";
					lin += sum17+"\t"+(sum17/dcount)+"\t";
					lin += sum18+"\t"+(sum18/dcount)+"\t";
					lin += sum4+"\t"+((double)sum4/dcount)+"\t";
					lin += sum10+"\t"+((double)sum10/dcount)+"\t";
					lin += sum14+"\t"+((double)sum14/dcount)+"\t";
					for(String t : types){
						Long n = !mimes.containsKey(t)?0L:mimes.get(t);
						lin += n+"\t"+((double)n/dcount)+"\t";
					}
					out.append(lin+"\n");
					start += permili;
					sum0 = 0L;
					sum3 = 0L;
					sum4 = 0L;
					sum5 = 0L;
					sum6 = 0L;
					sum7 = 0L;
					sum9 = 0L;
					sum10 = 0L;
					sum14 = 0L;
					sum15 = 0L;
					sum16 = 0L;
					sum17 = 0L;
					sum18 = 0L;
					mimes = new LinkedHashMap<String, Long>();
					count = 0L;
				}
				long five = (e.get(5)==null||e.get(5)=="")?0L:Long.parseLong(e.get(5));
				long six = (e.get(6)==null||e.get(6)=="")?0L:Long.parseLong(e.get(6));
				long seven = (e.get(7)==null||e.get(7)=="")?0L:Long.parseLong(e.get(7));
				sum0 += five + six + seven;
				sum3 += (e.get(3)==null||e.get(3)=="")?0L:Long.parseLong(e.get(3));
				sum4 += (e.get(4)==null||e.get(4)=="")?0L:Long.parseLong(e.get(4));
				sum5 += five;
				sum6 += six;
				sum7 += seven;
				sum9 += (e.get(9)==null||e.get(9)=="")?0L:Long.parseLong(e.get(9));
				sum10 += (e.get(10)==null||e.get(10)=="")?0L:1L;
				sum14 += (e.get(14)==null||e.get(14)=="")?0L:Long.parseLong(e.get(14));
				sum15 += (e.get(15)==null||e.get(15)=="")?0L:Long.parseLong(e.get(15));
				sum16 += (e.get(16)==null||e.get(16)=="")?0L:Long.parseLong(e.get(16));
				sum17 += (e.get(17)==null||e.get(17)=="")?0L:Long.parseLong(e.get(17));
				sum18 += (e.get(18)==null||e.get(18)=="")?0L:Long.parseLong(e.get(18));
				String ct = e.get(8);
				if(!mimes.containsKey(ct))mimes.put(ct, 0L);
				mimes.put(ct, mimes.get(ct) + 1L);
			}
			String lin = "";
			lin += dm.format(new Date(start), "yyyy/MM/dd HH:mm:ss")+"\t";
			lin += count+"\t"+(count/persecs)+"\t";
			double dcount = count;
			lin += sum0+"\t"+(sum0/dcount/1000.0D)+"\t";
			lin += sum5+"\t"+(sum5/dcount/1000.0D)+"\t";
			lin += sum6+"\t"+(sum6/dcount/1000.0D)+"\t";
			lin += sum7+"\t"+(sum7/dcount/1000.0D)+"\t";
			lin += sum3+"\t"+(sum3/dcount)+"\t";
			lin += sum15+"\t"+(sum15/dcount)+"\t";
			lin += sum16+"\t"+(sum16/dcount)+"\t";
			lin += sum9+"\t"+(sum9/dcount)+"\t";
			lin += sum17+"\t"+(sum17/dcount)+"\t";
			lin += sum18+"\t"+(sum18/dcount)+"\t";
			lin += sum4+"\t"+((double)sum4/dcount)+"\t";
			lin += sum10+"\t"+((double)sum10/dcount)+"\t";
			lin += sum14+"\t"+((double)sum14/dcount)+"\t";
			for(String t : types){
				Long n = !mimes.containsKey(t)?0L:mimes.get(t);
				lin += n+"\t"+((double)n/dcount)+"\t";
			}
			out.append(lin+"\n");
		} catch (NumberFormatException e) {
			System.out.println(Utils.getStackTrace(e));
		} catch (FileNotFoundException e) {
			System.out.println(Utils.getStackTrace(e));
		} catch (UnsupportedEncodingException e) {
			System.out.println(Utils.getStackTrace(e));
		} catch (IOException e) {
			System.out.println(Utils.getStackTrace(e));
		}
		finally{
			try {
				if(br!=null)br.close();
				if(out!=null)out.close();
			} catch (IOException e) {
			}
		}
	}
}
