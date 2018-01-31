package cs455.overlay.transport;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

import cs455.overlay.node.Node;
import cs455.overlay.wireframes.Event;

public class TCPServerThread implements Runnable {
	private ServerSocket serversocket;
	private Node master;
	private TCPConnectionsCache activeConnections;
	private AtomicBoolean isRunning;
	private Map<Integer, TCPConnection> tempConnections;
	private int tempIDCounter = -1;
	
	public TCPServerThread (Node listeningNode) throws IOException {
		master = listeningNode;
		activeConnections = new TCPConnectionsCache();
		tempConnections = new TreeMap<Integer, TCPConnection>();
		isRunning = new AtomicBoolean(true);
		// A port of 0, chooses a randomly available port
		serversocket = new ServerSocket(0);
		printRunningServer();
	}
	
	@Override
	public void run() {
		try {
			while (isRunning.get()) {
				Socket socket = serversocket.accept();
				TCPConnection newConnection = new TCPConnection(socket, this);
				newConnection.setID(tempIDCounter);
				tempConnections.put(tempIDCounter--, newConnection);
				System.out.println("New connection received from: " + socket.getInetAddress().getHostAddress());
			}
		} catch (SocketException e) {
			System.err.println("Server Socket closed");
			return;
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}
	
	private void cleanup() {
		try {
			serversocket.close();
			// Close and remove all active collections
			for (int i : activeConnections.keySet()) {
				activeConnections.get(i).close();
			}
			activeConnections.clear();
		} catch (IOException e) {
			System.err.println("Failure in closing socket");
			e.printStackTrace();
		}
	}
	
	public void kill () {
		try {
			isRunning.set(false);
			serversocket.close();
			cleanup();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void printRunningServer() throws UnknownHostException {
		InetAddress localhost = InetAddress.getLocalHost();
		System.out.printf("Server running on: %s,\n\tPort: %s\n", localhost.getHostAddress(), serversocket.getLocalPort());
	}
	
	public int getPort() {
		return serversocket.getLocalPort();
	}
	
	public void closeConnection(TCPConnection tconn) {
		// Negative ids are for the temp space
		if (tconn.getID() < 0) {
			tempConnections.remove(tconn.getID());
		} else {
			// All other ids are for the connections cache
			activeConnections.removeConnectionFromCache(tconn.getID());
		}
		tconn.close();
		return;
	}
	
	public void onEvent (Event e) {
		master.onEvent(e);
	}
	
	public synchronized void addConnectionToCache (int id, TCPConnection tconn) {
		// Remove from the temporary connections cache
		tempConnections.remove(tconn.getID());
		// Add to the primary connections cache
		tconn.setID(id);
		activeConnections.addConnectionToCache(id, tconn);
	}
	
	public TCPConnection getConnectionFromCache (int id) {
		return activeConnections.getConnectionById(id);
	}
	
	public void listCacheConnections () {
		System.out.println("All active connections: ");
		for (Map.Entry<Integer, TCPConnection> c : activeConnections.entrySet()) {
			int key = c.getKey();
			TCPConnection entry = c.getValue();
			System.out.println("\tEntry id: " + key + ", IP: " + entry.getSocketIP() + ", Port: " + entry.getRemotePort());
		}
	}
	
	public TCPConnectionsCache getActiveConnections() {
		return this.activeConnections;
	}
}
