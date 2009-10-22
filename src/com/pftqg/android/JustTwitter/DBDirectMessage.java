package com.pftqg.android.JustTwitter;

import java.util.Date;

public class DBDirectMessage extends java.lang.Object {
	@SuppressWarnings("unused")
	private static String LOG_TAG="com.pftqg.android.JustTwitter.DBDirectMessage";

	private long id;
	private String text;
	private Date createdAt;

	private long recipientId;
	private DBUser recipient;

	private boolean wasReceived;
	
	private boolean isViewed;
	
	private long senderId;

	private DBUser sender;

	private DatabaseHelper d;
	
	DBDirectMessage(DatabaseHelper dr) {
		d = dr;
    }
	DBDirectMessage(DatabaseHelper dr, DatabaseHelper.CursorHelper ch) {
		d = dr;
		id = ch.getLong("id");
		createdAt = (ch.getDate("created_at"));
		text = (ch.getString("dm_text"));
		setRecipientId(ch.getInt("recipient_id"));
		setSenderId(ch.getInt("sender_id"));
		wasReceived = ch.getBool("was_received");
		isViewed = ch.getBool("viewed");
	}
	
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	public long getRecipientId() {
		return recipientId;
	}

	public void setRecipientId(long recipientId) {
		this.recipient = d.getUser(recipientId);
		this.recipientId = recipientId;
	}

	public DBUser getRecipient() {
		return recipient;
	}

	public void setRecipient(DBUser recipient) {
		this.recipientId = recipient.getId();
		this.recipient = recipient;
	}

	public long getSenderId() {
		return senderId;
	}

	public void setSenderId(long senderId) {
		this.sender = d.getUser(senderId);
		this.senderId = senderId;
	}

	public DBUser getSender() {
		return sender;
	}

	public void setSender(DBUser sender) {
		this.senderId = sender.getId();
		this.sender = sender;
	}

	public void setWasReceived(boolean wasReceived) {
		this.wasReceived = wasReceived;
	}
	
	public boolean isWasReceived() {
		return wasReceived;
	}
	
	public DatabaseHelper getDatabaseHelper() {
		return d;
	}
	public void setViewed(boolean isViewed) {
		this.isViewed = isViewed;
	}
	public boolean isViewed() {
		return isViewed;
	}
	
}
