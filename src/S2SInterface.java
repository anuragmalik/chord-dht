import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * This interface provideSs methods for interaction between two servers.
 * 
 * @author Anurag Malik, am3926
 *
 */
public interface S2SInterface extends Remote {

	// receive forwarded request from another server
	boolean forwardRequest(Request packet, Trace trace) throws RemoteException;

	// receive file data from another server
	void insertFile(byte[] data, String fileName, int destination) throws RemoteException;
	
	// return host name of the server machine
	String getHostName() throws RemoteException;

	// request for adding new server to the network 
	void addNewServer(AddRequest request) throws RemoteException;

	// update details of the successor server machine in network
	void updateSuccessor(String server) throws RemoteException;

	// update details of the predecessor server machine in network
	void updatePredecessor(String server, int start, int end) throws RemoteException;

	// push message on the server machine
	void pushMessage(String trace) throws RemoteException;

}
