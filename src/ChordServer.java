import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Scanner;

/**
 * 
 * ChordServer represents a server capable of interacting with other similar
 * servers in a distributed systems setup. Each server can accept file search
 * and download request from other servers on network or from client. The
 * systems are connected using a Chord setup based upon consistent hashing.
 *
 * @author Anurag Malik, am3926
 *
 */

public class ChordServer extends Thread implements Serializable {

	private static final long serialVersionUID = 1L;
	static final int PORT = 4040;
	private HashSet<String> fileMap;
	private boolean inChord;
	private String lookupDirectory;
	private int[] keyRange;
	private int MAX_RANGE = 1000;

	// entry server for chord network
	private static String ENTRY_SERVER = "kansas.cs.rit.edu";
	private String pServer;
	private String sServer;
	private static String domain = ".cs.rit.edu";

	public ChordServer() {
		pServer = getHostName() + domain;
		sServer = getHostName() + domain;

		keyRange = new int[2];
		keyRange[0] = 0;
		keyRange[1] = MAX_RANGE;

		inChord = false;

		fileMap = new HashSet<String>();
		lookupDirectory = System.getProperty("user.home") + "/Courses/chord/" + getHostName() + "/";

	}

	@Override
	public void run() {
		execServer();
	}

	/**
	 * This method is run through a thread, to start the server and register its
	 * instance with RMI registry
	 */
	public void execServer() {

		try {
			// export rmi instance for Server to Client interaction
			S2CInterface exportedObj = new S2CImplementation(this);

			// export rmi instance for Server to Server interaction
			S2SInterface serverInterface = new S2SImplementation(this);

			// bind exported instanced on RMI registry
			Registry registry = LocateRegistry.createRegistry(PORT);
			registry.rebind("chord", exportedObj);
			registry.rebind("server", serverInterface);

			// if the server is itself entry point for chord, set its inChord
			// flag true.
			if (ENTRY_SERVER.equals(getHostName() + domain))
				inChord = true;
			System.out.println("Server Name : " + getHostName());
			System.out.println("Lookup directory : " + lookupDirectory);

		} catch (Exception exp) {
			System.out.println("Exception @ Server: " + exp);
		}
	}

