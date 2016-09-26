import java.io.Serializable;

/**
 * This class represents a Request packet. It includes details of file download
 * or search request.
 * 
 * @author Anurag Malik, am3926
 *
 */
public class Request implements Serializable {
	private static final long serialVersionUID = 1L;
	private ClientInterface client;
	private S2SInterface server;
	private String fileName;
	private int destination;

	public Request() {
		this.fileName = null;
		this.destination = 0;
	}

	/**
	 * This method is responsible for packing destination server data
	 * 
	 * @param file
	 * @param server
	 */
	public void packData(String file, int destination) {
		this.fileName = file;
		this.destination = destination;
	}

	/**
	 * Return file name of this request packet
	 * 
	 * @return
	 */
	public String getFileName() {
		return fileName;
	}

	/**
	 * Return details of the destination of this request packet
	 * 
	 * @return
	 */
	public int getDestination() {
		return destination;
	}

	/**
	 * Return details of the client who requested file with this request packet.
	 * 
	 * @return
	 */
	public ClientInterface getClient() {
		return client;
	}

	public void setDestination(int destinationPosition) {
		this.destination = destinationPosition;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public void setClient(ClientInterface callBack) {
		this.client = callBack;
	}

	public S2SInterface getServer() {
		return server;
	}

	public void setServer(S2SInterface server) {
		this.server = server;
	}

}
