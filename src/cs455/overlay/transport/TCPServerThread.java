package cs455.overlay.transport;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;

import cs455.overlay.node.Node;
import cs455.overlay.wireframes.Event;

public class TCPServerThread implements Runnable {
	private ServerSocket serversocket;
	private Node master;
	private TCPConnectionsCache activeConnections;
	private AtomicBoolean isRunning;
	
	public TCPServerThread (Node listeningNode) throws IOException {
		master = listeningNode;
		activeConnections = new TCPConnectionsCache();
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
				System.out.println("New connection received from: " + socket.getInetAddress().getHostAddress());
				activeConnections.add(newConnection);
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
			for (TCPConnection c : activeConnections) {
				c.close();
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
		int index = activeConnections.getConnectionIndexByID(tconn.getID());
		TCPConnection c = activeConnections.get(index);
		activeConnections.remove(index);
		c.close();
		return;
	}
	
	public void onEvent (Event e) {
		master.onEvent(e);
	}
	
}