	/**
	 * Return host name for this server
	 * 
	 * @return host name of the current server.
	 */
	public String getHostName() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * This method allows for a file to be inserted /down-loaded into default
	 * lookup directory of a server.
	 * 
	 * @param data
	 *            : file data
	 * @param fileName
	 *            : name of file being down-loaded
	 * @return True if file insertion is successful, False otherwise
	 */
	public boolean fileInsert(byte[] data, String fileName) {
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(lookupDirectory + fileName);
			fos.write(data);
			fos.close();
			System.out.println(fileName + " : new file inserted.");
			fileMap.add(fileName);
			return true;
		} catch (IOException e) {
			System.out.println("Failed to read input file.");
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Check if a file exists in default lookup directory of a server.
	 * 
	 * @param fileName
	 *            : file being searched
	 * @return File instance if file is found.
	 */
	public File getFile(String fileName) {
		File file = new File(lookupDirectory + fileName);
		if (file.exists())
			return file;
		else
			return null;
	}

	/**
	 * Utility function to read a requested file from default lookup directory
	 * on a server
	 * 
	 * @param file
	 *            : file to be read
	 * @return bytes of file data
	 * @throws FileNotFoundException
	 */
	private byte[] readFile(File file) throws FileNotFoundException {
		if (!file.exists())
			throw new FileNotFoundException();

		byte buffer[] = new byte[(int) file.length()];
		try {
			BufferedInputStream input = new BufferedInputStream(new FileInputStream(file.getPath()));
			input.read(buffer, 0, buffer.length);
			input.close();
		} catch (Exception exp) {
			exp.printStackTrace();
		}
		return (buffer);
	}

	/**
	 * This method is used to send a file to the client who requested it.
	 * 
	 * @param file
	 *            : file to be sent
	 * @param request
	 *            : request packet
	 * @param trace
	 *            : trace data for this request
	 */
	public void sendFile(File file, Request request, Trace trace) {

		ClientInterface client = request.getClient();
		S2SInterface server = request.getServer();
		try {
			// check if this is a file download request from client
			if (client != null) {
				// read file from server directory and send it to client
				byte[] buffer = readFile(file);
				trace.addToTrace(getHostName());
				trace.setStatus(true);

				// send file data along with whole trace of this request
				client.pushFile(buffer, trace.getTrace(), file.getName());
			}
			// check if this is a file search request from another server
			else if (server != null) {
				trace.addToTrace(getHostName());
				trace.setStatus(true);

				// push the trace of file search to the requesting server
				server.pushMessage(trace.getTrace());
			}

		} catch (Exception e) {
			System.out.println("File read & transfer error.");
			try {
				// just send the trace to the client
				if (client != null)
					client.pushTrace(trace.getTrace());
			} catch (RemoteException e1) {
				System.out.println("Lost connection with client. Exiting.");
				return;
			}
			return;
		}
	}

	/**
	 * If a file being requested from this server is not found, then forward
	 * request to another parent server.
	 * 
	 * @param request
	 * @param trace
	 * @return
	 */
	public boolean forwardRequest(Request request, Trace trace) {
		trace.addToTrace(getHostName());
		try {
			S2SInterface server = getConnectionToServer(sServer);
			System.out.println("Forwarding " + request.getFileName() + " search request to server: " + sServer);
			return server.forwardRequest(request, trace);

		} catch (RemoteException e) {
			System.out.println("@" + getHostName() + ": Failure connecting to -" + sServer);
			e.printStackTrace();
		}

		return false;
	}

	/**
	 * This method is used by a server in chord to redirect the file insertion
	 * request to its Successor server
	 * 
	 * @param data
	 * @param fileName
	 * @param destination
	 */
	public void forwardFileInsert(byte[] data, String fileName, int destination) {
		try {
			System.out.println("Forwarding file insert request :" + sServer);
			S2SInterface server = getConnectionToServer(sServer);

			// forward all details and data to successor node
			server.insertFile(data, fileName, destination);
		} catch (RemoteException e) {
			System.out.println("Forwarding file insert request : Failed to connect - " + sServer);
			e.printStackTrace();
		}
	}

	/**
	 * Program execution starts with creating new instance of the ChordServer
	 * and displaying the appropriate options on server for either joining a
	 * Chord Distributed network, exiting from Chord, View information on server
	 * or to upload or download a file onto network.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Scanner scr = new Scanner(System.in);
		ChordServer server = new ChordServer();

		// start execution thread
		server.start();
		try {
			Thread.sleep(1500);
		} catch (InterruptedException e) {
		}
		int option = 0;

		// display options and take appropriate action on input
		while (option != 5) {
			System.out.print("Options:\n\t1. ");
			if (!server.inChord)
				System.out.println("Join Chord");
			else
				System.out.println("Exit Chord");

			System.out.println("\n\t2. View Server Information");
			System.out.println("\n\t3. Upload file");
			System.out.println("\n\t4. Search file");
			System.out.println("\n\t5. Exit Server");
			option = scr.nextInt();

			if (option == 1) {
				if (!server.inChord) {
					server.enterNetwork();
				} else {
					server.exitNetwork();
				}
			}

			if (option == 2)
				server.viewInfo();

			if (option == 3 || option == 4) {
				System.out.print("Enter file name.. \t");
				if (option == 3)
					server.uploadFile(scr.next());
				else
					server.searchFile(scr.next());
			}
		}

		if (server.inChord)
			server.exitNetwork();

		System.out.println("Server shutting down");
		// System.exit(0);
		scr.close();
	}

	/**
	 * This method is used by a server to search a file on the network, a file
	 * search request is first send to the successor of the current server.
	 * Also, the server checks if the file being searched is present on
	 * localhost.
	 * 
	 * @param fileName
	 */
	private void searchFile(String fileName) {

		// check if file exist on localhost
		File file = new File(lookupDirectory + fileName);
		if (file.exists()) {
			System.out.println("File found at localhost");
			return;
		}

		// check if there are no other servers in chord network.
		if (sServer.equals(getHostName() + domain))
			return;

		// create a new file search request packet and forward to successor
		// server
		Request packet = new Request();
		packet.setFileName(fileName);
		packet.setDestination(Math.abs(fileName.hashCode() % MAX_RANGE));
		try {
			packet.setServer(new S2SImplementation(this));
			S2SInterface server = getConnectionToServer(sServer);
			server.forwardRequest(packet, null);
		} catch (RemoteException e) {
			System.out.println("Failed to connect : " + sServer);
			e.printStackTrace();
		}
	}

	/**
	 * ViewMessage method is used for message passing. The current server can
	 * receive messages from another servers.
	 * 
	 * @param message
	 */
	public void viewMessage(String message) {
		System.out.println(message);
	}

	/**
	 * This method is used by a server on chord distributed system to upload a
	 * file from its directory onto the network and it is saved at its right
	 * destination server.
	 * 
	 * @param fileName
	 */
	private void uploadFile(String fileName) {

		if (sServer.equals(getHostName() + domain))
			return;

		int dest = Math.abs(fileName.hashCode()) % MAX_RANGE;
		File file = new File(lookupDirectory + fileName);
		try {
			System.out.println("Inserting file - successor @" + sServer);
			// read file data
			byte[] data = readFile(file);

			// get connection to successor server and forward file data for
			// write
			S2SInterface server = getConnectionToServer(sServer);
			server.insertFile(data, fileName, dest);

			// delete the uploaded file from local directory
			deleteFile(file);

		} catch (FileNotFoundException e) {
			System.out.println("Error : File not found.");
			e.printStackTrace();
		} catch (RemoteException e) {
			System.out.println("Error : Failed to connect to successor.");
			e.printStackTrace();
		}
	}

	/**
	 * This method is used to display all the information of the current server,
	 * details include the host name, predecessor and successor nodes, and all
	 * the files stored on this server.
	 */
	private void viewInfo() {

		System.out.println("***SERVER INFORMATION***");
		System.out.println("Hostname : " + getHostName());
		System.out.println("Connected servers:");
		System.out.println("\tPredecessor : " + pServer);
		System.out.println("\tSuccessor : " + sServer);

		int count = 0;
		System.out.println("\nFiles:");
		Iterator<String> itr = fileMap.iterator();
		while (itr.hasNext()) {
			System.out.println("\t" + ++count + ". " + itr.next());
		}

		System.out.println("*************************\n");
	}

	/**
	 * This method is used by a server to exit from a chord distributed network.
	 * The server leaving from the network is responsible for sending all its
	 * files on its successor server and also notify/ update both its successor
	 * and predecessors details.
	 * 
	 */
	private void exitNetwork() {

		if (!inChord || pServer.equals(getHostName() + domain) || sServer.equals(getHostName() + domain))
			return;

		if ((getHostName() + domain).equals(ENTRY_SERVER)) {
			System.out.println("Entry point server in chord can't exit chord");
			return;
		}

		// move files to successor server
		moveFilesToServer(sServer, true);

		// connect to predecessor and update its successor
		// update predecessor -> successor = current -> successor
		S2SInterface server = getConnectionToServer(pServer);
		System.out.println("Updating values on: " + pServer);
		try {
			server.updateSuccessor(sServer);

			// connect to successor server and update its predecessor, and range
			// value
			server = getConnectionToServer(sServer);
			System.out.println("Updating values on: " + sServer);
			server.updatePredecessor(pServer, keyRange[0], -1);

			// reset predecessor and successor nodes to self.
			inChord = false;
			pServer = getHostName() + domain;
			sServer = getHostName() + domain;
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	/**
	 * While leaving a chord distributed network or while accepting a new server
	 * as its predecessor, the current server moves some or all of its files
	 * onto its adjacent servers in the network.
	 * 
	 * @param serverName
	 * @param allFiles
	 */
	public void moveFilesToServer(String serverName, boolean allFiles) {

		// get iterator on all files in map
		Iterator<String> itr = fileMap.iterator();
		if (allFiles != true) {

			// if not all files has to be copied.
			// fetch files with hash-code less than key range start
			HashSet<String> newMap = new HashSet<String>();
			while (itr.hasNext()) {
				String file = itr.next();
				int position = Math.abs(file.hashCode()) % MAX_RANGE;
				if (position < keyRange[0]) {
					newMap.add(file);
					itr.remove();
				}
			}

			// create new iterator
			itr = newMap.iterator();
		}

		try {
			int count = 0;
			while (itr.hasNext()) {

				// fetch all files data and forward it to the required server
				File file = new File(lookupDirectory + itr.next());
				byte[] buffer = readFile(file);

				// delete this file from the current server
				deleteFile(file);

				S2SInterface server = getConnectionToServer(serverName);
				server.insertFile(buffer, file.getName(), -1);
				count++;
			}

			// if all files were moved, clear hashset of file names.
			if (allFiles)
				fileMap.clear();

			System.out.println("@" + getHostName() + " - " + count + " files copied to Server : " + serverName);

		} catch (FileNotFoundException e) {
			System.out.println("Replication failed. File not found.");
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method is used to delete a file from the local directory of current
	 * server
	 * 
	 * @param file
	 */
	private void deleteFile(File file) {
		try {
			Files.deleteIfExists(file.toPath());
		} catch (IOException e) {
			System.out.println("Error : File to be deleted not found.");
			e.printStackTrace();
		}
	}

	/**
	 * This method is used to raise request for joining the chord distributed
	 * network. The current server is added to the chord network at its correct
	 * position based upon its hash value.
	 * 
	 */
	private void enterNetwork() {

		// create a new add request packet and forward to the entry_server.
		int dest = Math.abs(getHostName().hashCode()) % MAX_RANGE;
		AddRequest addReq = new AddRequest(getHostName() + domain, dest);
		S2SInterface entryServer = getConnectionToServer(ENTRY_SERVER);
		try {
			entryServer.addNewServer(addReq);
			inChord = true;
		} catch (RemoteException e) {
			System.out
					.println("@" + getHostName() + ": Not added to Chord Network. Failure connecting -" + ENTRY_SERVER);
		}
	}

	/**
	 * This method is used by a server in the distributed network to get
	 * connection to another server in the network, through RMI lookup.
	 * 
	 * @param serverName
	 * @return
	 */
	private S2SInterface getConnectionToServer(String serverName) {
		System.out.println("@" + getHostName() + " - Connecting to : " + serverName);
		String hostName = "rmi://" + serverName + ":" + PORT + "/server";
		try {
			return (S2SInterface) Naming.lookup(hostName);
		} catch (MalformedURLException | RemoteException | NotBoundException e) {
			System.out.println("@" + getHostName() + ": Failure connecting to -" + serverName);
		}
		return null;
	}

	/**
	 * This method is used to check if a value lies in the key range of current
	 * server.
	 * 
	 * @param position
	 * @return true/ false
	 */
	public boolean inKeyMap(int position) {
		return position >= keyRange[0] && position <= keyRange[1] ? true : false;
	}

	/**
	 * This method is used to update host-name of the successor server of the
	 * current server.
	 * 
	 * @param hostName
	 */
	public void updateSuccessor(String hostName) {
		sServer = hostName;
	}

	/**
	 * This method is used to update the Predecessor Hostname, key Space of the
	 * current server
	 * 
	 * @param hostName
	 * @param start
	 * @param end
	 */
	public void updatePredecessor(String hostName, int start, int end) {
		pServer = hostName;
		if (start > 0)
			this.keyRange[0] = start;
		if (end > 0)
			this.keyRange[1] = end;
	}

	/**
	 * This method is responsible for forwarding a new server add request to its
	 * successor server in the chord distributed network.
	 * 
	 * @param request
	 */
	public void forwardServerAddRequest(AddRequest request) {
		try {
			if (sServer.equals(getHostName() + domain))
				return;

			// get connection and forward request to its successor server
			S2SInterface server = getConnectionToServer(sServer);
			System.out.println("Forwarding new node adding request to successor server : " + sServer);
			server.addNewServer(request);

		} catch (RemoteException e) {
			System.out.println("Error : Couldn't forward new server add request to successor server :" + sServer);
		}

	}

	/**
	 * This method is responsible for accepting a new server add request in the
	 * key space of the current server. It includes updating the predecessor
	 * details and updating values on new server.
	 * 
	 * @param request
	 */
	public void insertPredecessorNode(AddRequest request) {

		// check if the new server wants to get added at the same position as
		// the current server, return with a failure in such a case
		if (request.getDestination() == keyRange[1]) {
			S2SInterface server = getConnectionToServer(request.getHostName());
			try {
				server.pushMessage("Failure : Another server already exist at same position in Chord");
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			return;
		}

		String oldPredecessor = pServer;
		int oldStartRange = keyRange[0];

		// update predecessor and starting key space on current server
		pServer = request.getHostName();
		keyRange[0] = request.getDestination() + 1;

		// update previous predecessor
		S2SInterface server = getConnectionToServer(oldPredecessor);
		System.out.println("Updating successor for: " + oldPredecessor);
		try {
			server.updateSuccessor(request.getHostName());

			// update new predecessor server, with details of its successor and
			// predecessors
			server = getConnectionToServer(pServer);
			System.out.println("Updating values on: " + pServer);
			server.updatePredecessor(oldPredecessor, oldStartRange, request.getDestination());
			server.updateSuccessor(getHostName() + domain);
		} catch (RemoteException e) {
			e.printStackTrace();
		}

		// move files to new predecessor server
		moveFilesToServer(pServer, false);
	}
}
