package cs455.overlay.transport;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.Executors;

import cs455.overlay.wireframes.Event;
import cs455.overlay.wireframes.EventFactory;

/**
 * An abstract implementation of a TCP connection.
 * The connection uses two threads one to send messages, and one to receive messages.
 * In order to send a message it is added to the messageQueue.
 * The sender thread picks messages off the queue and sends them over the socket.
 * @author Brandt Reutimann
 */
public class TCPConnection {
	private Socket socket;
	private TCPServerThread master;	// The master contains an onEvent method that is triggered when a new message is received
	private TCPSenderThread sender;
	private Thread receiver;
	private AtomicBoolean isRunning;
	private int uniqueID = -1;
	
	public TCPConnection (Socket socket, TCPServerThread master) {
		this.socket = socket;
		isRunning = new AtomicBoolean (true);
		this.master = master;
		try {
			sender = new TCPSenderThread();
			receiver = new Thread (new TCPReceiverThread(), "Receiver-Thread");
			receiver.start();
		} 
		catch (IOException e) {
			System.out.println("Connection ended");
			this.close();
		}
	}
	
	public boolean isAlive() {
		return !socket.isClosed();
	}
	
	/**
	 * Adds a new message to the message queue.
	 * These messages will eventually be sent to the receiver.
	 * @param message - a byte string message to send
	 */
	public void sendMessage (byte [] message) {
		try {
			sender.socketWrite(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public InetAddress getSocketIP() {
		return socket.getInetAddress();
	}
	
	public int getRemotePort() {
		return socket.getPort();
	}
	
	public void setID(int i) {
		uniqueID = i;
	}
	
	public int getID() {
		return uniqueID;
	}
	
	/**
	 * Ends the sender and receiver threads, and closes the socket resources.
	 */
	public void close() {
		try {
			// Set the running flag to false and interrupt blocking calls in the sender and receiver threads
			isRunning.set(false);
			socket.close();		// Close the socket connection
		} catch (IOException e) {
			System.err.println("Socket failed to close properly");
			e.printStackTrace();
		} 
	}
	
	/**
	 * Method called by classes for this connection to kill itself.
	 */
	protected void die() {
		master.closeConnection(this);
	}
	
	private class TCPSenderThread implements Runnable {
		private DataOutputStream dout;
		private LinkedBlockingQueue<byte[]> messageQueue;	// A thread safe queue for holding messages
		
		public TCPSenderThread () throws IOException {
			dout = new DataOutputStream(socket.getOutputStream());
			messageQueue = new LinkedBlockingQueue<byte[]>();
		}
		
		/**
		 * While the sender thread is active it grabs messages off the messageQueue and sends them.
		 */
		@Override
		public void run() {
			while (isRunning.get()) {
				try {
					byte[] toSend = messageQueue.poll();
					if (toSend != null && !socket.isClosed()) {
						socketWrite(toSend);
					}
				} catch (IOException e) {
					System.err.println("Message failed to send");
					die();
					return;
				}
			}
		}
		
		public void putOnMessageQueue (byte [] msg) {
			try {
				this.messageQueue.put(msg);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		/**
		 * Sends a message over the socket
		 * @param message - a byte string containing the message pay-load
		 * @throws IOException
		 */
		public synchronized void socketWrite (byte [] message) throws IOException {
			// Review framing protocols for why this is necessary
			// https://blog.stephencleary.com/2009/04/message-framing.html
			int length = message.length;
			dout.writeInt(length);				// Send the message length
			dout.write(message, 0, length);		// Send the message
			dout.flush();
		}
	}
	
	private class TCPReceiverThread implements Runnable {
		DataInputStream din;
		ExecutorService threadPool;
		
		public TCPReceiverThread() throws IOException {
			din = new DataInputStream(socket.getInputStream());
			threadPool = Executors.newFixedThreadPool (100);
		}

		/**
		 * While the receiver thread is running it receives new messages 
		 * 	and sends events to its master.
		 */
		@Override
		public void run() {
			while (isRunning.get() && !socket.isClosed()) {
				try {
					int dataLength = din.readInt();
					byte[] data = new byte[dataLength];
					din.readFully(data, 0, dataLength);
					Event event = EventFactory.convertBytesToEvent(data, TCPConnection.this);
					// Handle the event asynchronously, this syntax requires java 8
					// The thread pool architecture prevents you from thread bombing the system,
					// 		and eating all the memory.
					threadPool.submit(()->{
						master.onEvent(event);
					});
				} catch (SocketException e) {
					System.err.println("Socket connection closed: " + getSocketIP());
					die();
					return;
				} catch (IOException e) {
					System.err.println("Socket connection closed: " + getSocketIP());
					die();
					return;
				}
			}
		}
	}
}
