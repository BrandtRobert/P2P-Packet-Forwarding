package cs455.overlay.node;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

import cs455.overlay.routing.RoutingTable;
import cs455.overlay.routing.RoutingTableEntry;
import cs455.overlay.transport.TCPConnection;
import cs455.overlay.transport.TCPServerThread;
import cs455.overlay.util.StatisticsCollectorAndDisplay;
import cs455.overlay.wireframes.Event;
import cs455.overlay.wireframes.NodeReportsOverlaySetupStatus;
import cs455.overlay.wireframes.OverlayNodeReportsTaskFinished;
import cs455.overlay.wireframes.OverlayNodeReportsTrafficSummary;
import cs455.overlay.wireframes.OverlayNodeSendsData;
import cs455.overlay.wireframes.OverlayNodeSendsDeregistration;
import cs455.overlay.wireframes.OverlayNodeSendsRegistration;
import cs455.overlay.wireframes.RegistryReportsDeregistrationStatus;
import cs455.overlay.wireframes.RegistryReportsRegistrationStatus;
import cs455.overlay.wireframes.RegistryRequestsTaskInitiate;
import cs455.overlay.wireframes.RegistrySendsNodeManifest;

public class MessagingNode implements Node {
	private TCPServerThread server;
	private Thread serverThread;
	private int registeredId;
	private TCPConnection registry;
	private Integer [] nodeManifest;
	private RoutingTable routingtable;
	private StatisticsCollectorAndDisplay statistics;
	private AtomicBoolean newDataReceived = new AtomicBoolean(false);
	
	private static void usage() {
		System.out.println("java MessagingNode <server> <port>");
		System.exit(1);
	}
	
