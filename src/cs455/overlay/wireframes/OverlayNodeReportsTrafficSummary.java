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
import cs455.overlay.util.StatisticsCollectorAndDisplay;

public class OverlayNodeReportsTrafficSummary implements Event {
	private final Protocol msgType = Protocol.OVERLAY_NODE_REPORTS_TRAFFIC_SUMMARY;
	private int packetsSent;
	private int packetsRelayed;
	private int packetsReceived;
	private long sumSent;
	private long sumRecv;
	private int id;
	private TCPConnection responseConnection;
	
	public OverlayNodeReportsTrafficSummary (StatisticsCollectorAndDisplay s) {
		this.packetsSent = s.getPacketsSent();
		this.packetsRelayed = s.getPacketsRelayed();
		this.packetsReceived = s.getPacketsReceived();
		this.sumSent = s.getSumPacketSent();
		this.sumRecv = s.getSumPacketReceived();
		this.id = s.getNodeID();
	}
	
	public OverlayNodeReportsTrafficSummary(int packetsSent, int packetsRelayed, 
			int packetsReceived, long sumSent, long sumRecv, int id) 
	{
		this.packetsSent = packetsSent;
		this.packetsRelayed = packetsRelayed;
		this.packetsReceived = packetsReceived;
		this.sumSent = sumSent;
		this.sumRecv = sumRecv;
		this.id = id;
	}
	
	public OverlayNodeReportsTrafficSummary(byte [] msg, TCPConnection incomingConn) {
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
		id = din.readInt(); 				// Read the id
		packetsSent = din.readInt(); 		// Read packets sent
		packetsRelayed = din.readInt(); 	// Read the num packets relayed
		sumSent = din.readLong();			// Read the sum of data sent
		packetsReceived	= din.readInt();	// Read the number of packets received
		sumRecv	= din.readLong();			// Read the sum of data received
		
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
			dout.writeInt(id);
			dout.writeInt(packetsSent);
			dout.writeInt(packetsRelayed);
			dout.writeLong(sumSent);
			dout.writeInt(packetsReceived);
			dout.writeLong(sumRecv);
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

	/**
	 * @return the packetsSent
	 */
	public int getPacketsSent() {
		return packetsSent;
	}

	/**
	 * @return the packetsRelayed
	 */
	public int getPacketsRelayed() {
		return packetsRelayed;
	}

	/**
	 * @return the packetsReceived
	 */
	public int getPacketsReceived() {
		return packetsReceived;
	}

	/**
	 * @return the sumSent
	 */
	public long getSumSent() {
		return sumSent;
	}

	/**
	 * @return the sumRecv
	 */
	public long getSumRecv() {
		return sumRecv;
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

}
