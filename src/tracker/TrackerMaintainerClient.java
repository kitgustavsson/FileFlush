package tracker;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Observable;
import java.util.Observer;

import system.Header;
import system.User;
import system.UserMonitor;

/**
 * @author alexander
 *
 *	Manages the thread that connects to the tracking server.
 *	Is responsible for starting and restarting this thread when the user changes the tracker server.
 */
public class TrackerMaintainerClient implements Observer{
	TrackerClientThread track;
	UserMonitor userMonitor;
	User owner;
	
	public TrackerMaintainerClient(UserMonitor userMonitor){
		this.userMonitor = userMonitor;
		
    	userMonitor.addObserver(this);
	}
	
	@Override
	public void update(Observable o, Object arg) {
		if (arg.getClass() == String.class && (String) arg == "Server"){
			if (track != null && track.isAlive()) track.CloseConnection();
			track = new TrackerClientThread(userMonitor);
	    	track.start();
		}
	}
	
	/**
	 * @author alexander
	 *
	 *         Manages the client's connection to the Tracking server. Processes
	 *         received information to add and remove elements in the UserMonitor.
	 *
	 */
	private class TrackerClientThread extends Thread{

		private UserMonitor userMonitor;
		private Socket socket;
		private boolean closing = false;

		/**
		 * Initiates a client-to-tracker thread.
		 * 
		 * @param userMonitor The local UserMonitor.
		 * @param owner 
		 */
		public TrackerClientThread(UserMonitor userMonitor) {
			this.userMonitor = userMonitor;
		}

		public void run() {
		BufferedOutputStream os = null;
			try {
				// Start the connection
				socket = new Socket(userMonitor.getTrackerHost(), userMonitor.getTrackerPort());
				
				// Get streams
				os = new BufferedOutputStream(socket.getOutputStream());
				Header header = new Header(new BufferedInputStream(socket.getInputStream()));
				
				os.write(Header.createUserHeader(userMonitor.getOwner()));
				os.flush();
				
				System.out.println("Tracker connection Established");
				
				// Parse incoming users
				while(!closing && !socket.isClosed()){
					header.parseHeader();
					if(header.getType() == 1){
						userMonitor.addUser(header.parseUser());
					}
					else{
						throw new IOException("Unexpected data");
					}
				}
			}
			catch (UnknownHostException e) {
				System.out.println("Failed to resolve Tracker Address");
			}
			catch (SocketException e) {
				System.out.println("Tracker: " + e.getMessage());
			}
			catch (IOException e){
				System.out.println(e.getMessage());
			}
			this.CloseConnection();
		}
		
		public synchronized void CloseConnection(){
			this.closing = true;
			if (socket == null || socket.isClosed()) return;
			else try {
				socket.close();
			} catch (IOException e) {}
		}
	}
}
