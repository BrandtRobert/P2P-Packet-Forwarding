package cs455.overlay.wireframes;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cs455.overlay.routing.RoutingTableEntry;
import cs455.overlay.transport.TCPConnection;

public class RegistrySendsNodeManifest implements Event {
	private final Protocol msgType = Protocol.REGISTRY_SENDS_NODE_MANIFEST;
	private List<RoutingTableEntry> routingManifest;
	private int routingTableSize;
	private TCPConnection responseConnection;
	private Integer [] allNodeIds;
	
	public RegistrySendsNodeManifest(List<RoutingTableEntry> routingTable, Integer [] nodeIds) {
		this.routingManifest = routingTable;
		this.routingTableSize = routingTable.size();
		this.allNodeIds = nodeIds;
		this.responseConnection = null;
	}
	
	public RegistrySendsNodeManifest(byte [] msg, TCPConnection incomingConn) {
		this.responseConnection = incomingConn;
		try {
			this.unmarshalBytes(msg);
		} catch (IOException e) {
			System.err.println("Failed to decode message for RegistrySendsNodeManifest");
			e.printStackTrace();
		}
	}
	
	/**
	 * byte: Message type; REGISTRY_SENDS_NODE_MANIFEST
	 * byte: routing table size NR
	 * int: Node ID of node 1 hop away
	 * byte: length of following "IP address" field
	 * byte[^^]: IP address of node 1 hop away; from InetAddress.getAddress()
	 * int: Port number of node 1 hop away
	 * byte: Number of node IDs in the system
	 * int[^^]: List of all node IDs in the system [Note no IPs are included]
	 * @param msg - the byte stream from the client
	 * @throws IOException
	 */
	private void unmarshalBytes(byte [] msg) throws IOException {
		ByteArrayInputStream baInputStream = new ByteArrayInputStream(msg);
		DataInputStream din = new DataInputStream (new BufferedInputStream(baInputStream));
		din.readByte();		// Throw away the first byte (the msg type)
		this.routingTableSize = din.readByte();		// Get the number of entries in the routing table
		this.routingManifest = new ArrayList<RoutingTableEntry>(this.routingTableSize);
		// Grab routing manifest
		for (byte i = 0; i < this.routingTableSize; i++) {
			// Get the node ID
			int currID = din.readInt();		
			// Get the IP Address
			byte ipLength = din.readByte();
			byte [] ipAddrBytes = new byte [ipLength];
			din.readFully(ipAddrBytes, 0, ipLength);
			InetAddress ipAddr = InetAddress.getByAddress(ipAddrBytes);
			// Get the node port
			int port = din.readInt();	
			// Add info to the routing table manifest
			RoutingTableEntry currEntry = new RoutingTableEntry(ipAddr, port);
			currEntry.setId(currID);
			this.routingManifest.add(currEntry);
		}
		byte numIDs = din.readByte();
		this.allNodeIds = new Integer[numIDs];
		for (byte i = 0; i < numIDs; i++) {
			int id = din.readInt();
			this.allNodeIds[i] = id;
		}
		baInputStream.close();
		din.close();
	}
	
	@Override
	public Protocol getType() {
		return msgType;
	}

	/**
	 * byte: Message type; REGISTRY_SENDS_NODE_MANIFEST
	 * byte: routing table size NR
	 * int: Node ID of node 1 hop away
	 * byte: length of following "IP address" field
	 * byte[^^]: IP address of node 1 hop away; from InetAddress.getAddress()
	 * int: Port number of node 1 hop away
	 * byte: Number of node IDs in the system
	 * int[^^]: List of all node IDs in the system [Note no IPs are included]
	 * @param msg - the byte stream from the client
	 * @throws IOException
	 */
	@Override
	public byte[] getBytes() {
		try {
			byte [] marshalledBytes = null;
			ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
			DataOutputStream dout = new DataOutputStream (new BufferedOutputStream (baOutputStream));
			dout.writeByte(msgType.getValue());		// Write message size
			dout.writeByte(this.routingTableSize);	// Write routing table size
			// Write routing table entries
			for (byte i = 0; i < this.routingTableSize; i++) {
				RoutingTableEntry currEntry = this.routingManifest.get(i);
				// Write node id
				dout.writeInt(currEntry.getId());
				// Write IP Address
				byte [] ipAddr = currEntry.getIp().getAddress();
				byte ipLength = (byte) ipAddr.length;
				dout.writeByte(ipLength);
				dout.write(ipAddr);
				// Write port num of node
				dout.writeInt(currEntry.getPort());
			}
			// Write the entire node ID manifest
			dout.writeByte((byte) allNodeIds.length);
			for (byte i = 0; i < (byte) allNodeIds.length; i++) {
				dout.writeInt(allNodeIds[i]);
			}
			// Flush stream
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
	
	public List<RoutingTableEntry> getRoutingTable() {
		return this.routingManifest;
	}
	
	public Integer [] getNodeIdManifest() {
		return this.allNodeIds;
	}

}
