package com.aos;

public class RequestTimestamp implements Comparable<RequestTimestamp>{
	public int logical_ts;
	public int nodeId;
	public RequestTimestamp(int logical_ts,int nodeId) {
		this.logical_ts = logical_ts;
		this.nodeId = nodeId;
	}
	public int getLogical_ts() {
		return logical_ts;
	}
	public int getNodeId() {
		return nodeId;
	}
	@Override
	public int compareTo(RequestTimestamp o) {
		// TODO Auto-generated method stub
		if(o.logical_ts == logical_ts) {
			return Integer.compare(nodeId, o.nodeId);
		} else {
			return Double.compare(logical_ts, o.logical_ts);
		}
	}
	
}
