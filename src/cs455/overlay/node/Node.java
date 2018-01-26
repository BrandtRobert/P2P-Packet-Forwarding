package cs455.overlay.node;

import cs455.overlay.wireframes.Event;

public interface Node {
	// Eventually should change the byte array to an event
	public void onEvent (Event event);
}
