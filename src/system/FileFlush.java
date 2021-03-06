package system;
import gui.FileFlushGUI;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import tracker.TrackerMaintainerClient;

public class FileFlush {

	public static void main(String[] args) throws UnknownHostException {
		User owner = new User(InetAddress.getLocalHost(), ServerThread.SERVER_PORT);
		UserMonitor userMonitor = new UserMonitor(owner);
		TransferMonitor transferMonitor = new TransferMonitor();
    	
        System.out.println("Creating Server Socket");
        ServerThread server = new ServerThread(userMonitor, transferMonitor);
        server.start();
        
        ListenFind listenFind = new ListenFind();
        listenFind.setUser(owner);
        listenFind.start();
        
        FileFlushGUI gui = new FileFlushGUI(userMonitor, transferMonitor);
        gui.setVisible(true);
        
        new TrackerMaintainerClient(userMonitor);
        
        System.out.println("Specify: host port filePath");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String message;
        try {
			while((message = br.readLine()) != null){
				String[] options = message.split(" ");
				String hostName = options[0];
				int port = Integer.parseInt(options[1]);
				String filePath = options[2];
				Socket socket = new Socket(hostName, port);
				UploadThread ut = new UploadThread(socket, new File(filePath));
				new Thread(ut).start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
    }	
}
