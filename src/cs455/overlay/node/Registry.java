package cs455.overlay.node;
import java.io.IOException;
import java.lang.Thread;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import cs455.overlay.routing.RoutingTable;
import cs455.overlay.routing.RoutingTable.RoutingTableException;
import cs455.overlay.routing.RoutingTableEntry;
import cs455.overlay.transport.TCPConnection;
import cs455.overlay.transport.TCPServerThread;
import cs455.overlay.util.StatisticsCollectorAndDisplay;
import cs455.overlay.wireframes.*;

public class Registry implements Node {
	private TCPServerThread server;
	private Thread serverThread;
	private RoutingTable routingTable;
	private int nodeTableSize;
	private boolean setupOverlayComplete = false;
	private Integer finishedNodes;
	private Integer trafficSummaryCount;
	private List<StatisticsCollectorAndDisplay> statistics;
	
	private int seconds = 0;
	
	public Registry () {
		routingTable = new RoutingTable();
		finishedNodes = 0;
		trafficSummaryCount = 0;
		statistics = new ArrayList<StatisticsCollectorAndDisplay>();
	}
	
	public static void main (String [] args) {
		Registry registry = new Registry();
		int listeningPort = (args.length == 1) ? Integer.parseInt(args[0]) : 0;
		registry.runServer(listeningPort);
		// Delay to place before sending out traffic summary requests
		registry.seconds = (args.length == 2) ? Integer.parseInt(args[1]) : 0;
		// Open reader from the user
		Scanner sysin = new Scanner (System.in);
		while (true) {
			System.out.println("Enter a command: ");
			String input = sysin.nextLine();
			String [] splits = input.split("\\s+");
			System.out.println("Command received: " + input);
			if (splits[0].startsWith("q")) {
				registry.tearDown();
				sysin.close();
				System.out.println("System Exiting...");
				System.exit(0);
			} else if (splits[0].equals("list-messaging-nodes")) {
				registry.routingTable.printManifest();
			} else if (splits[0].equals("setup-overlay")) {
				// If a table size is specified use that, otherwise the default is 3
				registry.nodeTableSize = (splits.length > 1) ? Integer.parseInt(splits[1]): 3;
				registry.setupOverlay(false);
			} else if (splits[0].equals("list-routing-tables")) {
				registry.listRoutingTables();
			} else if (splits[0].equals("start")) {
				if (registry.setupOverlayComplete) {
					int numPackets = Integer.parseInt(splits[1]);
					registry.sendTaskInitiate(numPackets);
				} else {
					System.err.println("Unable to initiate message sending without overlay setup");
				}
			}
		}
	}
	
	private void listRoutingTables() {
		routingTable.printRoutingTables(this.nodeTableSize);
	}

	/**
	 * Sends task initiate
	 * @param numPackets
	 */
	private void sendTaskInitiate(int numPackets) {
		// Send a task initiate to all nodes
		for (Map.Entry<Integer, TCPConnection> entry : server.getActiveConnections().entrySet()) {
			TCPConnection conn = entry.getValue();
			RegistryRequestsTaskInitiate taskInit = new RegistryRequestsTaskInitiate(numPackets);
			conn.sendMessage(taskInit.getBytes());
		}
	}

