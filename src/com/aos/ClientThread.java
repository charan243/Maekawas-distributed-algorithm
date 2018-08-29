package com.aos;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class ClientThread implements Runnable{

	private Message msg;
	private int nodeId;

	public ClientThread(Message msg, int nodeId) {
		this.msg = msg;
		this.nodeId = nodeId;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub			
			ArrayList<String> arr = Core.location.get(nodeId);			
			String ip = arr.get(0);
			int port = Integer.parseInt(arr.get(1));
			Socket clientSocket = null;
			try
			{			
				clientSocket = new Socket(ip,port);
				ObjectOutputStream outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
				
				outputStream.writeObject(msg);
				outputStream.flush();
				outputStream.close();
				clientSocket.close();
			}
			catch(IOException ex)
			{
				System.out.println("Got error on client NodeId: "+nodeId);
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