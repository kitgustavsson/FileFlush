package tracker;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

import system.Header;
import system.User;
import system.UserMonitor;


/**
 * Manages the thread that accepts incoming connections from clients.
 * Also responsible for tracking active connections to the clients, and pushing updates to them.
 */
public class TrackerMaintainerServer implements Observer {
	private UserMonitor userMonitor;
	private TrackerServerThread server;
	private ArrayList<TrackerClientConnection> clients;

	public TrackerMaintainerServer(final UserMonitor userMonitor) throws InterruptedException {
		this.userMonitor = userMonitor;
		this.clients = new ArrayList<TrackerClientConnection>();
		
		userMonitor.addObserver(this);

		server = new TrackerServerThread(userMonitor, clients);
		server.start();
		
		System.out.println("Tracker Online");
		
		Thread status = new Thread(){
			public void run()
			{
				while(server.isAlive()){
					try {
						Thread.sleep(5000);
						System.out.println("Known Users: " + userMonitor.getUsers().size() + "/" + clients.size());
					} catch (InterruptedException e) {}
				}
			}
		};
		status.start();
	}

	@Override
	public void update(Observable userMonitor, Object arg) {
		if (arg.getClass() != User.class) return;
		
		User user = (User) arg;
		
		// If user was added, they exist in UserMonitor.
		// If user was removed, then they do not exist in UserMonitor.
		if (this.userMonitor.getUsers().contains(user)){
			this.SendUser(user);
		}
		else{
			this.SendRemovedUser(user);
		}
	}
	
	/**
	 * Sends a user to all connected clients.
	 * 
	 * @param user User to send
	 */
	public void SendUser(User user) {
		for (TrackerClientConnection conn : clients){
			// Skip sending updates on a user to that user.
			if (conn.getUser().equals(user)) continue;
			
			try {
				conn.SendUser(user);
			} catch (IOException e) {
				System.out.println("A connection error occured.");
				continue;
			}
		}
	}
	
	public void SendRemovedUser(User user) {
		// TODO Auto-generated method stub
	}
	
	/**
	 *         Maintains the server socket of the Tracker application.
	 *         This socket listens for new peers.
	 */
	private class TrackerServerThread extends Thread {
		public static final int SERVER_PORT = 50001;
		private UserMonitor userMonitor;
		private ArrayList<TrackerClientConnection> clients;

		public TrackerServerThread(UserMonitor userMonitor, ArrayList<TrackerClientConnection> clients) {
			this.userMonitor = userMonitor;
			this.clients = clients;
		}

		public void run() {
			ServerSocket serverIn = null;
			try {
				serverIn = new ServerSocket(SERVER_PORT);
				Socket connectionSocket = null;
				while (true) {
					connectionSocket = serverIn.accept();
					TrackerClientConnection conn = new TrackerClientConnection(connectionSocket, userMonitor, clients);
					this.clients.add(conn);
					conn.start();
				}
			} catch (IOException e) {
				
			} finally {
				try {
					for (TrackerClientConnection conn : this.clients){
						conn.Terminate();
					}
					serverIn.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 *	Represents a connection to a client.
	 *	Is responsible for I/O to a client.
	 */
	private class TrackerClientConnection extends Thread {
		private User user;
		private UserMonitor userMonitor;
		private Socket connectionSocket;
		private BufferedOutputStream os;
		private ArrayList<TrackerClientConnection> clients;

		/**
		 * Initializes a client connection from an existing socket.
		 * Must run Start() on this object to continue execution.
		 * 
		 * @param connectionSocket	The socket used for the connection.
		 * @param userMonitor	The UserMonitor object that keeps a record of users.
		 * @param clients	The List of Clients tracked by the server.
		 */
		public TrackerClientConnection(Socket connectionSocket, UserMonitor userMonitor, ArrayList<TrackerClientConnection> clients) {
			this.connectionSocket = connectionSocket;
			this.userMonitor = userMonitor;
			this.clients = clients;
			System.out.println("New connection:" + connectionSocket.getInetAddress() + ":" + connectionSocket.getPort());
		}
		
		public void run(){
			try {
				BufferedInputStream in = new BufferedInputStream(connectionSocket.getInputStream());
				os = new BufferedOutputStream(connectionSocket.getOutputStream());
				if (this.userMonitor.getOwner()!= null){
					SendUser(this.userMonitor.getOwner());
				}
				
				Header header = new Header(in);
				try {
					header.parseHeader();
				} catch (IOException e) {
					System.out.println(e);
				}
				int type = header.getType();
				
				switch (type) {
				case Header.TYPE_FILE:
					throw new IOException("File transfer attempted. Not supported.");
				case Header.TYPE_USER:
					this.user = header.parseUser();
					SendAllUsers();
					userMonitor.addUser(user);
					break;
				}
				
				while(in.read() != -1); // -1 is sent to FIN connection
				connectionSocket.close();
				
			} catch (IOException e) {
			} finally {
				clients.remove(this);
				this.userMonitor.removeUser(this.user);
			}
			System.out.println("Connection to a client was terminated.");
		}

		/**
		 * Sends all users in UserMonitor to this connection's peer.
		 * 
		 * @throws IOException
		 */
		synchronized public void SendAllUsers() throws IOException {
			for (User u : userMonitor.getUsers()) {
				os.write(Header.createUserHeader(u));
			}
			os.flush();
		}

		
		/**
		 * Sends a single user to this connection's peer.
		 * 
		 * @param u The user to transmit.
		 * @throws IOException
		 */
		synchronized public void SendUser(User u) throws IOException {
			os.write((u.toString() + ";").getBytes());
			os.flush();
		}
		
		/**
		 * Terminate the connection to this client and removes it from the client and user lists.
		 * @throws IOException 
		 */
		public void Terminate() throws IOException{
			clients.remove(this);
			this.userMonitor.removeUser(this.user);
			connectionSocket.close();
		}
		
		/**
		 * Gets the user object associated with this connection.
		 * 
		 * @return User object representing this connection's peer.
		 */
		public User getUser(){
			return this.user;
		}
	}
}
