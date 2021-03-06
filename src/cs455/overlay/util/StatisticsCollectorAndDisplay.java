package cs455.overlay.util;

import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import cs455.overlay.wireframes.OverlayNodeReportsTrafficSummary;

public class StatisticsCollectorAndDisplay {
	private Integer packetsSent;
	private Integer packetsRelayed;
	private Integer packetsReceived;
	private Long sumPacketSent;
	private Long sumPacketReceived;
	private int nodeID;
	// For extended statistics
	private Integer packetsTouched;
	private TreeMap<Integer, NodeCollector> packetsPerNode;
	
	private class NodeCollector {
		Integer packetsSentToNode;
		Integer packetsReceivedFromNode;
		Integer packetsRelayedToNode;
		
		public NodeCollector() {
			packetsSentToNode = 0;
			packetsReceivedFromNode = 0;
			packetsRelayedToNode = 0;
		}
		
		void incrementPacketsSentToNode () {
			packetsSentToNode++;
		}
		
		void incrementPacketsReceivedFromNode () {
			packetsReceivedFromNode++;
		}
		
		void incrementPacketsRelayedToNode () {
			packetsRelayedToNode++;
		}
	}
	
	public StatisticsCollectorAndDisplay(int nodeId) {
		this.packetsSent = 0;
		this.packetsRelayed = 0;
		this.packetsReceived = 0;
		this.sumPacketSent = 0L;
		this.sumPacketReceived = 0L;
		this.nodeID = nodeId;
		
		this.packetsTouched = 0;
		this.packetsPerNode = new TreeMap<Integer, NodeCollector>();
	}
	
	public StatisticsCollectorAndDisplay (OverlayNodeReportsTrafficSummary o) {
		this.packetsSent = o.getPacketsSent();
		this.packetsRelayed = o.getPacketsRelayed();
		this.packetsReceived = o.getPacketsReceived();
		this.sumPacketSent = o.getSumSent();
		this.sumPacketReceived = o.getSumRecv();
		this.nodeID = o.getId();
	}
	
	/**
	 * Constructor for rebuilding from a OverlayNodeReportsTrafficSummary
	 * @param packetsSent
	 * @param packetsRelayed
	 * @param packetsReceived
	 * @param sumPacketSent
	 * @param sumPacketReceived
	 */
	public StatisticsCollectorAndDisplay(int packetsSent, int packetsRelayed, int packetsReceived, 
			long sumPacketSent, long sumPacketReceived, int nodeID) 
	{
		this.packetsSent = packetsSent;
		this.packetsRelayed = packetsRelayed;
		this.packetsReceived = packetsReceived;
		this.sumPacketSent = sumPacketSent;
		this.sumPacketReceived = sumPacketReceived;
		this.nodeID = nodeID;
	}
	
	public static void DisplayStatistics (List<StatisticsCollectorAndDisplay> statList) {
		long totalPacketsSent = 0L;
		long totalPacketsRec  = 0L;
		long totalRelayed     = 0L;
		long totalSumSent     = 0L;
		long totalSumReceived = 0L;
		System.out.printf("         \t%8s\t%8s\t%10s\t%14s\t%14s\n", 
				"Sent", "Received", "Relayed", "Sum Sent", "Sum Received");
		for (StatisticsCollectorAndDisplay n : statList) {
			System.out.printf("Node: %2d\t%8d\t%8d\t%10d\t%14d\t%14d\n", 
					n.nodeID, n.packetsSent, n.packetsReceived,
					n.packetsRelayed, n.sumPacketSent, n.sumPacketReceived);
			totalPacketsSent += n.packetsSent;
			totalPacketsRec  += n.packetsReceived;
			totalRelayed     += n.packetsRelayed;
			totalSumSent     += n.sumPacketSent;
			totalSumReceived += n.sumPacketReceived;
		}
		System.out.printf("Sum:    \t%8d\t%8d\t%10d\t%14d\t%14d\n", 
				totalPacketsSent, totalPacketsRec, totalRelayed, totalSumSent, totalSumReceived);
	}
	
