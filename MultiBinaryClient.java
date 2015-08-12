// Student Name: Piotr Bogun
// Source File Name: MultiBinaryClient.java

import java.net.Socket;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;

public class MultiBinaryClient {
	
	private String hostName;
	private int portNumber;
	private String[] fileNames;
	
	private static final String DEFAULT_HOST = "140.192.39.93";
	private static final int DEFAULT_PORT = 6001;
	
	// Default Constructor
	public MultiBinaryClient() {
		hostName = DEFAULT_HOST;
		portNumber = DEFAULT_PORT;
	}
	
	// Constructor with host/port of server
	public MultiBinaryClient(String host, int port) {
		hostName = host;
		portNumber = port;
	}
	
	// Mutator to set string of filenames
	// VarArgs for indeterminate number of objects
	public void setFileNames(String...filenames) {
		this.fileNames = filenames;
	}
	
	public void startDownload() throws IOException {
		
		if (fileNames != null && fileNames.length > 0) {
			for (String filename : fileNames) {
				MultiBinaryDownload fileDownloader = new MultiBinaryDownload();
				fileDownloader.setConnection(hostName, portNumber);
				fileDownloader.setFileName(filename);
				fileDownloader.start();
			}
		}
	}
	
	private static class MultiBinaryDownload extends Thread {
		
		private String fileName;
		private Socket connectionSocket;
		private int downloadState = STATE_NOT_STARTED;
		public static final int BYTE_BUFFER = 1024;
		long serverResponse;
		
		// Download status enums
		public static final int 
			STATE_NOT_STARTED = 1,
			STATE_DOWNLOADED = 2,
			STATE_INVALID_REQUEST = -1,
			STATE_FAILED = -2;
		
		// Create socket connection using hostName/portNumber
		// IOException when socket connection has issues
		public void setConnection(String host, int port) throws IOException {
			connectionSocket = new Socket(host, port);
		}
		
		// Disconnect socket connection
		public void disconnect() {	
			try {
				if (connectionSocket != null) {
					connectionSocket.close();
				}
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		public void setFileName(String filename) {
			fileName = filename;
		}
		
		// Enum downloadState = state string
		// print the current state of the download
		public void printState() {
			
			String state;
			
			switch (downloadState) {
			case STATE_NOT_STARTED:
				state = "Download Not Started";
				break;
			case STATE_DOWNLOADED:
				state = "Download Complete";
				break;
			case STATE_INVALID_REQUEST:
				state = "Invalid File Name";
				break;
			case STATE_FAILED:
				state = "Download Failed";
				break;
			default:
				state = "No Session Started";
			}
			System.out.printf("\nAttempting to download: %s | Status: %s", fileName, state);
		}
		
		public void run() {
			try {
				// Send request to server along with the file name
				OutputStream toServer = connectionSocket.getOutputStream();
				DataOutputStream outStream = new DataOutputStream(toServer);
				outStream.writeUTF(fileName);
				
				// Read server response
				InputStream fromServer = connectionSocket.getInputStream();
				DataInputStream inStream = new DataInputStream(fromServer);
				serverResponse = inStream.readLong();
				
				// IF Invalid Response
				if (serverResponse == STATE_INVALID_REQUEST) {
					downloadState = STATE_INVALID_REQUEST;
				}
				else {
					// read files + use buffer
					ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
					byte buffer[] = new byte[BYTE_BUFFER];
					
					for (int i; (i = inStream.read(buffer)) != -1;) {
						byteStream.write(buffer, 0, i);
					}
					
					byte result[] = byteStream.toByteArray();
					
					// save all the bytes && change state
					createFile(result);
					downloadState = STATE_DOWNLOADED;
				}
			}
			catch (IOException e) {
				e.printStackTrace();
				downloadState = STATE_FAILED;
			}
			finally {
				disconnect();
			}
			printState();
		}
		
		// Create local file with specified name
		public void createFile(byte[] fileContent) throws IOException {
			FileOutputStream fileStream = new FileOutputStream(fileName);
			fileStream.write(fileContent);
			fileStream.close();
		}	
	}

	public static void main(String[] args) {
		
		Scanner reader = new Scanner(System.in);
		
		try {
			List<String> filenames = new ArrayList<String>();
			String userHost, userPort;
			
			MultiBinaryClient MBclient;
			
			System.out.print("Enter server host: (Press enter for default):");
			userHost = reader.nextLine();
			
			if (userHost.trim().length() > 0) {
				System.out.print("Enter server port: (Press enter for default):");
				userPort = reader.nextLine();
				
				try {
					int port = Integer.parseInt(userPort);
					MBclient = new MultiBinaryClient (userHost, port);
				}
				catch (NumberFormatException e) {
					System.err.println("Invalid Port Number!");
					return;
				}
			}
			else {
				System.out.println("Default Host & Port!");
				MBclient = new MultiBinaryClient();
			}
			
			System.out.println("Enter files to download. Each line = one file. Empty line = End of input:");
			
			String userInput = reader.nextLine();
			
			while (userInput.trim().length() > 0) {
				filenames.add(userInput);
				userInput = reader.nextLine();
			}
			
			MBclient.setFileNames(filenames.toArray(new String[filenames.size()]));
			
			try {
				System.out.println("Started Download!");
				MBclient.startDownload();
			}
			catch (IOException e) {
				System.err.println("Download Failed!");
				e.printStackTrace();
			}
		}
		finally {
			reader.close();
		}
	}
}
