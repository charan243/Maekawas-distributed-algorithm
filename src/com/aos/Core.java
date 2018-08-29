package com.aos;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.concurrent.Semaphore;

public class Core{
	static public final int REQUEST_MSG_TYPE = 0;
	static public final int LOCK_MSG_TYPE = 1;
	static public final int RELEASE_MSG_TYPE = 2;
	static public final int INQUIRE_MSG_TYPE = 3;
	static public final int YIELD_MSG_TYPE = 4;
	static public final int FAILED_MSG_TYPE = 5;
	static public final int DONE_MSG_TYPE = 6;
	//	static public Integer TERMINATE_MSG_TYPE = 6;
	public static int nodeId;
	public static HashMap<Integer, ArrayList<String>> location;
	public static ArrayList<Integer> quorum_members;
	public static int node_count;
	public static int interdelay;
	public static int cs_exec_time;
	public static int req_count;
	public static Exprv exp_rv_inter_request;
	public static Exprv exp_rv_cs_exec_time;
	public static int logical_clock;
	public static int[] vector_clock;
	public static String config_file;
	public static int membership_size;
	public static PriorityQueue<RequestTimestamp> queue = new PriorityQueue<RequestTimestamp>();
	public static RequestTimestamp locked = null;	// for current locked node id
	public static boolean sent_inquire = false;
	public static int central_process_port;
	public static String central_process_ip;
	
	
	public static int count_lock_msg_recv = 0;
	public static boolean inCS = false; // shared variable have to use semaphore
	public static boolean recvFailed_msg = false; // used between processInquire and processFailed
	public static ArrayList<Integer> recv_inquire_list = null;
	public static Semaphore csSem = new Semaphore(0,true);  
	public static Semaphore clockSem = new Semaphore(1,true);
	public static Semaphore incsSem = new Semaphore(1,true);
	public static int reqDone = 0;
	public static int recvDone = 0;
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		nodeId = Integer.parseInt(args[0]);
		location = getLocation(args[1]);
		quorum_members = getNeighbors(args[2]);
		node_count = Integer.parseInt(args[3]);
		interdelay = Integer.parseInt(args[4]);
		cs_exec_time = Integer.parseInt(args[5]);
		req_count = Integer.parseInt(args[6]);		
		config_file = args[7];
		membership_size = getMembershipSize(args[8],nodeId);
		central_process_port = Integer.parseInt(args[9]);
		central_process_ip = args[10];
		
		/*		
		System.out.println("NodeId: "+nodeId);
		System.out.println("location: "+location);
		System.out.println("neighbors: "+quorum_members);
		System.out.println("node_count: "+node_count);
		System.out.println("interdelay: "+interdelay);
		System.out.println("cs_exec_time: "+cs_exec_time);
		System.out.println("req_count: "+req_count);
		System.out.println("config_file: "+config_file);
		System.out.println("membership_size: "+membership_size);
		 */		
		Core core = new Core();	
		core.go();

