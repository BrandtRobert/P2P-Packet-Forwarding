package cs455.overlay.wireframes;

/**
 * Includes all the constants for message protocol types
 * @author Brandt Reutimann
 */
public enum Protocol {
	// Registration
	OVERLAY_NODE_SENDS_REGISTRATION(2),
	REGISTRY_REPORTS_REGISTRATION_STATUS(3),
	// Deregistration
	OVERLAY_NODE_SENDS_DEREGISTRATION(4),
	REGISTRY_REPORTS_DEREGISTRATION_STATUS(5),
	// Overlay setup
	REGISTRY_SENDS_NODE_MANIFEST(6),
	NODE_REPORTS_OVERLAY_SETUP_STATUS(7),
	// Data relay
	REGISTRY_REQUESTS_TASK_INITIATE(8),
	OVERLAY_NODE_SENDS_DATA(9),
	OVERLAY_NODE_REPORTS_TASK_FINISHED(10),
	// Traffic summaries
	REGISTRY_REQUESTS_TRAFFIC_SUMMARY(11),
	OVERLAY_NODE_REPORTS_TRAFFIC_SUMMARY(12);
	// Backing value for the enum
	private int value;
	private Protocol (int value) {
		this.value = value;
	}
	
	public byte getValue () {
		return (byte) value;
	}
	
}
