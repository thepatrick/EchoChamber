package com.pftqg.android.JustTwitter;

public class DBUser extends java.lang.Object {
	@SuppressWarnings("unused")
	private static String LOG_TAG="com.pftqg.android.JustTwitter.DBUser";

	private DatabaseHelper d;


	private long id;
	private String description;
	private int favourites;
	private int followers;
	private int friends;
	private String location;
	private String name;
	private String profileImageURL;
	private String screenName;
	private int statuses;
	private String url;
	private int utcOffset;
	private boolean isProtected;
	
	DBUser(DatabaseHelper d) {
		this.setD(d);
	}
	
	DBUser(DatabaseHelper d, DatabaseHelper.CursorHelper c) {
		this.setD(d);
		
		id = c.getLong("id");
		description = c.getString("description");
		favourites = c.getInt("favourites");
		followers = c.getInt("followers");
		friends = c.getInt("friends");
		location = c.getString("location");
		name = c.getString("name");
		profileImageURL = c.getString("profile_image_url");
		screenName = c.getString("screen_name");
		statuses = c.getInt("statuses");
		url = c.getString("url");
		utcOffset = c.getInt("utc_offset");
		isProtected = c.getBool("protected");
	}
	
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public int getFavourites() {
		return favourites;
	}

	public void setFavourites(int favourites) {
		this.favourites = favourites;
	}

	public int getFollowers() {
		return followers;
	}

	public void setFollowers(int followers) {
		this.followers = followers;
	}

	public int getFriends() {
		return friends;
	}

	public void setFriends(int friends) {
		this.friends = friends;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getProfileImageURL() {
		return profileImageURL;
	}

	public void setProfileImageURL(String profileImageURL) {
		this.profileImageURL = profileImageURL;
	}

	public String getScreenName() {
		return screenName;
	}

	public void setScreenName(String screenName) {
		this.screenName = screenName;
	}

	public int getStatuses() {
		return statuses;
	}

	public void setStatuses(int statuses) {
		this.statuses = statuses;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public int getUtcOffset() {
		return utcOffset;
	}

	public void setUtcOffset(int utcOffset) {
		this.utcOffset = utcOffset;
	}

	public boolean isProtected() {
		return isProtected;
	}

	public void setProtected(boolean isProtected) {
		this.isProtected = isProtected;
	}

	// These are just to shut the damn thing up.
	public void setD(DatabaseHelper d) {
		this.d = d;
	}

	public DatabaseHelper getD() {
		return d;
	}
	
}
