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
		} else if (msgType == Protocol.OVERLAY_NODE_SENDS_DEREGISTRATION.getValue()) {
			return new OverlayNodeSendsDeregistration(msg, incomingConnection);
		} else if (msgType == Protocol.REGISTRY_REPORTS_DEREGISTRATION_STATUS.getValue()) {
			return new RegistryReportsDeregistrationStatus(msg, incomingConnection);
		} else {
			System.err.println("Unrecognized message type in event factory");
			return null;
		}
	}
}
