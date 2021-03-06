package system;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class UploadThread extends Transfer implements Runnable {
	Socket socket;
	File file;

	public UploadThread(Socket socket, File file) {
		this.socket = socket;
		this.file = file;
	}

	public void run(){
		InputStream is = null;
		BufferedOutputStream os = null;
		byte[] buffer = new byte[8000];
		
		try {
			is = socket.getInputStream();
			os = new BufferedOutputStream(socket.getOutputStream());
			
			os.write(Header.createFileHeader(new FileMetadata(file)));
			os.flush();
			
			if (is.read() == Header.ACCEPT) {
				BufferedInputStream fis = new BufferedInputStream(new FileInputStream(file));
				int count;
				while ((count = fis.read(buffer)) > 0){
					os.write(buffer, 0, count);
					increaseUploadSize(count);
				}
				os.flush();
				fis.close();
			}
			is.close();
			os.close();
			socket.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void increaseUploadSize(int size) {
		transfered += size;
		setChanged();
		notifyObservers(this);
	}

	public String getFilename() {
		return this.file.getName();
	}
	
	public String getProgressFilename() {
		return "▲" + getFilename();
	}
	
	public long getFilesize() {
		return this.file.length();
	}
}
