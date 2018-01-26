package cs455.overlay.wireframes;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;

import cs455.overlay.transport.TCPConnection;

public class OverlayNodeSendsRegistration implements Event {
	private InetAddress IPaddr;
	private int port;
	private byte IPLength;
	private TCPConnection responseConnection;
	private final Protocol msgType = Protocol.OVERLAY_NODE_SENDS_REGISTRATION;
	
	public OverlayNodeSendsRegistration (InetAddress IPaddr, int port) {
		this.IPaddr   = IPaddr;
		this.port     = port;
		this.IPLength = (byte) IPaddr.getAddress().length;
	}
	
	public OverlayNodeSendsRegistration (byte [] msg, TCPConnection incomingConnection) {
		this.responseConnection = incomingConnection;
		try {
			unmarshalBytes(msg);
		} catch (IOException e) {
			System.err.println("Failed to decode the msg");
			e.printStackTrace();
		}
	}

	/**
	 * Builds the event from a byte []
	 * byte: Message Type (OVERLAY_NODE_SENDS_REGISTRATION)
	 * byte: length of following "IP address" field
	 * byte[^^]: IP address; from InetAddress.getAddress()
	 * int: Port number
	 * @throws IOException 
	 */
	private void unmarshalBytes (byte [] msg) throws IOException {
		ByteArrayInputStream baInputStream = new ByteArrayInputStream(msg);
		DataInputStream din = new DataInputStream (new BufferedInputStream(baInputStream));
		din.readByte();		// Throw away the first byte (the msg type)
		// Read the IP address
		this.IPLength = din.readByte();
		byte [] ipaddr = new byte [IPLength];
		din.readFully(ipaddr, 0, IPLength);
		this.IPaddr = InetAddress.getByAddress(ipaddr);
		// Read the port number
		this.port = din.readInt();
		
		baInputStream.close();
		din.close();
	}
	
	@Override
	public Protocol getType() {
		return msgType;
	}

	@Override
	/**
	 * Builds the byte [] message 
	 * byte: Message Type (OVERLAY_NODE_SENDS_REGISTRATION)
	 * byte: length of following "IP address" field
	 * byte[^^]: IP address; from InetAddress.getAddress()
	 * int: Port number
	 */
	public byte[] getBytes() {
		try {
			byte [] marshalledBytes = null;
			ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
			DataOutputStream dout = new DataOutputStream (new BufferedOutputStream (baOutputStream));
			// Write values to a byte stream
			dout.writeByte(msgType.getValue());
			dout.writeByte(IPLength);
			dout.write(IPaddr.getAddress());
			dout.writeInt(port);
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
	
	public InetAddress getInetAddress() {
		return IPaddr;
	}
	
	public int getPort() {
		return port;
	}
	
}
