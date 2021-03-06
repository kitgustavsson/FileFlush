package system;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerThread extends Thread {
	
	public static final int SERVER_PORT = 50000;
	
	private UserMonitor userMonitor;
	private TransferMonitor transferMonitor;

	public ServerThread(UserMonitor userMonitor, TransferMonitor transferMonitor) {
		this.userMonitor = userMonitor;
		this.transferMonitor = transferMonitor;
	}

	public void run() {
		ServerSocket serverIn = null;
		try {
			 serverIn = new ServerSocket(SERVER_PORT);
			 Socket connectionSocket = null;
			 while ((connectionSocket = serverIn.accept()) != null) {
				BufferedInputStream in = new BufferedInputStream(connectionSocket.getInputStream());

				Header header = new Header(in);
				try {
					header.parseHeader();
				} catch (IOException e) {
					System.out.println(e);
					continue;
				}
				int type = header.getType();
				switch (type) {
				case Header.TYPE_FILE:
					FileMetadata metadata = header.parseFileMetadata();
					metadata.setDirectory(userMonitor.getDirectory());
					System.out.println(String.format("Filename: %s | filesize: %s",
							metadata.getFilename(), metadata.getFilesize()));
					DownloadThread dt = new DownloadThread(metadata, connectionSocket);
					transferMonitor.addDownload(dt);
					break;
				case Header.TYPE_USER:
					User user = header.parseUser();
					System.out.println("TCP user packet: " + user);
					userMonitor.addUser(user);
					System.out.println("Users:");
					for (User u : userMonitor.getUsers()) {
						System.out.println(u);
					}
					break;
				}
			 }
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				serverIn.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
