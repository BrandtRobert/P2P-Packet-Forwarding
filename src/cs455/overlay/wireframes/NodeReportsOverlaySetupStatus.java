package cs455.overlay.wireframes;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import cs455.overlay.transport.TCPConnection;

public class NodeReportsOverlaySetupStatus implements Event {
	private final Protocol msgType = Protocol.NODE_REPORTS_OVERLAY_SETUP_STATUS;
	private int successStatus;
	private byte [] infoStr;
	private byte infoStrlength;
	private TCPConnection responseConnection;
	
	public NodeReportsOverlaySetupStatus(int success, String infoStr) {
		// If the info string is too big then truncate it
		if (infoStr.length() >= Byte.MAX_VALUE) {
			infoStr = infoStr.substring(0, Byte.MAX_VALUE);
		}
		this.infoStr = infoStr.getBytes();
		this.infoStrlength = (byte) this.infoStr.length;
		this.successStatus = success;
	}
	
	public NodeReportsOverlaySetupStatus(byte [] msg, TCPConnection incomingConnection) {
		responseConnection = incomingConnection;
		try {
			unmarshalBytes(msg);
		} catch (IOException e) {
			System.err.println("Failed to unmarshal message for NodeReportsOverlaySetupStatus");
		}
	}
	
	/**
	 * byte: Message type (NODE_REPORTS_OVERLAY_SETUP_STATUS)
	 * int: Success status; Assigned ID if successful, -1 in case of a failure 
	 * byte: Length of following "Information string" field
	 * byte[^^]: Information string; ASCII charset
	 * @param msg
	 * @throws IOException 
	 */
	private void unmarshalBytes(byte [] msg) throws IOException {
		ByteArrayInputStream baInputStream = new ByteArrayInputStream(msg);
		DataInputStream din = new DataInputStream (new BufferedInputStream(baInputStream));
		din.readByte();		// Throw away the first byte (the msg type)
		this.successStatus = din.readInt();	// Get success status
		// Get info str
		this.infoStrlength = din.readByte();
		this.infoStr = new byte [infoStrlength];
		din.readFully(infoStr, 0, infoStrlength);
		baInputStream.close();
		din.close();
	}
	
	@Override
	public Protocol getType() {
		return msgType;
	}

	@Override
	public byte[] getBytes() {
		try {
			byte [] marshalledBytes = null;
			ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
			DataOutputStream dout = new DataOutputStream (new BufferedOutputStream (baOutputStream));
			// Write values to a byte stream
			dout.writeByte(msgType.getValue());
			dout.writeInt(successStatus);
			dout.writeByte(infoStrlength);
			dout.write(infoStr);
			// Get the backing byte values
			dout.flush();
			marshalledBytes = baOutputStream.toByteArray();
			// Close output streams
			baOutputStream.close();
			dout.close();
			return marshalledBytes;
		} catch (IOException e) {
			System.err.println("Failed to marhsal bytes for NodeReportsOverlaySetupStatus");
			return null;
		}
	}

	@Override
	public TCPConnection getResponseConnection() {
		return this.responseConnection;
	}

	public String getInfoString() {
		return new String (infoStr);
	}
}
