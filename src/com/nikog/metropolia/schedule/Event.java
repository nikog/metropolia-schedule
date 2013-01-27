package com.nikog.metropolia.schedule;

public class Event {
	private String subject;
	private long start;
	private long end;
	private String roomId;
	
	public Event(String subject, long start, long end, String roomId) {
		this.subject = subject;
		this.start = start;
		this.end = end;
		this.roomId = roomId;
	}
	
	public String getSubject() {
		return subject;
	}
	public long getStart() {
		return start;
	}
	public long getEnd() {
		return end;
	}
	public String getRoomId() {
		return roomId;
	}
	
	public void setSubject(String subject) {
		this.subject = subject;
	}
	public void setStart(long start) {
		this.start = start;
	}
	public void setEnd(long end) {
		this.end = end;
	}
	public void setRoomId(String roomId) {
		this.roomId = roomId;
	}

	@Override
	public String toString() {
		return "Event [subject=" + subject + ", start=" + start + ", end=" + end + ", roomId=" + roomId + "]";
	}
	
}
