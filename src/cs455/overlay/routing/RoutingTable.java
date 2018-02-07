package cs455.overlay.routing;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

public class RoutingTable {
	private TreeMap<Integer, RoutingTableEntry> routingtable;
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
	
	public RoutingTable (List<RoutingTableEntry> routingTableEntries) {
		routingtable = new TreeMap<Integer, RoutingTableEntry>();
		for (RoutingTableEntry re : routingTableEntries) {
			routingtable.put(re.getId(), re);
		}
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
		tableEntry.setId(id);
		routingtable.put(id, tableEntry);
		// Return the id of the registered node
		return id;
	}
	
	public synchronized void remove (int id) {
		routingtable.remove(id);
	}
	
	/**
	 * Synchronized because if several threads call at once it may report the same size
	 * @return the number of elements in the routing table
	 */
	public synchronized int size() {
		return routingtable.size();
	}
	
	public void printManifest () {
		if (routingtable.isEmpty()) {
			System.out.println("No nodes are present in the manifest");
			return;
		}
		for (Map.Entry<Integer, RoutingTableEntry> r : routingtable.entrySet()) {
			int key = r.getKey();
			RoutingTableEntry entry = r.getValue();
			System.out.println("Entry id: " + key + ", IP: " + entry.getIp() + ", Port: " + entry.getPort());
		}
	}
	
	public RoutingTableEntry getEntryByID(int id) {
		return routingtable.get(id);
	}
	
	/**
	 * Returns the routing table for a given node using the following algorithm:
	 * Let k be the index of the given id in a sorted list of node ids
	 * And let subtable_size be Nr or the size of each routing table
	 * For i <= subtable_size
	 * 	Entry at i is 2**i-1 hops away from k
	 * @param id - the id of the node we are constructing the routing table for
	 * @param subtableSize - the routing table size
	 * @return a list containing the nodes routing table
	 */
	public List<RoutingTableEntry> getSubTable(int id, int subtableSize) {
		ArrayList<Integer> nodeIDs = new ArrayList<Integer>(routingtable.keySet());
		ArrayList<RoutingTableEntry> nodesRoutingTable = new ArrayList<RoutingTableEntry>(subtableSize);
		int setSize = nodeIDs.size();
		int kStartIndex = nodeIDs.indexOf(id);
		for (int i = 1; i <= subtableSize; i++) {
			int numHops = (int) Math.pow(2, (i-1));		// 2**i-1 hops
			// Go numHops from the start point, and wrap around
			int indexOfNextEntry = (kStartIndex + numHops) % setSize; 
			// Get the id at numhops away from start
			int idOfNextEntry = nodeIDs.get(indexOfNextEntry);
			// Add this entry to the nodes routing table
			nodesRoutingTable.add(routingtable.get(idOfNextEntry));
		}
		return nodesRoutingTable;
	}
	
	/**
	 * @return a sorted array of all the keys in the routing table
	 */
	public Integer[] keyManifest() {
		return routingtable.keySet().toArray(new Integer[routingtable.size()]);
	}
	
	/**
	 * Gets the route to the closest nodes for given id.
	 *  - If the node you are looking for is not in the routing table then
	 *  	- Select the highest value (node id) node less than the given node id
	 *  		- If this is not possible (there is no node less than the given node), 
	 *  			then select the greatest value node
	 * @return the id of the next node in the route
	 */
	public int getClosestNode(int nodeToFind) {
		// Returns the greatest key less than or equal to the given key, or null if there is no such key.
		Integer key = routingtable.floorKey(nodeToFind);
		if (key != null) {
			// Will return either the node to find, 
			//	or the next greatest key less than that node
			return key;
		} else {
			// If no such key is possible (there is no node less than the given node id)
			// 	select the greatest key in the table
			return routingtable.lastKey();
		}
	}

	public void resetNodes() {
		for (Map.Entry<Integer, RoutingTableEntry> r : routingtable.entrySet()) {
			RoutingTableEntry entry = r.getValue();
			entry.resetIsFinished();
		}
	}
}
