package cs455.overlay.wireframes;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;

import cs455.overlay.transport.TCPConnection;

public class OverlayNodeReportsTaskFinished implements Event {
	private Protocol msgType = Protocol.OVERLAY_NODE_REPORTS_TASK_FINISHED;
	private TCPConnection responseConnection;
	private int port;
	private int id;
	private InetAddress ipAddr;
	
	public OverlayNodeReportsTaskFinished(InetAddress ip, int port, int id) {
		this.ipAddr = ip;
		this.port = port;
		this.id = id;
	}
	
	public OverlayNodeReportsTaskFinished(byte [] msg, TCPConnection incomingConn) {
		this.responseConnection = incomingConn;
		try {
			unmarshalBytes(msg);
		} catch (IOException e) {
			System.err.println("Failed to decode OverlayNodeSendsDeregistration");
			e.printStackTrace();
		}
	}

	private void unmarshalBytes(byte[] msg) throws IOException {
		ByteArrayInputStream baInputStream = new ByteArrayInputStream(msg);
		DataInputStream din = new DataInputStream (new BufferedInputStream(baInputStream));
		din.readByte();		// Throw away the first byte (the msg type)
		// Read the IP address
		byte IPLength = din.readByte();
		byte [] ipaddr = new byte [IPLength];
		din.readFully(ipaddr, 0, IPLength);
		this.ipAddr = InetAddress.getByAddress(ipaddr);
		
		this.port = din.readInt();		// Read the port number
		this.id = din.readInt();     	// Read the assigned ID
		
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
			dout.writeByte((byte)ipAddr.getAddress().length);
			dout.write(ipAddr.getAddress());
			dout.writeInt(port);
			dout.writeInt(id);
			// Get the backing byte values
			dout.flush();
			marshalledBytes = baOutputStream.toByteArray();
			// Close output streams
			baOutputStream.close();
			dout.close();
			return marshalledBytes;
		} catch (IOException e) {
			System.err.println("Failed to marhsal bytes for OverlayNodeSendsRegistration");
			return null;
		}
	}

	@Override
	public TCPConnection getResponseConnection() {
		return responseConnection;
	}
	
	public InetAddress getIP() {
		return ipAddr;
	}
	
	public int getID() {
		return id;
	}

	public int getPort() {
		return port;
	}
	
}
