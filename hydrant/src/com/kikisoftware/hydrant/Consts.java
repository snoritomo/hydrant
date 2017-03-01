package com.kikisoftware.hydrant;

public class Consts {
	public static final String REAL_HOST_NAME = "Host";
	public static final String REAL_CONTENT_LENGTH = "Content-Length: ";
	public static final String REAL_CONTENT_LENGTH_NAME = "Content-Length";
	public static final String LOWER_CONTENT_LENGTH = "content-length: ";
	public static final String LOWER_CONTENT_LENGTH_FOR_CHECK = "content-length:";
	public static final String LOWER_CHARSET_FOR_CHECK = "charset=";
	public static final String LOWER_CONTENT_TYPE = "content-type: ";
	public static final String LOWER_CONTENT_TYPE_FOR_CHECK = "content-type:";
	public static final String REAL_TRANSFER_ENCODING_NAME = "Transfer-Encoding";
	public static final String LOWER_TRANSFER_ENCODING = "transfer-encoding: ";
	public static final String LOWER_TRANSFER_ENCODING_FOR_CHECK = "transfer-encoding:";
	public static final String REAL_LOCATION = "Location: ";
	public static final String REAL_LOCATION_NAME = "Location";
	public static final String LOWER_LOCATION = "location: ";
	public static final String LOWER_LOCATION_FOR_CHECK = "location:";
	public static final String REAL_CONNECTION = "Connection: ";
	public static final String REAL_ACCEPT_ENCODING_NAME = "Accept-Encoding";
	public static final String REAL_GZIP = "gzip";
	public static final String LOWER_CONNECTION_FOR_CHECK = "connection:";
	public static final String REAL_CHUNKED = "chunked";
	public static final String REAL_CLOSE = "close";
	public static final String REAL_KEEP_ALIVE = "Keep-Alive";
	public static final String USER_AGENT_KEY_NAME = "User-Agent";
	public static final String CRLF = "\r\n";
	public static final String HEADER_SEPARATOR = ": ";
	public static final String HEADER_SEPARATOR_NO_SPACE = ":";
	public static final String HTTP_SCHEME = "http://";
	public static final String HTTPS_SCHEME = "https://";
	public static final String HTTP_SCHEME_NO_SEPARATOR = "http";
	public static final String HTTPS_SCHEME_NO_SEPARATOR = "https";
	public static final String SCHEME_HOST_SEPARATOR = "://";
	public static final String URL_PATH_SEPARATOR = "/";
	public static final String URL_QUERY_SEPARATOR = "?";
	public static final String URL_REF_SEPARATOR = "#";
	public static final String URL_HOSTPORT_SEPARATOR = ":";
	public static final String URL_EXTENSION_SEPARATOR = ".";
	public static final String CONTENT_TYPE_CHARSET_SEPARATOR = ";";
	

	public static final String REQUEST_KEY_MODE = "mode";
	public static final String REQUEST_KEY_SCHEME = "scheme";
	public static final String REQUEST_KEY_HOST = "host";
	public static final String REQUEST_KEY_PORT = "port";
	public static final String REQUEST_KEY_PATH = "path";
	public static final String REQUEST_KEY_FILE = "file";
	public static final String REQUEST_KEY_EXT = "ext";
	public static final String REQUEST_KEY_PARAM = "param";
	public static final String REQUEST_KEY_HEAD = "h_";
	public static final String REQUEST_KEY_BODY = "body";
	public static final String REQUEST_KEY_UASTR = "uastring";
}
