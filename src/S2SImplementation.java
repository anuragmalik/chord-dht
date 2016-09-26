import java.io.File;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * S2SImplementation provides implementation of the {@link S2SInterface} It
 * provides methods for forwarding a file search request or inserting a file
 * onto one server from another server.
 * 
 * @author Anurag Malik, am3926
 *
 */
public class S2SImplementation extends UnicastRemoteObject implements S2SInterface {

	private static final long serialVersionUID = 1L;
	private ChordServer server;

	public S2SImplementation(ChordServer server) throws RemoteException {
		super();
		this.server = server;
	}

	@Override
	public boolean forwardRequest(Request request, Trace trace) throws RemoteException {
		
		if (trace == null)
			trace = new Trace();

		System.out.println("New request for file '" + request.getFileName() + "'");
		String fileName = request.getFileName();

		if (checkRange(request.getDestination())) {
			if (server.getFile(fileName) != null) {
				server.sendFile(new File(fileName), request, trace);
				return true;
			} else {
				if (request.getClient() != null)
					request.getClient().pushTrace(trace.getTrace());
				else if (request.getServer() != null)
					request.getServer().pushMessage(trace.getTrace());
			}
		} else {
			return server.forwardRequest(request, trace);
		}
		return false;
	}

	private boolean checkRange(int position) {
		return server.inKeyMap(position);
	}

	@Override
	public void insertFile(byte[] data, String fileName, int destination) throws RemoteException {
		if (checkRange(destination) || destination == -1)
			server.fileInsert(data, fileName);
		else
			server.forwardFileInsert(data, fileName, destination);
	}

	@Override
	public String getHostName() throws RemoteException {
		return server.getHostName();
	}

	@Override
	public void addNewServer(AddRequest request) throws RemoteException {

		if (checkRange(request.getDestination()))
			server.insertPredecessorNode(request);
		else
			server.forwardServerAddRequest(request);
	}

	@Override
	public void updateSuccessor(String serverName) {
		server.updateSuccessor(serverName);
	}

	@Override
	public void updatePredecessor(String serverName, int start, int end) {
		server.updatePredecessor(serverName, start, end);
	}

	@Override
	public void pushMessage(String trace) {
		server.viewMessage(trace);		
	}

}
