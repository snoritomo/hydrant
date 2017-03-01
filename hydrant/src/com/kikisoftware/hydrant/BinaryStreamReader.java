package com.kikisoftware.hydrant;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class BinaryStreamReader {
	private InputStream is;
	private Byte pre;
	public BinaryStreamReader(InputStream source){
		is = source;
		pre = null;
	}
	private ArrayList<Byte> getInitBuf(){
		ArrayList<Byte> bl = new ArrayList<Byte>();
		if(pre!=null){
			bl.add(pre);
			pre = null;
		}
		return bl;
	}
	public ArrayList<Byte> readLine() throws IOException{
		byte[] buf = new byte[1];
		ArrayList<Byte> bl = getInitBuf();
		if(bl.size()==1 && (bl.get(0)==0x0D || bl.get(0)==0x0A)){
			bl.clear();
			return bl;
		}
		while(true){
			int len = is.read(buf);
			if(len<=0)break;
			if(buf[0]==0x0D || buf[0]==0x0A){//\r \n
				if(buf[0]==0x0D){//\r
					len = is.read(buf);
					if(len>0 && buf[0]!=0x0A){
						pre = buf[0];
					}
				}
				break;
			}
			bl.add((byte)buf[0]);
		}
		return bl;
	}
	public ArrayList<Byte> readAll() throws IOException{
		byte[] buf = new byte[1024];
		ArrayList<Byte> bl = getInitBuf();
		if(bl.size()==1 && (bl.get(0)==0x0D || bl.get(0)==0x0A)){
			bl.clear();
			return bl;
		}
		while(true){
			int len = is.read(buf);
			if(len<0)break;
			for(int i = 0; i < len; i++){
				bl.add(buf[i]);
			}
		}
		return bl;
	}
}
