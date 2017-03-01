package com.kikisoftware.hydrant.server;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.MissingResourceException;
import java.util.Set;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;


import org.apache.log4j.Logger;

import com.kikisoftware.hydrant.Utils;
import com.kikisoftware.hydrant.log.Stats;

public class TcpServer implements Runnable {
	public static final Logger log_ = Logger.getLogger(TcpServer.class);
	public static String SSLKey;
	private static SSLContext sslCtx;
	static {
		try {
			SSLKey = Utils.getSslKey();
		} catch (MissingResourceException MRE) {
		} finally {
			if(SSLKey==null)SSLKey = "";
		}
		if(!SSLKey.equals("")){
			String trustStoreFile = Utils.getSslTrust();
			String keystoretype = Utils.getSslKeyStoreType();
			String truststoretype = Utils.getSslTrustStoreType();
			String kpass = Utils.getSslKeyPass();
			String tpass = Utils.getSslTrustPass();
			String kalgo = Utils.getSslKeyAlgorith();
			String talgo = Utils.getSslTrustAlgorith();
			String ssltype = Utils.getSslType();

			String keyStoreFile = SSLKey;

			try {
				KeyStore ks = KeyStore.getInstance(keystoretype);
				char[] kpassphrase = kpass.toCharArray();
				ks.load(new FileInputStream(keyStoreFile), kpassphrase);
				KeyManagerFactory kmf = KeyManagerFactory.getInstance(kalgo);
				kmf.init(ks, kpassphrase);

				TrustManagerFactory tmf = null;
				if(!trustStoreFile.equals("")){
					KeyStore ts = KeyStore.getInstance(truststoretype);
					char[] tpassphrase = tpass.toCharArray();
					ts.load(new FileInputStream(trustStoreFile), tpassphrase);
					tmf = TrustManagerFactory.getInstance(talgo);
					tmf.init(ts);
				}

				sslCtx = SSLContext.getInstance(ssltype);

				sslCtx.init(kmf.getKeyManagers(), tmf==null?null:tmf.getTrustManagers(), null);

			} catch (UnrecoverableKeyException e) {
				log_.error(Utils.getStackTrace(e));
				System.exit(-1);
			} catch (KeyManagementException e) {
				log_.error(Utils.getStackTrace(e));
				System.exit(-1);
			} catch (KeyStoreException e) {
				log_.error(Utils.getStackTrace(e));
				System.exit(-1);
			} catch (NoSuchAlgorithmException e) {
				log_.error(Utils.getStackTrace(e));
				System.exit(-1);
			} catch (CertificateException e) {
				log_.error(Utils.getStackTrace(e));
				System.exit(-1);
			} catch (FileNotFoundException e) {
				log_.error(Utils.getStackTrace(e));
				System.exit(-1);
			} catch (IOException e) {
				log_.error(Utils.getStackTrace(e));
				System.exit(-1);
			}
		}
	}

	private String _host;

	/**
	 *
	 */
	private int _port;

	/**
	 *
	 */
	private Selector _selector = null;
	public Selector getSelector(){return this._selector;}

	/**
	 *
	 */
	private ServerSocketChannel _serverSocketChannel = null;

	private boolean ssl;
	/**
	 * close待ちのSelectionKeyを格納する
	 */
	private LinkedList<SelectionKey> _closeWaitList = new LinkedList<SelectionKey>();
	public LinkedList<SelectionKey> getCloseWaitList() {return this._closeWaitList;}

	/**
	 *
	 * @param host
	 * @param port
	 */
	public TcpServer(String host, int port, boolean ssl) {
		if (host==null || port<0) {
			log_.fatal("Host or Port configuration error.");
			System.exit(-1);
		}

		this._host = host;
		this._port = port;
		this.ssl = ssl;
		log_.info("Server Host=" + this._host+",Port" + (ssl?"[SSL]":"") + "=" + this._port+(Utils.getlocalDNSResolver() ? ":Use Local DNS Resolver" : ""));
	}

	/**
	 *
	 */
	@Override
	public void finalize() throws IOException {
		if (this._serverSocketChannel != null) {
			try {
				this._serverSocketChannel.close();
				this._serverSocketChannel = null;
			}catch(IOException IOE) {
				log_.warn(TcpServer.class.getSimpleName() + ":'ServerSocketChannel.close' " +
						Utils.getStackTrace(IOE));
			}
		}
		if (this._selector != null) {
			try {
				this._selector.close();
				this._selector = null;
			} catch(IOException IOE) {
				log_.warn(TcpServer.class.getSimpleName() + ":'selector.close' " +
						Utils.getStackTrace(IOE));
			}
		}
	}

