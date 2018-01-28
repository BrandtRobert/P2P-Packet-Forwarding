package cs455.overlay.node;
import java.io.IOException;
import java.lang.Thread;
import java.net.InetAddress;
import java.util.Scanner;

import cs455.overlay.routing.RoutingTable;
import cs455.overlay.routing.RoutingTable.RoutingTableException;
import cs455.overlay.routing.RoutingTableEntry;
import cs455.overlay.transport.TCPServerThread;
import cs455.overlay.wireframes.*;

public class Registry implements Node {
	private TCPServerThread server;
	private Thread serverThread;
	private RoutingTable routingTable;
	
	public Registry () {
		routingTable = new RoutingTable();
	}
	
	public static void main (String [] args) {
		Registry registry = new Registry();
		registry.runServer();
		// Open reader from the user
		Scanner sysin = new Scanner (System.in);
		while (true) {
			System.out.println("Enter a command: ");
			String input = sysin.next();
			System.out.println("Command received: " + input);
			if (input.startsWith("q")) {
				registry.tearDown();
				sysin.close();
				System.out.println("System Exiting...");
				System.exit(0);
			}
			if (input.equals("list-messaging-nodes")) {
				registry.routingTable.printManifest();
			}
		}
	}
	
	@Override
	public void onEvent(Event event) {
		Protocol protocol = event.getType();
		switch (protocol) {
			case OVERLAY_NODE_SENDS_REGISTRATION:
				registerNode((OverlayNodeSendsRegistration) event);
				break;
			case OVERLAY_NODE_SENDS_DEREGISTRATION:
				deregisterNode((OverlayNodeSendsDeregistration) event);
				break;
			// Do nothing in the default case
			default:
				String msg = new String (event.getBytes());
				System.out.println(msg);
				break;
		}
	}
	
	/**
	 * Removes a node from the registry.
	 * @param event - the registration wireframe event
	 * @return the id of the registering node
	 */
	private void deregisterNode(OverlayNodeSendsDeregistration event) {
		InetAddress ip = event.getInetAddress();
		InetAddress socketIp = event.getResponseConnection().getSocketIP();
		int portnum = event.getPort();
		int registeredId = event.getRegisteredID();
		String responseStr = "";
		// Verify the ip address given matches the connection
		if (!ip.equals(socketIp)) {
			responseStr = String.format("Given IP address: %s, does not match socket IP: %s\n", ip, socketIp);
		} else {
			RoutingTableEntry re = routingTable.getEntryByID(registeredId);
			// Verify that the entry's matches the one in the routing table
			if (re.getIp().equals(socketIp) && re.getPort() == portnum) {
				routingTable.remove(registeredId);
				responseStr = String.
						format("Deregistration request successful, there are now (%d) nodes in the overlay.", routingTable.size()); 
			} else {
				// If the entry is unverified, do not honor the request
				responseStr = String.format("Request IP: %s, and ID: %d do not match routing table entry", ip, registeredId);
				registeredId = -1;
			}
		}
		// Craft the response message
		RegistryReportsDeregistrationStatus response = 
				new RegistryReportsDeregistrationStatus(registeredId, responseStr);
		// Send the response message back to the client
		event.getResponseConnection().sendMessage(response.getBytes());
	}
	
	/**
	 * Records a node in the registry.
	 * @param event - the registration wireframe event
	 * @return the id of the registering node
	 */
	private int registerNode (OverlayNodeSendsRegistration event) {
		InetAddress ip = event.getInetAddress();
		InetAddress socketIp = event.getResponseConnection().getSocketIP();
		int portnum = event.getPort();
		int registeredId = -1;
		String responseStr = "";
		// Verify the ip address given matches the connection
		if (!ip.equals(socketIp)) {
			responseStr = String.format("Given IP address: %s, does not match socket IP: %s\n", ip, socketIp);
		} else {
			try {
				// Try to add the node to the registry
				registeredId = routingTable.add(ip, portnum);
				// If there is not exception than registration was successful
				responseStr = String.format("Registration successful, "
						+ "there are currently %d nodes in the overlay\n", routingTable.size());
				// Add this connection to the connections cache
				server.addConnectionToCache(registeredId, event.getResponseConnection());
			} catch (RoutingTableException e) {
				// Routing table exception occurs if the table is full, or the entry is duplicate
				responseStr = e.getMessage();
			}
		}
		// Craft the response message
		RegistryReportsRegistrationStatus response = 
				new RegistryReportsRegistrationStatus(registeredId, responseStr);
		// Send the response message back to the client
		if (event.getResponseConnection().isAlive()) {
			event.getResponseConnection().sendMessage(response.getBytes());
			return registeredId;
		} else {
			// If the client died after registration request, then remove them from the registry
			routingTable.remove(registeredId);
			return -1;
		}
	}
	
	/**
	 * Tears down the server, stops the thread, and closes all connections.
	 * @throws InterruptedException 
	 */
	private void tearDown() {
		try {
			server.kill();
			serverThread.join();
		} catch (InterruptedException e) {
			System.err.println("Interrupt while joining server thread");
			e.printStackTrace();
		}
	}
	
	/**
	 * Quits and exits the program
	 */
	private void quit() {
		tearDown();
		System.exit(1);
	}
	
	/**
	 * Starts the server, and keeps it running.
	 */
	private void runServer() {
		try {
			server = new TCPServerThread(this);
		} catch (IOException e) {
			System.err.println("Failed to start server!");
			quit();
		}
		serverThread = new Thread (server, "Server-Thread");
		serverThread.start();
		return;
	}
}
