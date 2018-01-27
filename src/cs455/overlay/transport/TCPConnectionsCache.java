package cs455.overlay.transport;

import java.util.Map;
import java.util.TreeMap;

/**
 * A collection of TCP connections created by the TCP Server Thread.
 * When a new connection is formed it is added to the collection of connections.
 * @author Brandt Reutimann
 */
@SuppressWarnings("serial")
public class TCPConnectionsCache extends TreeMap<Integer, TCPConnection> {
	
	public synchronized int addConnectionToCache (int id, TCPConnection conn) {
		// Two connections with the same id not allowed
		if (get(id) != null) {
			return -1;
		} else {
			put(id, conn);
			return id;
		}
	}
	
	public synchronized int removeConnectionFromCache (int id) {
		if (get(id) == null) {
			return -1;
		} else {
			remove(id);
			return id;
		}
	}
}
