package cs455.overlay.routing;

import java.net.InetAddress;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

public class RoutingTable {
	private Map<Integer, RoutingTableEntry> routingtable;
	private final int MAX_TABLE_SIZE = 128;		// The size of the node manifest id space
	
	@SuppressWarnings("serial")
	public class RoutingTableException extends Exception {
		public RoutingTableException (String message) {
			super (message);
		}
	}
	
	public RoutingTable () {
		routingtable = new TreeMap<Integer, RoutingTableEntry>();
	}
	
	/**
	 * Adds a new node to the routing table, randomly assigns an id.
	 * @param ip - ip of the registering node
	 * @param portnum - port num of the registering node
	 */
	public synchronized int add (InetAddress ip, int portnum) throws RoutingTableException {
		Random random = new Random();
		// Create a new entry
		RoutingTableEntry tableEntry = new RoutingTableEntry(ip, portnum);
		// This is a duplicate entry in the routing table
		if (routingtable.containsValue(tableEntry)) {
			String duplicateEntryErr = String.format("IP address: %s, and Port Number: %d are already registered\n", ip, portnum);
			throw new RoutingTableException (duplicateEntryErr);
		}
		// Routing table is full
		if (routingtable.size() == MAX_TABLE_SIZE) {
			throw new RoutingTableException("Cannot register new entries, routing table is full\n");
		}
		// Find an id that isn't currently in the table
		int id = random.nextInt(MAX_TABLE_SIZE);
		while (routingtable.containsKey(id)) {
			id = random.nextInt(MAX_TABLE_SIZE); 
		}
		// Add to the routing table
		routingtable.put(id, tableEntry);
		// Return the id of the registered node
		return id;
	}
	
	public synchronized void remove (int id) {
		
	}
	
	public int size() {
		return routingtable.size();
	}
	
	public void printManifest () {
		for (Map.Entry<Integer, RoutingTableEntry> r : routingtable.entrySet()) {
			int key = r.getKey();
			RoutingTableEntry entry = r.getValue();
			System.out.println("Entry id: " + key + ", IP: " + entry.getIp() + ", Port: " + entry.getPort());
		}
	}
}
