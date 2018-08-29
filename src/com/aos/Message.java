package com.aos;

import java.io.Serializable;

public class Message implements Serializable {
	private int Id;
	private int MessageType;
	private int logical_ts;
	private int[] vector_ts;
	private int req_number;
	
	public Message(Integer Id,int MessageType,int logical_ts,int[] vector_ts,int req_number) {
		this.Id = Id;
		this.MessageType = MessageType;
		this.logical_ts = logical_ts;
		this.vector_ts = vector_ts;
		this.req_number = req_number;
	}
	public int getMessageType() {
		return MessageType;
	}
	public int getID() {
		return Id;
	}
	public int getLogical_ts() {
		return logical_ts;
	}
	public int[] getVector_ts() {
		return vector_ts;
	}
	public int getReq_number() {
		return req_number;
	}
	
}
