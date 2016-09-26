import java.io.Serializable;

/**
 * This class represents a new server add request packet. It includes details of
 * the host-name and its position within the Chord distributed system network.
 * 
 * @author Anurag Malik, am3926
 *
 */
public class AddRequest implements Serializable {

	private static final long serialVersionUID = 1L;
	int destination;
	String hostName;

	public AddRequest(String hostName2, int dest) {
		this.hostName = hostName2;
		this.destination = dest;
	}

	/**
	 * Set host-name of new server being added to network.
	 * 
	 * @param hostName
	 */
	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	/**
	 * Get position of the new server being added to network.
	 */
	public int getDestination() {
		return destination;
	}

	/**
	 * Set destination of the new server being added to the network.
	 */
	public void setDestination(int destination) {
		this.destination = destination;
	}

	/**
	 * Get host-name of the new server being added to the network.
	 * 
	 * @return
	 */
	public String getHostName() {
		return hostName;
	}
}
