package cs455.overlay.wireframes;

import cs455.overlay.transport.TCPConnection;

/**
 * Defines an interface for all wireframe messages. These messages exist as events that nodes can respond to.
 * @author Brandt Reutimann
 *
 */
public interface Event {
	public Protocol getType();
	public byte[] getBytes();
	public TCPConnection getResponseConnection();
}
