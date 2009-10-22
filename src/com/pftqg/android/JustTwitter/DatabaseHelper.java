package com.pftqg.android.JustTwitter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.codehaus.jackson.JsonNode;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {
	private static String LOG_TAG="com.pftqg.android.JustTwitter.DatabaseHelper";

	public SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	public SimpleDateFormat jsonDateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss ZZZZ yyyy");
	
	private Context context;
	
	public DatabaseHelper(Context context) {
		super(context, "twittercache.jtc", null, 16);
		this.context = context;
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		migrate_2_createBase(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if(oldVersion < 16) {
			undo_2_createBase(db);
			undo_3_addHoldingForNonTimelineTweets(db);
			migrate_2_createBase(db);
			migrate_3_addHoldingForNonTimelineTweets(db);
		}
	}
	
	public void undo_2_createBase(SQLiteDatabase db) {
		db.execSQL("DROP TABLE cache_users");
		db.execSQL("DROP TABLE cache_status");
		db.execSQL("DROP TABLE cache_dm");
	}

	public void migrate_2_createBase(SQLiteDatabase db) {
		
		// Create user table
		db.execSQL("CREATE TABLE cache_users (" +
				"id BIGINT PRIMARY KEY," +
				"description TEXT," +
				"favourites INTEGER," +
				"followers INTEGER," +
				"friends INTEGER," +
				"location TEXT," +
				"name TEXT," +
				"profile_image_url TEXT," +
				"screen_name TEXT, " +
				"statuses INTEGER, " +
				"url TEXT, " +
				"utc_offset INTEGER, " +
				"protected INTEGER" +
				")");
		
		//  Create the status table
		
		db.execSQL("CREATE TABLE cache_status (" +
				"id BIGINT PRIMARY KEY," +
				"user_id INTEGER," +
				"created_at DATETIME," +
				"in_reply_to_user_id INTEGER," +
				"in_reply_to_status_id INTEGER," +
				"source TEXT," +
				"status_text TEXT," +
				"favourited INTEGER," +
				"truncated INTEGER," +
				"is_mention INTEGER," +
				"viewed INTEGER" +
				")");
		
		//  Create the DM table
		
		db.execSQL("CREATE TABLE cache_dm (" +
				"id BIGINT PRIMARY KEY," +
				"created_at DATETIME," +
				"recipient_id INTEGER," +
				"sender_id INTEGER," +
				"dm_text TEXT," +
				"was_received INTEGER," +
				"viewed INTEGER" + 
				")");
	}
	
	public void undo_3_addHoldingForNonTimelineTweets(SQLiteDatabase db) {
		db.execSQL("DROP TABLE cache_status_others");		
	}
	
	public void migrate_3_addHoldingForNonTimelineTweets(SQLiteDatabase db) {
		//  Create the status table
		db.execSQL("CREATE TABLE cache_status_others (" +
				"id BIGINT PRIMARY KEY," +
				"user_id INTEGER," +
				"created_at DATETIME," +
				"in_reply_to_user_id INTEGER," +
				"in_reply_to_status_id INTEGER," +
				"source TEXT," +
				"status_text TEXT," +
				"favourited INTEGER," +
				"truncated INTEGER," +
				"is_mention INTEGER," +
				"viewed INTEGER" +
				")");
	}

	
	public long serializeUser(JsonNode u) {
		SQLiteDatabase db = this.getWritableDatabase();
		long id = u.get("id").getLongValue();

		boolean isNewUser = !countIsNonZero(db, "cache_users WHERE id = ?", 
				Long.toString(id));

		ArrayList<Object> storeUser = new ArrayList<Object>();
		if(isNewUser)
			storeUser.add(id);

		storeUser.add(u.get("description").getTextValue());
		storeUser.add(u.get("favourites_count").getLongValue());
		storeUser.add(u.get("followers_count").getLongValue());
		storeUser.add(u.get("friends_count").getLongValue());
		storeUser.add(u.get("location").getTextValue());
		storeUser.add(u.get("name").getTextValue());
		storeUser.add(u.get("profile_image_url").getTextValue());
		storeUser.add(u.get("screen_name").getTextValue());
		storeUser.add(u.get("statuses_count").getLongValue());
		storeUser.add(u.get("url").getTextValue());
		storeUser.add(u.get("utc_offset").getLongValue());
		storeUser.add(u.get("protected").getBooleanValue() ? 1L : 0L);
		
		if(!isNewUser)
			storeUser.add(id);

		String sql;
		if(isNewUser) {
			sql = "INSERT INTO cache_users VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		} else {
			sql = "UPDATE cache_users SET description = ?," +
					"favourites = ?, followers = ?, " +
					"friends = ?, location = ?, name = ?, profile_image_url = ?, " +
					"screen_name = ?, statuses = ?, url = ?, utc_offset = ?, protected = ? " +
					"WHERE id = ?";
		}
		db.execSQL(sql, storeUser.toArray());
		return id;
	}
	
	public String[] singleStringArray(String entry) {
		String[] arr = new String[1];
		arr[0] = entry;
		return arr;
	}
	
	public boolean countIsNonZero(SQLiteDatabase db, String query, String attribute) {
		Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + query, singleStringArray(attribute));
		boolean isNonZero = false;
		if(c.moveToFirst()) {
			if(c.getInt(0) > 0)
				isNonZero = true;
		}
		c.close();
		return isNonZero;
	}
	
	public long latestStatus(boolean isMention) {
		SQLiteDatabase db = this.getWritableDatabase();	
		String sql = "SELECT MAX(id) FROM cache_status WHERE is_mention = ?";
		Cursor c = db.rawQuery(sql, singleStringArray(isMention ? "1" : "0"));
		if(c.moveToFirst()) {
			long l = c.getLong(0);
			c.close();
			return l;
		}
		c.close();
		return 0;
	}
	
	public long latestDM(boolean wasReceived) {
		SQLiteDatabase db = this.getWritableDatabase();	
		String sql = "SELECT MAX(id) FROM cache_dm WHERE was_received = ?";
		Cursor c = db.rawQuery(sql, singleStringArray(wasReceived ? "1" : "0"));
		if(c.moveToFirst()) {
			long l = c.getLong(0);
			c.close();
			return l;
		}
		c.close();
		return 0;
	}
	
	public long serializeMention(JsonNode s) {
		SQLiteDatabase db = this.getWritableDatabase();
		
		Long id = s.get("id").getLongValue();

		boolean isNewStatus = !countIsNonZero(db, "cache_status WHERE id = ?", 
				Long.toString(id));
		
		ArrayList<Object>storeStatus = new ArrayList<Object>();
		if(isNewStatus)
			storeStatus.add(id);
		storeStatus.add((long)this.serializeUser(s.get("user")));
		storeStatus.add(sdf.format(parseJsonDate(s.get("created_at").getTextValue())));
		storeStatus.add(s.get("in_reply_to_user_id").getLongValue());
		storeStatus.add(s.get("in_reply_to_status_id").getLongValue());
		storeStatus.add(s.get("source").getTextValue());
		storeStatus.add(s.get("text").getTextValue());
		storeStatus.add(s.get("favorited").getBooleanValue() ? 1L : 0L);
		storeStatus.add(s.get("truncated").getBooleanValue() ? 1L : 0L);
		if(!isNewStatus)
			storeStatus.add(id);

		String sql;
		if(isNewStatus) {
			sql = "INSERT INTO cache_status VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 0)";
		} else {
			sql = "UPDATE cache_status SET user_id = ?, created_at = ?, " +
					"in_reply_to_user_id = ?, in_reply_to_status_id = ?, " +
					"source = ?, status_text = ?, favourited = ?, truncated = ?, is_mention = 1 " +
					"WHERE id = ?";
		}
		
		db.execSQL(sql, storeStatus.toArray());
		return id;
	}
	
	public long serializeStatus(JsonNode s) {
		SQLiteDatabase db = this.getWritableDatabase();
		
		Long id = s.get("id").getLongValue();

		boolean isNewStatus = !countIsNonZero(db, "cache_status WHERE id = ?", 
				Long.toString(id));
		
		ArrayList<Object>storeStatus = new ArrayList<Object>();
		if(isNewStatus)
			storeStatus.add(id);
		storeStatus.add((long)this.serializeUser(s.get("user")));
		storeStatus.add(sdf.format(parseJsonDate(s.get("created_at").getTextValue())));
		storeStatus.add(s.get("in_reply_to_user_id").getLongValue());
		storeStatus.add(s.get("in_reply_to_status_id").getLongValue());
		storeStatus.add(s.get("source").getTextValue());
		storeStatus.add(s.get("text").getTextValue());
		storeStatus.add(s.get("favorited").getBooleanValue() ? 1L : 0L);
		storeStatus.add(s.get("truncated").getBooleanValue() ? 1L : 0L);
		if(!isNewStatus)
			storeStatus.add(id);
		else
			storeStatus.add(0L);

		String sql;
		if(isNewStatus) {
			sql = "INSERT INTO cache_status VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)";
		} else {
			sql = "UPDATE cache_status SET user_id = ?, created_at = ?, " +
					"in_reply_to_user_id = ?, in_reply_to_status_id = ?, " +
					"source = ?, status_text = ?, favourited = ?, truncated = ? " +
					"WHERE id = ?";
		}
		db.execSQL(sql, storeStatus.toArray());
		return id;
	}

	public long serializeStatusOther(JsonNode s) {
		SQLiteDatabase db = this.getWritableDatabase();
		
		Long id = s.get("id").getLongValue();

		boolean isNewStatus = !countIsNonZero(db, "cache_status_others WHERE id = ?", 
				Long.toString(id));
		
		ArrayList<Object>storeStatus = new ArrayList<Object>();
		if(isNewStatus)
			storeStatus.add(id);
		storeStatus.add((long)this.serializeUser(s.get("user")));
		storeStatus.add(sdf.format(parseJsonDate(s.get("created_at").getTextValue())));
		storeStatus.add(s.get("in_reply_to_user_id").getLongValue());
		storeStatus.add(s.get("in_reply_to_status_id").getLongValue());
		storeStatus.add(s.get("source").getTextValue());
		storeStatus.add(s.get("text").getTextValue());
		storeStatus.add(s.get("favorited").getBooleanValue() ? 1L : 0L);
		storeStatus.add(s.get("truncated").getBooleanValue() ? 1L : 0L);
		if(!isNewStatus)
			storeStatus.add(id);
		else
			storeStatus.add(0L);

		String sql;
		if(isNewStatus) {
			sql = "INSERT INTO cache_status_others VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)";
		} else {
			sql = "UPDATE cache_status_others SET user_id = ?, created_at = ?, " +
					"in_reply_to_user_id = ?, in_reply_to_status_id = ?, " +
					"source = ?, status_text = ?, favourited = ?, truncated = ? " +
					"WHERE id = ?";
		}
		db.execSQL(sql, storeStatus.toArray());
		return id;
	}
	
	public long serializeDM(JsonNode dm, boolean wasReceived) {
		SQLiteDatabase db = this.getWritableDatabase();
		
		Long id = dm.get("id").getLongValue();		
		
		boolean isNewDM = !countIsNonZero(db, "cache_dm WHERE id = ?", 
				Long.toString(id));
		ArrayList<Object>storeDM = new ArrayList<Object>();
		if(isNewDM)
			storeDM.add(id);
		storeDM.add(sdf.format(parseJsonDate(dm.get("created_at").getTextValue())));
		storeDM.add((long)this.serializeUser(dm.get("recipient")));
		storeDM.add((long)this.serializeUser(dm.get("sender")));
		storeDM.add(dm.get("text").getTextValue());
		storeDM.add(wasReceived ? 1L : 0L);
		if(!isNewDM)
			storeDM.add(id);

		String sql;
		if(isNewDM) {
			sql = "INSERT INTO cache_dm VALUES (?, ?, ?, ?, ?, ?, 0)";
		} else {
			sql = "UPDATE cache_dm SET created_at = ?, recipient_id = ?, " +
					"sender_id = ?, dm_text = ?, was_received = ?" +
					"WHERE id = ?";
		}
		db.execSQL(sql, storeDM.toArray());
				
		return id;
	}
	
	public void markAllDMsViewed() {
		SQLiteDatabase db = this.getWritableDatabase();
		String sql = "UPDATE cache_dm SET viewed = 1 WHERE viewed = 0";
		db.execSQL(sql);
		context.sendBroadcast(new Intent().setAction("com.pftqg.android.JustTwitter.RefreshAllDMs"));
	}	

	public void markAllStatusesViewed() {
		SQLiteDatabase db = this.getWritableDatabase();
		String sql = "UPDATE cache_status SET viewed = 1 WHERE is_mention = 0 AND viewed = 0";
		db.execSQL(sql);
		context.sendBroadcast(new Intent().setAction("com.pftqg.android.JustTwitter.RefreshAllStatuses"));
	}

	public void markAllMentionsViewed() {
		SQLiteDatabase db = this.getWritableDatabase();
		String sql = "UPDATE cache_status SET viewed = 1 WHERE is_mention = 1 AND viewed = 0";
		db.execSQL(sql);
		context.sendBroadcast(new Intent().setAction("com.pftqg.android.JustTwitter.RefreshAllMentions"));
	}

	public DBUser getUser(long user_id) {
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor c = db.rawQuery("SELECT * FROM cache_users WHERE id = ?", singleStringArray(Long.toString(user_id)));
		if(!c.moveToFirst()) {
			c.close();
			return null;
		}
		DBUser du = new DBUser(this,new CursorHelper(c));
		c.close();
		return du;
	}
	
	public DBStatus getOtherStatus(long status_id) {
		SQLiteDatabase db = this.getWritableDatabase();
		
		// do we have one in other?
		
		Cursor c = db.rawQuery("SELECT * FROM cache_status_others WHERE id = ?", singleStringArray(Long.toString(status_id)));
		if(!c.moveToFirst()) {
			c.close();
			return null;
		}		
		DBStatus dbs = new DBStatus(this,new CursorHelper(c));
		c.close();
		return dbs;
	}
	
	public DBStatus getStatus(long status_id) {
		SQLiteDatabase db = this.getWritableDatabase();
		
		// do we have one in normal?
		
		Cursor c = db.rawQuery("SELECT * FROM cache_status WHERE id = ?", singleStringArray(Long.toString(status_id)));
		if(!c.moveToFirst()) {
			c.close();
			return getOtherStatus(status_id);
		}		
		DBStatus dbs = new DBStatus(this,new CursorHelper(c));
		c.close();
		return dbs;
	}

	public DBDirectMessage getDM(long dm_id) {
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor c = db.rawQuery("SELECT * FROM cache_dm WHERE id = ?", singleStringArray(Long.toString(dm_id)));
		if(!c.moveToFirst()) {
			c.close();
			return null;
		}		
		DBDirectMessage dbs = new DBDirectMessage(this,new CursorHelper(c));
		c.close();
		return dbs;
	}
	
	public List<Long> getTimeline() {
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor c = db.rawQuery("SELECT id FROM cache_status ORDER BY id DESC", null);
		ArrayList<Long> mentions = new ArrayList<Long>();
		while(c.moveToNext()) {
			mentions.add(c.getLong(0));
		}	
		c.close();
		return mentions;		
	}
	
	public List<Long> getMentions() {
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor c = db.rawQuery("SELECT id FROM cache_status WHERE is_mention = ? ORDER BY id DESC", singleStringArray("1"));
		ArrayList<Long> mentions = new ArrayList<Long>();
		while(c.moveToNext()) {
			mentions.add(c.getLong(0));
		}	
		c.close();
		return mentions;
	}
	
	public List<Long> getDMs() {
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor c = db.rawQuery("SELECT id FROM cache_dm ORDER BY id DESC", null);
		ArrayList<Long> dms = new ArrayList<Long>();
		while(c.moveToNext()) {
			dms.add(c.getLong(0));
		}	
		c.close();
		return dms;		
	}

	private Date parseJsonDate(String date) {
		try {
			return jsonDateFormat.parse(date);
		} catch(Exception ex) {
			Log.e(LOG_TAG, "parseJsonDate failed", ex);
		}
		return new Date();
	}
	
	class CursorHelper extends java.lang.Object {
		Cursor c;
		CursorHelper(Cursor newc) {
			c = newc;
		}
		public long getLong(String name) {
			return c.getLong(c.getColumnIndexOrThrow(name));
		}
		public int getInt(String name) {
			return c.getInt(c.getColumnIndexOrThrow(name));
		}
		public String getString(String name) {
			return c.getString(c.getColumnIndexOrThrow(name));
		}
		public Date getDate(String name) {
			try {
				return sdf.parse(getString(name));
			} catch (ParseException px) {
				return null;
			}
		}
		public boolean getBool(String name) {
			return c.getInt(c.getColumnIndexOrThrow(name)) == 1;
		}
	}
}