	public static void main (String args []) {
		if (args.length != 2) {
			usage();
		}
		MessagingNode messagingNode = new MessagingNode();
		/*
		 * Attempt to connect to host and register messenger with the host.
		 */
		try {
			// Parse IP and port num
			InetAddress registryAddr = InetAddress.getByName(args[0]);
			int port = Integer.parseInt(args[1]);
			// You must open a server before you register, since the host needs the server port
			messagingNode.runServer();
			// Attemp to connect to the server
			InetSocketAddress sockAddr = new InetSocketAddress(registryAddr, port);
			Socket socket = new Socket();
			// Set timeout on socket
			socket.connect(sockAddr, 5000);
			
			messagingNode.registry = new TCPConnection(socket, messagingNode.server);
			messagingNode.sendRegistrationRequest(messagingNode.server.getPort());
		} catch (UnknownHostException e) {
			System.err.println("Unable to find host IP");
			return;
		} catch (SocketTimeoutException e) {
			System.err.println("MessagingNode failed to connect to server on socket timeout, please check IP and Port");
			return;
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		// Scan for user input
		Scanner sysin = new Scanner (System.in);
		while (true) {
			System.out.println("Enter a command: ");
			String input = sysin.next();
			System.out.println("Command received: " + input);
			if (input.startsWith("q")) {
				sysin.close();
				System.out.println("System Exiting...");
				messagingNode.quit();
			} else if (input.equals("exit-overlay")) {
				messagingNode.sendDegistrationRequest();
			} else if (input.equals("stats")) {
				System.out.println(messagingNode.statistics);
			} else if (input.equals("print-counters-and-diagnostics")) {
				messagingNode.printDiagnosticInfo();
			}
		}
	}
	
	/**
	 * Prints info about current state of the node
	 */
	private void printDiagnosticInfo() {
		System.out.println(statistics.extendedToString());
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
	
	/**
	 * Sends a deregistration request to the server.
	 */
	private void sendDegistrationRequest() {
		InetAddress localhost;
		try {
			localhost = InetAddress.getLocalHost();
			int port = server.getPort();
			OverlayNodeSendsDeregistration o = new OverlayNodeSendsDeregistration(localhost, port, registeredId);
			registry.sendMessage(o.getBytes());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Send node's information to the registry.
	 * @param ip - The registry's IP
	 * @param registryPort - The registry's Port
	 * @param hostPort - The port that this host is listening on
	 * @return
	 */
	private void sendRegistrationRequest (int hostPort) {
		try {
			InetAddress localhost = InetAddress.getLocalHost();
			OverlayNodeSendsRegistration o = new OverlayNodeSendsRegistration(localhost, hostPort);
			registry.sendMessage(o.getBytes());  // Send registration message
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Verify registration status. If registration failure, then shut down the messaging node.
	 */
	private void reportRegistration (RegistryReportsRegistrationStatus r) {
		InetAddress rIp = r.getResponseConnection().getSocketIP();
		registeredId = r.getSuccessStatus();
		statistics = new StatisticsCollectorAndDisplay(registeredId);
		// If the ip didn't come from the registry don't recognize this registration
		if (!registry.getSocketIP().equals(rIp)) {
			System.err.println("Incoming IP doesn't match registry");
			quit();
		// If registration failed shut the system down
		} else if (registeredId == -1) {
			System.err.println("Registration failed: " + r.getInfoString());
			quit();
		} else {
			System.out.println(r.getInfoString());
		}
	}
	
	/**
	 * Reports the results of the deregistration request
	 */
	private void reportDeregistration (RegistryReportsDeregistrationStatus r) {
		InetAddress rIp = r.getResponseConnection().getSocketIP();
		registeredId = r.getSuccessStatus();
		// If the ip didn't come from the registry don't recognize this registration
		if (!registry.getSocketIP().equals(rIp)) {
			System.err.println("Incoming IP doesn't match registry");
		} else if (registeredId == -1) {
			System.err.println("deregistration failed: " + r.getInfoString());
		} else {
			System.out.println(r.getInfoString());
			quit();
		}
	}
	
	/**
	 * Using the given routing table from the registry attempt to connect to other nodes.
	 * Report the result of connection to other nodes to the registry.
	 */
	private void connectToOverlay(RegistrySendsNodeManifest event) {
		System.out.println("Manifest received: " + Arrays.toString(event.getNodeIdManifest()));
		this.nodeManifest = event.getNodeIdManifest();
		this.routingtable = new RoutingTable (event.getRoutingTable());
		int numOfFails = 0;
		for (RoutingTableEntry re : event.getRoutingTable()) {
			try {
				Socket socket = new Socket(re.getIp(), re.getPort());
				server.addConnectionToCache(re.getId(), new TCPConnection(socket, server));
				System.out.println("\tConnected successfully to Node: " + re.getId() + ", IP: " + 
									re.getIp() + ", Port: " + re.getPort());
			} catch (IOException e) {
				System.err.println("\tUnable to connect to Node: " + re.getId() + ", IP: " + 
									re.getIp() + ", Port: " + re.getPort());
				numOfFails++;
			}
		}
		server.listCacheConnections();
		int successStatus;
		String infoString;
		if (numOfFails > 0) {
			// Report failure to registry
			successStatus = -1;
			infoString = String.format("Node: %d, failed to connect to %d nodes", this.registeredId, numOfFails);
		} else {
			// Report success
			successStatus = this.registeredId;
			infoString = String.format("Node %d, successfully connected to %d nodes", successStatus, event.getRoutingTable().size());
		}
		// Send response to server
		NodeReportsOverlaySetupStatus response = new NodeReportsOverlaySetupStatus(successStatus, infoString);
		registry.sendMessage(response.getBytes());
		return;
	}
	
	private void intiateMessageSend (RegistryRequestsTaskInitiate event) {
		int numPackets = event.getNumPackets();
		// Choose a node randomly and send a packet to it
		Random rand = new Random();
		for (int i = 0; i < numPackets; i++) {
			// Choose a random node
			int randIndex = rand.nextInt(nodeManifest.length);
			// This node can't send messages to itself
			while (nodeManifest[randIndex] == this.registeredId) 
				randIndex = rand.nextInt(nodeManifest.length);
			int randomNode = nodeManifest[randIndex];
			// Get the route to that node
			int route = routingtable.getClosestNode(randomNode);
			// Craft a payload, a random number between int max and int min
			int payload = rand.nextInt();
			// Craft the message to send
			OverlayNodeSendsData packet = new OverlayNodeSendsData(randomNode, this.registeredId, payload);
			// Send it
			server.getConnectionFromCache(route).sendMessage(packet.getBytes());
			// Increment statistics
			statistics.incrementPacketsSent();
			statistics.addToSentSum(payload);
			statistics.incrementPacketsSentTo(randomNode);
		}
		// Report task finished
		try {
			// One way to handle the node terminating before it has received all it's messages
			while (newDataReceived.get()) {
				newDataReceived.getAndSet(false);
				// Because we can't use thread.sleep we busy wait
				long start = new Date().getTime();
				while(new Date().getTime() - start < 1500L);
			}
			InetAddress localhost = InetAddress.getLocalHost();
			OverlayNodeReportsTaskFinished tFinished = 
					new OverlayNodeReportsTaskFinished(localhost, server.getPort(), this.registeredId);
			registry.sendMessage(tFinished.getBytes());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
	
	private void onNextPacket(OverlayNodeSendsData event) {
		newDataReceived.getAndSet(true);
		statistics.incrementPacketsTouched();
		int destination = event.getDestID();
		if (destination == this.registeredId) {
			// packet received, increment statistics
			statistics.incrementPacketsReceivedFrom(event.getSrcID());
			statistics.incrementPacketsReceived();
			statistics.addToReceivedSum(event.getPayload());
			return;
		}
		// Forward the packet
		statistics.incrementPacketsRelayed();
		statistics.incrementPacketsRelayedTo(destination);
		int route = routingtable.getClosestNode(destination);
		event.addToDisseminationTrace(this.registeredId);
		server.getConnectionFromCache(route).sendMessage(event.getBytes());
	}
	
	private void sendTrafficSummary() {
		OverlayNodeReportsTrafficSummary summary = new OverlayNodeReportsTrafficSummary(statistics);
		registry.sendMessage(summary.getBytes());
		statistics = new StatisticsCollectorAndDisplay(registeredId);
		System.out.println("Sending traffic summary...");
	}

	@Override
	public void onEvent(Event event) {
		switch (event.getType()) {
			case REGISTRY_REPORTS_REGISTRATION_STATUS:
				reportRegistration((RegistryReportsRegistrationStatus) event);
				break;
			case REGISTRY_REPORTS_DEREGISTRATION_STATUS:
				reportDeregistration((RegistryReportsDeregistrationStatus) event);
				break;
			case REGISTRY_SENDS_NODE_MANIFEST:
				connectToOverlay((RegistrySendsNodeManifest) event);
				break;
			case REGISTRY_REQUESTS_TASK_INITIATE:
				intiateMessageSend((RegistryRequestsTaskInitiate) event); 
				break;
			case OVERLAY_NODE_SENDS_DATA:
				onNextPacket((OverlayNodeSendsData) event);
				break;
			case REGISTRY_REQUESTS_TRAFFIC_SUMMARY:
				sendTrafficSummary();
				break;
			default:
				System.out.println(event.getBytes());
				break;
		}
	}

}