	@Override
	public void run() {
		try {
			this._selector = Selector.open();
			this._serverSocketChannel = ServerSocketChannel.open();
			this._serverSocketChannel.configureBlocking(false);

			InetSocketAddress isa = null;
			if (Utils.getlocalDNSResolver()) {
				InetAddress address = InetAddress.getByName(this._host);
				isa = new InetSocketAddress(address, this._port);
			} else {
				isa = new InetSocketAddress(this._port);
			}
			this._serverSocketChannel.socket().bind(isa, 128);
			this._serverSocketChannel.register(this._selector, SelectionKey.OP_ACCEPT);
		} catch (BindException BE) {
			log_.fatal(TcpServer.class.getSimpleName() + ":Cannot assign requested address." +
					"(" +this._host+ ":" +this._port+ ")" +
					"\nPlease check hydrant.properties.\n" +
					Utils.getStackTrace(BE) +
					"shutdown");
			System.exit(-1);
		} catch (Exception e) {
			log_.fatal(TcpServer.class.getSimpleName() + ":" + Utils.getStackTrace(e) +
						"\nshutdown");
			System.exit(-1);
		}

		while(true) {
			try {
				if (this._selector.select(Utils.getTimeoutCheckInterval()) > 0) {
					Set<SelectionKey> readyKeySet = this._selector.selectedKeys();
					synchronized (readyKeySet) {
						for (Iterator<SelectionKey> I = readyKeySet.iterator(); I.hasNext();) {
							SelectionKey key = I.next();
							I.remove();
							if (!key.isValid()){
								key.channel().close();
								key.cancel();
								continue;
							}
							if (key.isAcceptable()) {
								processAccept(key);
							} else if (key.isReadable()) {
								processRead(key);
							}
						}
					}
				}
				erace();
			} catch (Exception e) {
				//認識していないエラー
				Stats lg = Stats.getInstance();
				lg.countUpUnKnownError();
				log_.warn(TcpServer.class.getSimpleName() + ":Unrecognized error has caused.\n"+Utils.getStackTrace(e));
			}
		}
	}

	/**
	 * クライアントから接続があった場合の処理
	 *
	 * @param sKey
	 * @throws Exception
	 */
	private void processAccept(SelectionKey sKey) throws Exception {
		ServerSocketChannel serverChannel = (ServerSocketChannel)sKey.channel();
		SocketChannel channel = serverChannel.accept();
		if(channel!=null){
			channel.configureBlocking(false);
			SelectionKey processKey = channel.register(this._selector, SelectionKey.OP_READ);

			RequestProcessor rp = null;
			if(ssl){
				SSLEngineManager man = SSLEngineManager.getInstance(sslCtx, channel);
				rp = RequestProcessor.getInstance(this, processKey, man);
			}
			else
				rp = RequestProcessor.getInstance(this, processKey, null);
			processKey.attach(rp);

			Stats lg = Stats.getInstance();
			lg.countUpAccept();
		}
	}

	/**
	 * クライアントからデータが送信されてきた場合の処理
	 *
	 * @param sKey
	 * @throws Exception
	 */
	public void processRead(SelectionKey sKey) throws Exception {
		RequestProcessor RP = (RequestProcessor)sKey.attachment();

		try {
			int count = 0;

			if(ssl){
				SSLEngineManager mgr = RP.getSSLManager();
				if(!mgr.usable())return;
				ByteBuffer  request = mgr.getAppRecvBuffer();
				count = mgr.read();
				if (count < 0) {
					finishSelectionKey(sKey);
				} else if (count > 0) {
					RP.processRequestBytes(request.array(), count);//!SSLKey.equals(""));
					request.clear();
				}
			}
			else{
				SocketChannel channel = (SocketChannel)sKey.channel();
				if(!channel.isConnected() || !channel.isOpen() || !sKey.isValid() || !sKey.isReadable())return;
				ByteBuffer byteBuffer = RP.getByteBuffer();
				byteBuffer.clear();
				count = channel.read(byteBuffer);
				if (count < 0) {
					finishSelectionKey(sKey);
				} else if (count > 0) {
					RP.processRequestBytes(byteBuffer.array(), count);//!SSLKey.equals(""));
				} else {
					// ありえないと思う　→　別スレッドでやることになったので普通にありうる ←　戻したので復活させる
					log_.warn(TcpServer.class.getSimpleName() + "#processRead:count is 0.");
				}
			}
		} catch (Exception IOE) {
			//読み込みエラー
			Stats lg = Stats.getInstance();
			lg.countUpReadSocketError();
			log_.info(TcpServer.class.getSimpleName() + ":" + IOE.getMessage());//Helper.getStackTrace(IOE));
			sKey.channel().close();
			sKey.cancel();
		}
	}

	public void erace() {
		// TimeOutチェック
		// PERFORMANCE WEEK POINT?
		long checkTime = System.currentTimeMillis() - Utils.getRequestTimeout();
		Set<SelectionKey> keySet = this._selector.keys();
		synchronized (keySet) {
			for (Iterator<SelectionKey> I = keySet.iterator(); I.hasNext();) {
				SelectionKey key = I.next();
				if (!key.isValid()){
					try {
						key.channel().close();
					} catch (IOException e) {
					}
					key.cancel();
					continue;
				}

				Object tmpAttachment = key.attachment();

				if (tmpAttachment != null
						&& tmpAttachment instanceof RequestProcessor) {
					RequestProcessor RP = (RequestProcessor) tmpAttachment;
					if (RP.getLastAccessTime() < checkTime) {
						// タイムオーバーの場合
						RP.rejectNewRequest();
					}
				}
			}
		}
		// socketの終了
		synchronized(this._closeWaitList) {
			if (this._closeWaitList.size() > 0) {
				for(Iterator<SelectionKey> I=this._closeWaitList.iterator() ; I.hasNext() ;) {
					SelectionKey sKey = I.next();
					try {
						sKey.channel().close();
						sKey.cancel();
						I.remove();
					} catch (IOException e) {
						log_.error(Utils.getStackTrace(e));
					}
				}
			}
		}
	}
	private void finishSelectionKey(SelectionKey sKey) {
		RequestProcessor RP = (RequestProcessor)sKey.attachment();
		RP.connectionClose();
	}
	public void processCloseChannel(SelectionKey sKey) {
		synchronized(this._closeWaitList) {
			this._closeWaitList.addLast(sKey);
		}
	}
}
