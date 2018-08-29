package com.aos;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class SendCsTimestamp implements Runnable{

	private csExecutionTimestamp cstimestamp;

	public SendCsTimestamp(csExecutionTimestamp cstimestamp) {
		this.cstimestamp = cstimestamp;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub			
			String ip = Core.central_process_ip;
			int port = Core.central_process_port;
			Socket clientSocket = null;
			try
			{			
				clientSocket = new Socket(ip,port);
				ObjectOutputStream outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
				outputStream.writeObject(cstimestamp);
				outputStream.flush();
				outputStream.close();
				clientSocket.close();
			}
			catch(IOException ex)
			{
				System.out.println("Got error on client NodeId: "+cstimestamp.nodeId);
				ex.printStackTrace();
			}  finally {
				if (clientSocket != null) {
					try {
						clientSocket.close();
					} catch (IOException e) {
						// log error just in case
					}
				}
			}

	}

}
