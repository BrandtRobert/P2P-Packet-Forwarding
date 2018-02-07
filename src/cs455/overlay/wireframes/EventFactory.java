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
		} else if (msgType == Protocol.REGISTRY_SENDS_NODE_MANIFEST.getValue()) {
			return new RegistrySendsNodeManifest(msg, incomingConnection);
		} else if (msgType == Protocol.NODE_REPORTS_OVERLAY_SETUP_STATUS.getValue()) {
			return new NodeReportsOverlaySetupStatus(msg, incomingConnection);
		} else if (msgType == Protocol.REGISTRY_REQUESTS_TASK_INITIATE.getValue()) {
			return new RegistryRequestsTaskInitiate(msg, incomingConnection);
		} else if (msgType == Protocol.OVERLAY_NODE_SENDS_DATA.getValue()) {
			return new OverlayNodeSendsData(msg, incomingConnection);
		} else if (msgType == Protocol.OVERLAY_NODE_REPORTS_TASK_FINISHED.getValue()) {
			return new OverlayNodeReportsTaskFinished(msg, incomingConnection);
		} else if (msgType == Protocol.REGISTRY_REQUESTS_TRAFFIC_SUMMARY.getValue()) {
			return new RegistryRequestsTrafficSummary(msg, incomingConnection);
		} else if (msgType == Protocol.OVERLAY_NODE_REPORTS_TRAFFIC_SUMMARY.getValue()) {
			return new OverlayNodeReportsTrafficSummary(msg, incomingConnection);
		}  else {
			System.err.println("Unrecognized message type in event factory");
			return null;
		}
	}
}