		//output to file
		//toFile(NodeId,c.parent,c.childern,config_file,path);
	}

	public void go() {
		int port = Integer.parseInt(location.get(nodeId).get(1));
		Thread server = new Thread(new ServerThread(port));
		server.start();
		exp_rv_cs_exec_time = new Exprv(cs_exec_time);
		exp_rv_inter_request = new Exprv(interdelay);
		for(int i=0;i<req_count;i++) {
			try {
				
				clockSem.acquire();
				int[] startTime = vector_clock.clone();
				clockSem.release();
				
				csEnter();
				
				incsSem.acquire();
				inCS = true;
				incsSem.release();
				
				Thread.sleep(exp_rv_cs_exec_time.exp_rv().longValue());
				
				csLeave();
				reqDone++;
				
				clockSem.acquire();
				int[] endTime = vector_clock.clone();
				clockSem.release();
				
				// sending cs execution timestamps to central process
				Thread send_cstimestamp = new Thread(new SendCsTimestamp(new csExecutionTimestamp(nodeId, startTime, endTime, reqDone)));
				send_cstimestamp.start();
				
				incsSem.acquire();
				inCS = false;
				incsSem.release();
				
				Thread.sleep(exp_rv_inter_request.exp_rv().longValue());
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		for(int j=0;j<quorum_members.size();j++) {
			send(quorum_members.get(j), DONE_MSG_TYPE);
		}

		
	}

	public void csEnter() {
		for(int j=0;j<quorum_members.size();j++) {
			send(quorum_members.get(j), REQUEST_MSG_TYPE);
		}
		try {
			csSem.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void csLeave() {
		for(int j=0;j<quorum_members.size();j++) {
			send(quorum_members.get(j), RELEASE_MSG_TYPE);
		}
	}



	public static void send(int target_nodeId,int msg_type) {
		try {
			clockSem.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// update clock
		logical_clock++;
		vector_clock[nodeId]++;		
		Message msg = new Message(Core.nodeId, msg_type,Core.logical_clock,Core.vector_clock,Core.reqDone);
		clockSem.release();
		
		Thread send = new Thread(new ClientThread(msg, target_nodeId));
		send.start();
	}


	public static int getMembershipSize(String str,int id) {
		String a[] = str.split(" ");
		int size=0;
		for(int i=0;i<a.length;i++) {
			if(Integer.parseInt(a[i]) == id) {
				size++;
			}			
		}
		return size;
	}

	public static HashMap<Integer, ArrayList<String>> getLocation(String str) {
		String a[] = str.split("#");
		HashMap<Integer, ArrayList<String>> map = new HashMap<Integer, ArrayList<String>>();
		for(int i=0;i<a.length;i++) {
			//System.out.print(a[i]+",");
			String b[] = a[i].split(" ");
			ArrayList<String> arr = new ArrayList<String>();
			arr.add(b[1]);
			arr.add(b[2]);
			map.put(Integer.parseInt(b[0]),arr);
		}
		return map;
	}

	public static ArrayList<Integer> getNeighbors(String str) {
		String a[] = str.split(" ");
		ArrayList<Integer> arr = new ArrayList<Integer>();
		for(int i=0;i<a.length;i++) {
			arr.add(Integer.parseInt(a[i]));
		}
		return arr;
	}

	public class ServerThread implements Runnable{

		private int port;

		public ServerThread(int port){
			this.port = port;
		}

		public void run() {
			ServerSocket serverSock = null;
			try
			{
				serverSock = new ServerSocket(port);
				while((recvDone < membership_size) && (reqDone < req_count)) {	// have to change the while condition
					Socket sock = serverSock.accept();
					ObjectInputStream inStream = new ObjectInputStream(sock.getInputStream());
					Message recvMessage = (Message) inStream.readObject();
					inStream.close();

					try {
						clockSem.acquire();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					//Acquire lock for logical clock have to do it for vector as well but will do at the end
					logical_clock = logical_clock < recvMessage.getLogical_ts() ? recvMessage.getLogical_ts() : logical_clock;
					logical_clock++;
					int[] recv_vectorclock = recvMessage.getVector_ts();
					for(int i=0;i<node_count;i++) {
						vector_clock[i] = Math.max(vector_clock[i], recv_vectorclock[i]);
					}
					vector_clock[nodeId]++;
					//Release lock
					clockSem.release();
					
					switch(recvMessage.getMessageType()) {
					case REQUEST_MSG_TYPE:
						processRequest(recvMessage);
						break;
					case LOCK_MSG_TYPE:
						processLock(recvMessage);
						break;
					case RELEASE_MSG_TYPE:
						processRelease();
						break;
					case INQUIRE_MSG_TYPE:
						processInquire(recvMessage);
						break;
					case YIELD_MSG_TYPE:
						processYield();
						break;
					case FAILED_MSG_TYPE:
						processFailed();
						break;
					case DONE_MSG_TYPE:
						recvDone++;
						break;
					}
					sock.close();
				}
			} catch(IOException | ClassNotFoundException ex) {
				System.out.println("port "+port+" already in use NodeId "+nodeId);
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
		}

		private void processLock(Message lockMessage) {
			count_lock_msg_recv++;
			if(count_lock_msg_recv == quorum_members.size()) {
				csSem.release();
				count_lock_msg_recv = 0;
				recvFailed_msg = false; // intialize recvFailed to false when received grant form all
				recv_inquire_list = null; //initialise to null if we receive Inquire from some node and then lock from all the quorum members
			}
		}

		private void processInquire(Message inquireMessage) {
			try {
				incsSem.acquire();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(!inCS) {
				incsSem.release();
				if(recvFailed_msg) {
					// send yield message to particular node id
					send(inquireMessage.getID(), YIELD_MSG_TYPE);
					count_lock_msg_recv--;
				} else {
					if(recv_inquire_list == null) recv_inquire_list = new ArrayList<Integer>();
					recv_inquire_list.add(inquireMessage.getID());
				}
			}
			else 
				incsSem.release();
		}

		private void processFailed() {
			recvFailed_msg = true;
			if(recv_inquire_list!= null) {			
				// send yield message by creating a thread to all the nodes in the list
				for(int i=0;i<recv_inquire_list.size();i++) {
					send(recv_inquire_list.get(i), YIELD_MSG_TYPE);
				}
				count_lock_msg_recv = count_lock_msg_recv - recv_inquire_list.size();
				recv_inquire_list = null;
			}
		}


		private void processRequest(Message recvMessage) {
			RequestTimestamp recvTimestamp = new RequestTimestamp(recvMessage.getLogical_ts(), recvMessage.getID());
			if(locked == null) {
				locked = new RequestTimestamp(recvMessage.getLogical_ts(), recvMessage.getID());
				// send lock to recv request message
				send(locked.nodeId, LOCK_MSG_TYPE);
			} else {
				if(locked.compareTo(recvTimestamp) > 0) {
					if(!sent_inquire) {
						// send inquire message to locked node
						send(locked.nodeId, INQUIRE_MSG_TYPE);
						sent_inquire = true;
					}
				} else {
					send(recvMessage.getID(), FAILED_MSG_TYPE);
					// send failed to recv request node
				}
				queue.add(recvTimestamp);
			}
		}

		private void processYield() {
			sent_inquire = false;
			queue.add(locked);
			if(!queue.isEmpty()) {
				locked = queue.poll();
				// sent lock message to locked node
				send(locked.nodeId, LOCK_MSG_TYPE);
			}
		}

		private void processRelease() {
			sent_inquire = false;
			if(!queue.isEmpty()) {
				locked = queue.poll();
				send(locked.nodeId, LOCK_MSG_TYPE);
				// sent lock message to locked node			
			}		
		}

	}


}