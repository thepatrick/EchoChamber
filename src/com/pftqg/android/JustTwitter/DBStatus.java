package com.pftqg.android.JustTwitter;

import java.util.Date;

public class DBStatus extends java.lang.Object {
	@SuppressWarnings("unused")
	private static String LOG_TAG="com.pftqg.android.JustTwitter.DBStatus";
	
    /**
	 * 
	 */
	private static final long serialVersionUID = -236681668438960296L;
	private long id;
	private String text;
	private String source;
	private Date createdAt;
	private long inReplyToStatusId;
	private int inReplyToUserId;
	private boolean isFavorited;
	private int userId;
	private boolean isMention;
	private boolean isViewed;
	private DBUser user;
	private DatabaseHelper d;

	DBStatus(DatabaseHelper dr) {
		d = dr;
    }
	DBStatus(DatabaseHelper dr, DatabaseHelper.CursorHelper ch) {
		d = dr;
		id = ch.getLong("id");
		setUserId(ch.getInt("user_id"));
		createdAt = (ch.getDate("created_at"));
		inReplyToUserId = (ch.getInt("in_reply_to_user_id"));
		inReplyToStatusId = (ch.getLong("in_reply_to_status_id"));
		source = (ch.getString("source"));
		text = (ch.getString("status_text"));
		isFavorited  = (ch.getBool("favourited"));
		isMention = ch.getBool("is_mention");
		isViewed = ch.getBool("viewed");
	}

	public void setFavorited(boolean isFavorited) {
		this.isFavorited = isFavorited;
	}

	public boolean isFavorited() {
		return isFavorited;
	}

	public void setInReplyToUserId(int inReplyToUserId) {
		this.inReplyToUserId = inReplyToUserId;
	}

	public int getInReplyToUserId() {
		return inReplyToUserId;
	}

	public void setInReplyToStatusId(long inReplyToStatusId) {
		this.inReplyToStatusId = inReplyToStatusId;
	}

	public long getInReplyToStatusId() {
		return inReplyToStatusId;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getSource() {
		return source;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getText() {
		return text;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getId() {
		return id;
	}

	public void setUser(DBUser user) {
		this.user = user;
	}

	public DBUser getUser() {
		return user;
	}

	public void setUserId(int userId) {
		this.userId = userId;
		this.user = d.getUser(userId);
	}

	public int getUserId() {
		return userId;
	}

	public void setMention(boolean isMention) {
		this.isMention = isMention;
	}

	public boolean isMention() {
		return isMention;
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
