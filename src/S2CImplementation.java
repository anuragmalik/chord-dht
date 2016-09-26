import java.io.File;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * This class provides implementation of the {@link S2CInterface}. It provides
 * methods for interaction between a Server and a Client. It allows
 * functionality for file search, file download and file insertion.
 * 
 * @author Anurag Malik, am3926
 *
 */
public class S2CImplementation extends UnicastRemoteObject implements S2CInterface {
	private static final long serialVersionUID = 1L;
	private ChordServer server;

	public S2CImplementation(ChordServer server) throws RemoteException {
		super();
		this.server = server;
	}

	@Override
	public boolean searchFile(Request request, ClientInterface client) throws RemoteException {
		System.out.println("New request for file '" + request.getFileName() + "' from :" + client.getAddress());
		String fileName = request.getFileName();
		// setNode(request.getDestination());

		File file = server.getFile(fileName);
		if (file != null) {
			return true;
		} else
			return false;

	}

	@Override
	public void insertFile(byte[] data, String fileName, int destination) throws RemoteException {
		if(checkRange(destination)) 
			server.fileInsert(data, fileName);
		else
			server.forwardFileInsert(data, fileName, destination);
	}

	@Override
	public synchronized boolean requestFile(Request request) throws RemoteException {

		System.out.println(
				"New request for file '" + request.getFileName() + "' from : " + request.getClient().getAddress());
		Trace trace = new Trace();
		String fileName = request.getFileName();
		if (checkRange(request.getDestination())) {
			if (server.getFile(fileName) != null) {
				server.sendFile(new File(fileName), request, trace);
				return true;
			} else {
				request.getClient().pushTrace(trace.getTrace());
			}
		} else {
			return server.forwardRequest(request, trace);
		}
		return false;

	}

	private boolean checkRange(int position) {
		return server.inKeyMap(position);
	}

}
