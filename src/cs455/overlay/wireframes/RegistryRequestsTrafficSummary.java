package cs455.overlay.wireframes;

import cs455.overlay.transport.TCPConnection;

public class RegistryRequestsTrafficSummary implements Event {
	private Protocol msgType = Protocol.REGISTRY_REQUESTS_TRAFFIC_SUMMARY;
	TCPConnection responseConnection;
	
	public RegistryRequestsTrafficSummary(byte [] msg, TCPConnection incomingConnection) {
		this.responseConnection = incomingConnection;
	}

	public RegistryRequestsTrafficSummary() {
		responseConnection = null;
	}

	@Override
	public Protocol getType() {
		return msgType;
	}

	@Override
	public byte[] getBytes() {
		byte [] toReturn = {msgType.getValue()};
		return toReturn;
	}

	@Override
	public TCPConnection getResponseConnection() {
		return responseConnection;
	}
	
}
