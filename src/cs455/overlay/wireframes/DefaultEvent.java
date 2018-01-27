package cs455.overlay.wireframes;

import cs455.overlay.transport.TCPConnection;

/**
 * Only exists for testing purposes
 * @author Brandt Reutimann
 *
 */
public class DefaultEvent implements Event {
	private String msgStr = null;
	private byte[] strBytes = null;
	private TCPConnection responseConn = null;
	
	public DefaultEvent (byte[] msg, TCPConnection res) {
		strBytes = msg;
		msgStr = new String (msg);
		responseConn = res;
	}
	
	public DefaultEvent (String str) {
		msgStr = str;
		strBytes = str.getBytes();
	}
	
	public Protocol getType() {
		return null;
	}
	
	public byte[] getBytes() {
		return strBytes;
	}
	
	public TCPConnection getResponseConnection() {
		return responseConn;
	}
}
