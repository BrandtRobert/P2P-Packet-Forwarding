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

public class OverlayNodeSendsDeregistration implements Event {
	private InetAddress IPaddr;
	private int port;
	private byte IPLength;
	private int registeredID;
	private TCPConnection responseConnection;
	private final Protocol msgType = Protocol.OVERLAY_NODE_SENDS_DEREGISTRATION;
	
	public OverlayNodeSendsDeregistration(InetAddress IPaddr, int port, int id) {
		this.IPaddr = IPaddr;
		this.IPLength = (byte) IPaddr.getAddress().length;
		this.port   = port;
		this.registeredID = id;
		this.responseConnection = null;
	}
	
	public OverlayNodeSendsDeregistration(byte [] msg, TCPConnection incomingConn) {
		this.responseConnection = incomingConn;
		try {
			unmarshalBytes(msg);
		} catch (IOException e) {
			System.err.println("Failed to decode OverlayNodeSendsDeregistration");
			e.printStackTrace();
		}
	}

	/**
	 * Builds the event from a byte []
	 * byte: Message Type (OVERLAY_NODE_SENDS_DEREGISTRATION)
	 * byte: length of following "IP address" field
	 * byte[^^]: IP address; from InetAddress.getAddress()
	 * int: Port number
	 * int: assigned Node ID
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
		
		this.port = din.readInt();				// Read the port number
		this.registeredID = din.readInt();     	// Read the assigned ID
		
		baInputStream.close();
		din.close();
	}
	
	@Override
	public Protocol getType() {
		return this.msgType;
	}

	/**
	 * byte: Message Type (OVERLAY_NODE_SENDS_DEREGISTRATION)
	 * byte: length of following "IP address" field
	 * byte[^^]: IP address; from InetAddress.getAddress()
	 * int: Port number
	 * int: assigned Node ID
	 */
	@Override
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
			dout.writeInt(registeredID);
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
		return this.responseConnection;
	}

	/**
	 * @return the IP address
	 */
	public InetAddress getInetAddress() {
		return IPaddr;
	}

	/**
	 * @return the port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * @return the registeredID
	 */
	public int getRegisteredID() {
		return registeredID;
	}

}
