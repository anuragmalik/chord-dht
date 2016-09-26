import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Scanner;

public class Client implements Remote {

	private String lookupDirectory;
	private int PORT = 4040;
	private static int MAX_RANGE = 1000;
	private static String entry_server = "kansas.cs.rit.edu";

	public Client() {
		lookupDirectory = System.getProperty("user.home") + "/Courses/dht/Client/";
	}

	public String getHostName() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return null;
	} // getHostName

	/**
	 * this method is responsible for reading file data from the default lookup
	 * directory on client machine and return its byte data.
	 * 
	 * @param fileName
	 *            : name of file to be read
	 * @return byte data read from file
	 * @throws FileNotFoundException
	 */
	public byte[] readFile(String fileName) throws FileNotFoundException {

		// lookup for file in default lookup directory
		File file = new File(lookupDirectory + fileName);
		if (!file.exists())
			throw new FileNotFoundException();

		// read data into byte array
		byte buffer[] = new byte[(int) file.length()];
		try {
			BufferedInputStream input = new BufferedInputStream(new FileInputStream(file.getPath()));
			input.read(buffer, 0, buffer.length);
			input.close();
		} catch (Exception exp) {
			exp.printStackTrace();
		}
		return buffer;
	} // readFile

	/**
	 * This method is responsible for connecting to any random servers on a
	 * distributed systems network and upload a given file to the server.
	 * 
	 * @param fileName
	 *            : file to be uploaded
	 */
	public void sendToServer(String fileName) {
		try {
			byte[] data = readFile(fileName);

			int dest = Math.abs(fileName.hashCode()) % MAX_RANGE;
			
			System.out.println("Sending file to : " + entry_server);
			String registryURL = "rmi://" + entry_server + ":" + PORT + "/chord";
			S2CInterface server = (S2CInterface) Naming.lookup(registryURL);

			server.insertFile(data, fileName, dest);

		} catch (MalformedURLException | RemoteException | NotBoundException e) {
			System.out.println("Error : Unable to establish connection with server.");
		} catch (FileNotFoundException e) {
			System.out.println("Error : File reading error.\nFile not found.");
		}
	} // sendToServer

	/**
	 * This method is responsible for accepting file data from a server after a
	 * download request has been made for a file and it is found of any of the
	 * servers.
	 * 
	 * @param data
	 *            : data of the file requested
	 * @param fileName
	 *            : name of file to be searched on servers
	 * @return true if file is successfully down-loaded, false otherwise
	 */
	public boolean fileInsert(byte[] data, String fileName) {
		FileOutputStream fos;
		try {
			// write the file data into default lookup directory
			fos = new FileOutputStream(lookupDirectory + fileName);
			fos.write(data);
			fos.close();
			System.out.println("File Insertion successful.");
			return true;
		} catch (IOException e) {
			System.out.println("Failed to read input file.");
			e.printStackTrace();
		}
		return false;
	} // fileInsert

	/**
	 * This method is called to search and request for downloading a file from
	 * servers.
	 * 
	 * @param client
	 *            : reference of the client instance making download request.
	 * @param fileName
	 *            : name of the file being request
	 * @throws RemoteException
	 * @throws NotBoundException
	 * @throws MalformedURLException
	 */
	private static void downloadData(Client client, String fileName)
			throws RemoteException, NotBoundException, MalformedURLException {

		ClientInterface callBack = new ClientImplementation(client);
		Request packet = new Request();
		packet.setFileName(fileName);
		packet.setClient(callBack);

		int id = Math.abs((fileName).hashCode()) % MAX_RANGE;
		packet.setDestination(id);
		
		System.out.println("@Client - Connecting to : " + entry_server);
		String registryURL = "rmi://" + entry_server + ":" + client.PORT + "/chord";
		S2CInterface server = (S2CInterface) Naming.lookup(registryURL);

		server.requestFile(packet);
	} // downloadData

	public static void main(String args[]) {
		Client client = new Client();
		try {

			boolean exit = false;
			Scanner reader = new Scanner(System.in);
			while (!exit) {
				System.out.println(
						"\nOptions :\n\t1. Upload file onto server.\n\t2. Download file from servers.\n\t3. Exit");
				System.out.print("Enter your option : \t");
				String fileName;

				// switch to performing required operation
				switch (reader.nextInt()) {
				case 1:
					System.out.println("Enter FILE NAME?");
					fileName = reader.next();

					// upload file onto server
					client.sendToServer(fileName);
					break;
				case 2:
					System.out.println("Enter FILE NAME?");
					fileName = reader.next();

					// request file to be downloaded from servers, if available
					downloadData(client, fileName);
					break;
				case 3: // exit client service
					exit = true;
					break;
				default:
					System.out.println("Illegal option input");
				}
			}
			reader.close();
		} catch (Exception e) {
			System.out.println("Exception in Client: " + e);
		}
	} // main

}// Client
