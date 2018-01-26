package cs455.overlay.transport;

import java.util.ArrayList;

/**
 * A collection of TCP connections created by the TCP Server Thread.
 * When a new connection is formed it is added to the collection of connections.
 * @author Brandt Reutimann
 */
@SuppressWarnings("serial")
public class TCPConnectionsCache extends ArrayList<TCPConnection> {
	/**
	 * Use a linear search to find the connection
	 * @param uuid - the uuid of the connection being searched for.
	 * @return
	 */
	public int getConnectionIndexByID(String uuid) {
		for (int i = 0; i < size(); i++) {
			TCPConnection c = get(i);
			if (c.getID().equals(uuid)) {
				return i;
			}
		}
		return -1;
	}
}
