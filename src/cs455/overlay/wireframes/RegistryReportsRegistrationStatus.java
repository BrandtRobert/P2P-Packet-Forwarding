package cs455.overlay.wireframes;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import cs455.overlay.transport.TCPConnection;

public class RegistryReportsRegistrationStatus implements Event {
	
	protected Protocol msgType = Protocol.REGISTRY_REPORTS_REGISTRATION_STATUS;
	private int successStatus;
	private byte infoLength;
	private String infoString;
	private TCPConnection responseConnection;
	
	public RegistryReportsRegistrationStatus(int status, String infoString) {
		this.successStatus = status;
		// If the info string is too big then truncate it
		if (infoString.length() >= Byte.MAX_VALUE) {
			infoString = infoString.substring(0, Byte.MAX_VALUE);
		}
		this.infoString = infoString;
		this.infoLength = (byte) infoString.length();
	}

	public RegistryReportsRegistrationStatus(byte[] msg, TCPConnection incomingConnection) {
		this.responseConnection = incomingConnection;
		try {
			unmarhsalBytes(msg);
		} catch (IOException e) {
			System.err.println("Failed to unmarshal bytes for RegistryReportsRegistrationStatus");
			e.printStackTrace();
		}
	}
	
	/**
	 * byte: Message type (REGISTRY_REPORTS_REGISTRATION_STATUS)
	 * int: Success status; Assigned ID if successful, -1 in case of a failure
	 * byte: Length of following "Information string" field
	 * byte[^^]: Information string; ASCII charset
	 */
	public void unmarhsalBytes (byte[] msg) throws IOException {
		ByteArrayInputStream baInputStream = new ByteArrayInputStream(msg);
		DataInputStream din = new DataInputStream (new BufferedInputStream(baInputStream));
		din.readByte();		// Throw away the first byte (the msg type)
		this.successStatus = din.readInt();
		this.infoLength = din.readByte();
		byte[] infoArr = new byte [infoLength];
		din.readFully(infoArr, 0, infoLength);
		this.infoString = new String (infoArr);
		
		baInputStream.close();
		din.close();
	}
	
	@Override
	public Protocol getType() {
		return msgType;
	}

	@Override
	/**
	 * byte: Message type (REGISTRY_REPORTS_REGISTRATION_STATUS)
	 * int: Success status; Assigned ID if successful, -1 in case of a failure
	 * byte: Length of following "Information string" field
	 * byte[^^]: Information string; ASCII charset
	 */
	public byte[] getBytes() {
		try {
			byte [] marshalledBytes = null;
			ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
			DataOutputStream dout = new DataOutputStream (new BufferedOutputStream (baOutputStream));
			// Write values to a byte stream
			dout.writeByte(msgType.getValue());
			dout.writeInt(successStatus);
			dout.writeByte(infoLength);
			dout.write(infoString.getBytes());
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
	
	public int getSuccessStatus() {
		return successStatus;
	}
	
	public String getInfoString() {
		return infoString;
	}
}
