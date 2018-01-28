package cs455.overlay.wireframes;

import cs455.overlay.transport.TCPConnection;

/**
 * Wireframe is the same as RegistryReportsRegistrationStatus, except that it has a different msg type.
 * "The registry will respond with a REGISTRY_REPORTS_DEREGISTRATION_STATUS that is similar to
 * the REGISTRY_REPORTS_REGISTRATION_STATUS message."
 * @author Brandt Reutimann
 *
 */
public class RegistryReportsDeregistrationStatus extends RegistryReportsRegistrationStatus {
	
	public RegistryReportsDeregistrationStatus(int status, String infoString) {
		super(status, infoString);
		super.msgType = Protocol.REGISTRY_REPORTS_DEREGISTRATION_STATUS;
	}

	public RegistryReportsDeregistrationStatus(byte[] msg, TCPConnection incomingConnection) {
		super(msg, incomingConnection);
		super.msgType = Protocol.REGISTRY_REPORTS_DEREGISTRATION_STATUS;
	}

}