	/**
	 * Dispatches routing tables to all messaging nodes in the manifest
	 */
	private void setupOverlay(boolean printTable) {
		Integer[] keyManifest = routingTable.keyManifest();
		// If 2**k-1 % tablesize == 0, then a node will select itself in the routing subtable
		// Not an issue if tablesize > 2 * k
		if (2*this.nodeTableSize >= keyManifest.length && this.nodeTableSize > 1) {
			System.err.println("Routing table size too large for number of registered nodes, will not setup overlay");
			return;
		}
		// Dispatch routing tables
		for (int i = 0; i < keyManifest.length; i++) {
			int keyID = keyManifest[i];
			List<RoutingTableEntry> subtable = routingTable.getSubTable(keyID, this.nodeTableSize);
			// If print table only print entries
			if (printTable) {
				RoutingTableEntry keyEntry = routingTable.getEntryByID(keyID);
				System.out.println("Node: " + keyEntry.getId() + ", IP:" + keyEntry.getIp());
				// Print routing table for now
				for (RoutingTableEntry r : subtable) {
					System.out.println("\tNode: " + r.getId() + ", IP: " + r.getIp() + ", Port: " + r.getPort());
				}
			// Otherwise dispatch tables to their respective nodes
			} else {
				// Send the routing tables to the respective nodes
				TCPConnection currConnection = server.getConnectionFromCache(keyID);
				RegistrySendsNodeManifest tableToSend = new RegistrySendsNodeManifest(subtable, keyManifest);
				currConnection.sendMessage(tableToSend.getBytes());
			}
		}
		this.setupOverlayComplete = true;
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
			case NODE_REPORTS_OVERLAY_SETUP_STATUS:
				NodeReportsOverlaySetupStatus n = (NodeReportsOverlaySetupStatus) event;
				System.out.println(n.getInfoString());
				break;
			case OVERLAY_NODE_REPORTS_TASK_FINISHED:
				reportNodeTaskFinish((OverlayNodeReportsTaskFinished) event);
				break;
			case OVERLAY_NODE_REPORTS_TRAFFIC_SUMMARY:
				reportNodeTrafficSummary((OverlayNodeReportsTrafficSummary) event);
				break;
			// Do nothing in the default case
			default:
				String msg = new String (event.getBytes());
				System.out.println(msg);
				break;
		}
	}
	
	/**
	 * Record the node's traffic summary.
	 */
	private void reportNodeTrafficSummary(OverlayNodeReportsTrafficSummary event) {
		int nodeId = event.getId();
		RoutingTableEntry entry = routingTable.getEntryByID(nodeId);
		if (!entry.getIp().equals(event.getResponseConnection().getSocketIP())) {
			System.err.println("Cannot verify node ip for traffic summary");
			return;
		}
		StatisticsCollectorAndDisplay nodeState = new StatisticsCollectorAndDisplay(event);
		synchronized (statistics) {
			statistics.add(nodeState);
		}
		synchronized (trafficSummaryCount) {
			trafficSummaryCount++;
			if (trafficSummaryCount == routingTable.size()) {
				System.out.println("All traffic summaries received");
				trafficSummaryCount = 0;
				StatisticsCollectorAndDisplay.DisplayStatistics(statistics);
				statistics.clear();
			}
		}
	}

	/**
	 * Record the node as finished.
	 */
	private void reportNodeTaskFinish(OverlayNodeReportsTaskFinished event) {
		// Validate IP
		InetAddress incomingAddr = event.getResponseConnection().getSocketIP();
		if (!incomingAddr.equals(event.getIP())) {
			System.err.println("Given IP does not match socket connection");
			return;
		}
		// Verify the node in the routing table
		RoutingTableEntry entry = routingTable.getEntryByID(event.getID());
		if (!entry.getIp().equals(entry.getIp()) || entry.getPort() != event.getPort()) {
			System.err.println("Cannot verify node entry in the registry");
			return;
		}
		// Set the entry as finished
		if (!entry.isFinished()) {
			entry.setIsFinished();
			synchronized (finishedNodes) {
				finishedNodes++;
				// If all the nodes in the registry are finished
				// Send out traffic summary requests
				if (finishedNodes == routingTable.size()) {
					finishedNodes = 0;
					routingTable.resetNodes();
				// For testing purposes ??
				try {
					Thread.sleep(seconds * 1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
					sendTrafficSummaryRequests();
				}
			}
		}
	}

	/**
	 * Sends requests for traffic summaries to every node.
	 */
	private void sendTrafficSummaryRequests() {
		// Send a task initiate to all nodes
		for (Map.Entry<Integer, TCPConnection> entry : server.getActiveConnections().entrySet()) {
			TCPConnection conn = entry.getValue();
			RegistryRequestsTrafficSummary tSummary = new RegistryRequestsTrafficSummary();
			conn.sendMessage(tSummary.getBytes());
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
	 * @param listeningPort 
	 */
	private void runServer(int listeningPort) {
		try {
			server = new TCPServerThread(this, listeningPort);
		} catch (IOException e) {
			System.err.println("Failed to start server, you may be using a reserved port!");
			System.exit(1);
		}
		serverThread = new Thread (server, "Server-Thread");
		serverThread.start();
		return;
	}
}
