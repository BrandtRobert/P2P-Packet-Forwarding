package cs455.overlay.node;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

import cs455.overlay.transport.TCPConnection;
import cs455.overlay.transport.TCPServerThread;
import cs455.overlay.wireframes.Event;
import cs455.overlay.wireframes.OverlayNodeSendsDeregistration;
import cs455.overlay.wireframes.OverlayNodeSendsRegistration;
import cs455.overlay.wireframes.RegistryReportsDeregistrationStatus;
import cs455.overlay.wireframes.RegistryReportsRegistrationStatus;

public class MessagingNode implements Node {
	private TCPServerThread server;
	private Thread serverThread;
	private int registeredId;
	private TCPConnection registry;
	
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
			Socket socket = new Socket (registryAddr, port);
			messagingNode.registry = new TCPConnection(socket, messagingNode.server);
			messagingNode.sendRegistrationRequest(messagingNode.server.getPort());
		} catch (UnknownHostException e) {
			System.err.println("Unable to find host IP");
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
			}
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

	@Override
	public void onEvent(Event event) {
		switch (event.getType()) {
			case REGISTRY_REPORTS_REGISTRATION_STATUS:
				reportRegistration((RegistryReportsRegistrationStatus) event);
				break;
			case REGISTRY_REPORTS_DEREGISTRATION_STATUS:
				reportDeregistration((RegistryReportsDeregistrationStatus) event);
				break;
			default:
				System.out.println(event.getBytes());
				break;
		}
	}
}
