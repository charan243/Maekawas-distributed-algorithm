package com.aos;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class CentralProcess {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		int port = Integer.parseInt(args[0]);
		int node_count = Integer.parseInt(args[1]);
		int req_count = Integer.parseInt(args[2]);
		csExecutionTimestamp[][] cs = new csExecutionTimestamp[req_count][node_count];

		int recv_count = 0;
		ServerSocket serverSock = null;
		try
		{
			serverSock = new ServerSocket(port);
			while(recv_count < (node_count*req_count)) {
				Socket sock = serverSock.accept();
				ObjectInputStream inStream = new ObjectInputStream(sock.getInputStream());
				csExecutionTimestamp recvMessage = (csExecutionTimestamp) inStream.readObject();
				inStream.close();
				cs[recvMessage.nodeId][recvMessage.reqs_done-1] = recvMessage;		
				sock.close();
			}
		} catch(IOException | ClassNotFoundException ex) {
			System.out.println("port already in use in central node ");
			ex.printStackTrace();
		} finally {
			if (serverSock != null) {
				try {
					serverSock.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		for(int i=0;i<node_count-1;i++) {
			for (int j=0; j<req_count;j++){
				for (int k=i+1; k<node_count;k++){
					for (int l=0;l<req_count;l++){
						if (incomparable(cs[j][i].endTime,cs[l][k].startTime,cs[j][i].nodeId,cs[l][k].nodeId) && 
								incomparable(cs[j][i].startTime,cs[l][k].endTime,cs[j][i].nodeId,cs[l][k].nodeId)){
							System.out.println("There was possible concurrent CS execution.");
							return;
						}
					}
				}
			}

		}
		System.out.println("The program ran without error.");

	}

	public static boolean incomparable(int[] a, int[] b, int nodeID1, int nodeID2){


		if (a[nodeID1]>b[nodeID1] && b[nodeID2]>a[nodeID2])
			return true;

		return false;
	}



}
