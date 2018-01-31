package cs455.overlay.wireframes;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import cs455.overlay.transport.TCPConnection;

public class RegistryRequestsTaskInitiate implements Event {
	private final Protocol msgType = Protocol.REGISTRY_REQUESTS_TASK_INITIATE;
	private int numPackets;
	private TCPConnection responseConnection;
	
	public RegistryRequestsTaskInitiate(int packetsToSend) {
		this.numPackets = packetsToSend;
	}
	
	public RegistryRequestsTaskInitiate(byte [] msg, TCPConnection incomingConnection) {
		this.responseConnection = incomingConnection;
		try {
			unmarshalBytes(msg);
		} catch (IOException e) {
			System.err.println("Unable to unmarshal bytes for RegistryRequestsTaskInitiate");
		}
	}

	private void unmarshalBytes(byte [] msg) throws IOException {
		ByteArrayInputStream baInputStream = new ByteArrayInputStream(msg);
		DataInputStream din = new DataInputStream (new BufferedInputStream(baInputStream));
		din.readByte();		// Throw away the first byte (the msg type)
		this.numPackets = din.readInt();	// Get num packets
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
			dout.writeInt(numPackets);
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
		return responseConnection;
	}
	
	public int getNumPackets() {
		return this.numPackets;
	}

}
