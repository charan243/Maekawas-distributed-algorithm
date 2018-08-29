package com.aos;

public class csExecutionTimestamp {
		public int nodeId;
		public int[] startTime;
		public int[] endTime;
		public int reqs_done;
		public csExecutionTimestamp(int nodeId,int[] startTime,int[] endTime,int reqs_done) {
			this.nodeId = nodeId;
			this.startTime = startTime;
			this.endTime = endTime;
			this.reqs_done = reqs_done;
		}
}
