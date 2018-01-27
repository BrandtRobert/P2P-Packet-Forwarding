package cs455.overlay.wireframes;

import cs455.overlay.transport.TCPConnection;

/**
 * Given a new message (byte-string) creates and returns an event.
 * @author Brandt Reutimann
 */
public class EventFactory {
	public static Event convertBytesToEvent (byte [] msg, TCPConnection incomingConnection) {
		byte msgType = msg[0];
		if (msgType == Protocol.OVERLAY_NODE_SENDS_REGISTRATION.getValue()) {
			return new OverlayNodeSendsRegistration(msg, incomingConnection);
		} else if (msgType == Protocol.REGISTRY_REPORTS_REGISTRATION_STATUS.getValue()) {
			return new RegistryReportsRegistrationStatus(msg, incomingConnection);
		} else {
			return null;
		}
	}
}
