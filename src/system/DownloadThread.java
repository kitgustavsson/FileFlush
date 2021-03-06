package system;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class DownloadThread extends Transfer implements Runnable {

	private Socket socket;
	private FileMetadata metadata;
	private boolean download;

	public DownloadThread(FileMetadata metadata, Socket connectionSocket) {
		this.metadata = metadata;
		this.socket = connectionSocket;
		this.download = false;
	}
	
	public void run() {
		FileOutputStream out = null;
		byte[] buffer = new byte[8192];
		int dataLength = 0;
		BufferedInputStream inputStream = null;
		OutputStream outputStream = null;
		try {
			outputStream = socket.getOutputStream();
			if (download) {
				outputStream.write(Header.ACCEPT);
				out = new FileOutputStream(metadata.getFile());
				inputStream = new BufferedInputStream(socket.getInputStream());
				while((dataLength = inputStream.read(buffer, 0, buffer.length)) > 0) {
					out.write(buffer, 0, dataLength);
					increaseDownloadSize(dataLength);
				}
				out.flush();
			} else {
				outputStream.write(Header.NO_ACCEPT);
			}
			outputStream.flush();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (out != null) out.close();
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void increaseDownloadSize(int size) {
		transfered += size;
		setChanged();
		notifyObservers(this);
	}

	@Override
	public String getFilename() {
		return metadata.getFilename();
	}

	public String getProgressFilename() {
		return "▼" + getFilename();
	}

	public long getFilesize() {
		return metadata.getFilesize();
	}

	public void setDownload(boolean download) {
		this.download = download;
	}
}
