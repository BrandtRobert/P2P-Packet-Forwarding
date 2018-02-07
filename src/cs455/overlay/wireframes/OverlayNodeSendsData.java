package cs455.overlay.wireframes;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import cs455.overlay.transport.TCPConnection;

public class OverlayNodeSendsData implements Event {
	private final Protocol msgType = Protocol.OVERLAY_NODE_SENDS_DATA;
	private int destID;
	private int srcID;
	private int payload;
	private int dissTraceLength;
	private int [] dissTrace;
	private TCPConnection responseConnection;
	
	public OverlayNodeSendsData(int destID, int srcID, int payload) {
		this.destID = destID;
		this.srcID = srcID;
		this.payload = payload;
		this.dissTraceLength = 0;
	}
	
	public OverlayNodeSendsData(byte [] msg, TCPConnection incomingConnection) {
		responseConnection = incomingConnection;
		try {
			unmarshalBytes(msg);
		} catch (IOException e) {
			System.err.println("Unable to unmarshal bytes for OverlayNodeSendsData");
		}
	}
	
	/**
	 * byte: Message type; OVERLAY_NODE_SENDS_DATA 
	 * int: Destination ID
	 * int: Source ID
	 * int: Payload
	 * int: Dissemination trace field length (number of hops)
	 * int[^^]: Dissemination trace comprising nodeIDs that the packet traversed through
	 * @param msg
	 * @throws IOException
	 */
	private void unmarshalBytes(byte msg []) throws IOException {
		ByteArrayInputStream baInputStream = new ByteArrayInputStream(msg);
		DataInputStream din = new DataInputStream (new BufferedInputStream(baInputStream));
		din.readByte();		// Throw away the first byte (the msg type)
		
		this.destID = din.readInt(); // Read the dest id
		this.srcID = din.readInt(); // Read the src id
		this.payload = din.readInt(); // Read the message payload
		this.dissTraceLength = din.readInt(); // Read the src id
		// Read the dissemination trace
		this.dissTrace = new int [dissTraceLength];
		for (byte i = 0; i < dissTraceLength; i++) {
			this.dissTrace[i] = din.readInt();
		}
		
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
			dout.writeInt(destID);
			dout.writeInt(srcID);
			dout.writeInt(payload);
			dout.writeInt(dissTraceLength);
			if (dissTraceLength > 0) {
				for (int i = 0; i < dissTrace.length; i++) {
					dout.writeInt(dissTrace[i]);
				}
			}
			// Get the backing byte values
			dout.flush();
			marshalledBytes = baOutputStream.toByteArray();
			// Close output streams
			baOutputStream.close();
			dout.close();
			return marshalledBytes;
		} catch (IOException e) {
			System.err.println("Failed to marhsal bytes for OverlayNodeSendsData");
			return null;
		}
	}

	/**
	 * Expand the dissemination trace array to include this node.
	 * Not an efficient operation but dissemination trace should be small enough that it doesn't matter.
	 * @param nodeId - id of the node to add to the trace
	 */
	public void addToDisseminationTrace (int nodeId) {
		int [] tempArray = new int [dissTraceLength + 1];
		for (int i = 0; i < dissTraceLength; i++) {
			tempArray[i] = dissTrace[i];
		}
		tempArray[dissTraceLength] = nodeId;
		dissTrace = tempArray;
		dissTraceLength = dissTrace.length;
	}
	
	@Override
	public TCPConnection getResponseConnection() {
		return responseConnection;
	}

	/**
	 * @return the destID
	 */
	public int getDestID() {
		return destID;
	}

	/**
	 * @return the srcID
	 */
	public int getSrcID() {
		return srcID;
	}

	/**
	 * @return the dissTrace
	 */
	public int[] getDissTrace() {
		return dissTrace;
	}
	
	public int getPayload() {
		return payload;
	}

}