	/**
	 * Increment methods acquire a lock on the counters instead of the object.
	 * This should allow multiple threads to increment their specific counter without locking access to others.
	 */

	public synchronized void incrementPacketsSent() {
		packetsSent++;
	}
	
	public synchronized void incrementPacketsRelayed() {
		packetsRelayed++;
	}
	
	public synchronized void incrementPacketsReceived() {
		packetsReceived++;
	}
	
	public synchronized void addToSentSum (long toAdd) {
		sumPacketSent += toAdd;
	}
	
	public synchronized void addToReceivedSum (long toAdd) {
		sumPacketReceived += toAdd;
	}
	
	public synchronized void incrementPacketsTouched () {
		packetsTouched++;
	}
	
	public synchronized void incrementPacketsReceivedFrom (int nodeID) {
		NodeCollector n = packetsPerNode.get(nodeID);
		if (n == null) {
			n = new NodeCollector();
			packetsPerNode.put(nodeID, n);
		}
		n.incrementPacketsReceivedFromNode();
	}
	
	public synchronized void incrementPacketsSentTo (int nodeID) {
		NodeCollector n = packetsPerNode.get(nodeID);
		if (n == null) {
			n = new NodeCollector();
			packetsPerNode.put(nodeID, n);
		}
		n.incrementPacketsSentToNode();
	}
	
	public synchronized void incrementPacketsRelayedTo (int nodeID) {
		NodeCollector n = packetsPerNode.get(nodeID);
		if (n == null) {
			n = new NodeCollector();
			packetsPerNode.put(nodeID, n);
		}
		n.incrementPacketsRelayedToNode();
	}
	
	public synchronized String extendedToString() {
		String str = this.toString();
		String extendedToStr = String.format("\npacketsTouched=%s\nPackets Per Node:\t    Sent\tReceived\t Relayed\n", packetsTouched);
		int sumSent = 0;
		int sumRec = 0;
		int sumRelayed = 0;
		for (Entry<Integer, NodeCollector> e: packetsPerNode.entrySet()) {
			String nStr = String.format("Node %d:", e.getKey());
			NodeCollector n = e.getValue();
			extendedToStr += String.format("%17s\t%8d\t%8d\t%8d\n", 
					nStr, n.packetsSentToNode, n.packetsReceivedFromNode, n.packetsRelayedToNode);
			sumSent    += n.packetsSentToNode;
			sumRec     += n.packetsReceivedFromNode;
			sumRelayed += n.packetsRelayedToNode;
		}
		String sumStr = "Sums: ";
		extendedToStr += String.format("%17s\t%8d\t%8d\t%8d\n", sumStr, sumSent, sumRec, sumRelayed);
		return str + extendedToStr;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public synchronized String toString() {
		return String.format(
				"StatisticsCollectorAndDisplay:\npacketsSent=%s\npacketsRelayed=%s\npacketsReceived=%s\nsumPacketSent=%s\nsumPacketReceived=%s",
				packetsSent, packetsRelayed, packetsReceived, sumPacketSent, sumPacketReceived);
	}

	/**
	 * @return the packetsSent
	 */
	public synchronized Integer getPacketsSent() {
		return packetsSent;
	}

	/**
	 * @return the packetsRelayed
	 */
	public synchronized Integer getPacketsRelayed() {
		return packetsRelayed;
	}

	/**
	 * @return the packetsReceived
	 */
	public synchronized Integer getPacketsReceived() {
		return packetsReceived;
	}

	/**
	 * @return the sumPacketSent
	 */
	public synchronized Long getSumPacketSent() {
		return sumPacketSent;
	}

	/**
	 * @return the sumPacketReceived
	 */
	public synchronized Long getSumPacketReceived() {
		return sumPacketReceived;
	}

	/**
	 * @return the nodeID
	 */
	public synchronized int getNodeID() {
		return nodeID;
	}
}
