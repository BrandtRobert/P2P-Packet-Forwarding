package cs455.overlay.routing;

import java.net.InetAddress;

/**
 * Represents an entry in the routing table.
 * Contains info on a registere host:
 * 	- id (assigned by the routing table)
 * 	- ip (host's ip address)
 * 	- port (port the host is listening on)
 * @author Brandt Reutimann
 */
public class RoutingTableEntry {
	private InetAddress ip; // IP address of this host
	private int port;		// Port the host is listening on
	private int id;
	private boolean isFinished;
	
	public RoutingTableEntry (InetAddress ip, int port) {
		this.ip = ip;
		this.port = port;
		isFinished = false;
	}
	/**
	 * @return the ip
	 */
	public InetAddress getIp() {
		return ip;
	}

	/**
	 * @param ip the ip to set
	 */
	public void setIp(InetAddress ip) {
		this.ip = ip;
	}

	/**
	 * @return the port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * @param port the port to set
	 */
	public void setPort(int port) {
		this.port = port;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((ip == null) ? 0 : ip.hashCode());
		result = prime * result + port;
		return result;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		RoutingTableEntry other = (RoutingTableEntry) obj;
		if (ip == null) {
			if (other.ip != null) {
				return false;
			}
		} else if (!ip.equals(other.ip)) {
			return false;
		}
		if (port != other.port) {
			return false;
		}
		return true;
	}
	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(int id) {
		this.id = id;
	}
	
	public synchronized boolean isFinished() {
		return isFinished;
	}
	
	public synchronized void setIsFinished() {
		isFinished = true;
	}
	
	public synchronized void resetIsFinished() {
		isFinished = false;
	}
	
}
