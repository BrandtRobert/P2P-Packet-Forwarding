package cs455.overlay.node;

import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Scanner;

import cs455.overlay.transport.TCPServerThread;
import cs455.overlay.wireframes.Event;
import cs455.overlay.wireframes.OverlayNodeSendsRegistration;
import cs455.overlay.wireframes.Protocol;
import cs455.overlay.wireframes.RegistryReportsRegistrationStatus;

public class MessagingNode implements Node {
	private TCPServerThread server;
	private Thread serverThread;
	private int registeredId;
	
	private static void usage() {
		System.out.println("java MessagingNode <server> <port>");
		System.exit(1);
	}
	
	public static void main (String args []) {
		if (args.length != 2) {
			usage();
		}
		MessagingNode messagingNode = new MessagingNode();
		// Parse the ip and port
		InetAddress registryAddr = null;
		int port = 0;
		try {
			registryAddr = InetAddress.getByName(args[0]);
			port = Integer.parseInt(args[1]);
			messagingNode.runServer();
			messagingNode.registeredId = 
					messagingNode.registerNode(registryAddr, port, messagingNode.server.getPort());
			if (messagingNode.registeredId == -1) {
				messagingNode.quit();
			}
		} catch (UnknownHostException e) {
			System.err.println("Unable to find host IP");
			return;
		}
		// Open reader from the user
		Scanner sysin = new Scanner (System.in);
		while (true) {
			System.out.println("Enter a command: ");
			String input = sysin.next();
			System.out.println("Command received: " + input);
			if (input.startsWith("q")) {
				sysin.close();
				System.out.println("System Exiting...");
				messagingNode.quit();
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
	
	private int registerNode (InetAddress ip, int registryPort, int hostPort) {
		try {
			Socket registry = new Socket (ip, registryPort);
			InetAddress localhost = InetAddress.getLocalHost();
			// Attempt to register node with registry
			OverlayNodeSendsRegistration o = new OverlayNodeSendsRegistration(localhost, hostPort);
			DataOutputStream dout = new DataOutputStream(registry.getOutputStream());
			byte[] toWrite = o.getBytes();
			dout.writeInt(toWrite.length);
			dout.write(toWrite);
			// Read server response
			DataInputStream din = new DataInputStream(registry.getInputStream());
			int responseLen = din.readInt();
			byte [] responseMsg = new byte [responseLen];
			din.read(responseMsg, 0, responseLen);
			// Close the connection to the registry
			registry.close();
			// Unmarshal the response and check for registration success
			if (responseMsg[0] == Protocol.REGISTRY_REPORTS_REGISTRATION_STATUS.getValue()) {
				RegistryReportsRegistrationStatus r = new RegistryReportsRegistrationStatus(responseMsg, null);
				int registerID = r.getSuccessStatus();
				System.out.println(r.getInfoString());
				return registerID;
			} else {
				quit();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return -1;
	}

	@Override
	public void onEvent(Event event) {
		switch (event.getType()) {
			case REGISTRY_REPORTS_REGISTRATION_STATUS:
				RegistryReportsRegistrationStatus r = (RegistryReportsRegistrationStatus) event;
				registeredId = r.getSuccessStatus();
				// If registration failed shut the system down
				if (registeredId == -1) {
					System.err.println("Registration failed: " + r.getInfoString());
					tearDown();
					System.exit(0);
				} else {
					System.out.println(r.getInfoString());
				}
				break;
			default:
				System.out.println(event.getBytes());
				break;
		}
	}
}
