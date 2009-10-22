package com.pftqg.android.JustTwitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.commonsware.cwac.thumbnail.ThumbnailAdapter;

import android.app.Activity;
import android.database.DataSetObserver;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;

public class TweetsDataSource implements ListAdapter {
	
	@SuppressWarnings("unused")
	private static String LOG_TAG="com.pftqg.android.JustTwitter.TweetsDataSource";
    
	private Activity owner;
    
    List<Long> mentions;
    List<Long> newMentions;
    
    HashMap<Long,DBStatus> statusCache;
    
    List<DataSetObserver> observers;

    private DatabaseHelper d; 
    
    
    public TweetsDataSource(Activity newOwner, DatabaseHelper d) {
		owner = newOwner;
	    mentions = new ArrayList<Long>(); 
	    statusCache = new HashMap<Long,DBStatus>();
	    observers = new ArrayList<DataSetObserver>();
		this.d = d;
		fetchTweets();
    }

    public void onStatusesClearCache() {
    	statusCache.clear();
    	fetchTweets();
    }
    
    public void onTimelineUpdated() {
    	Log.e("Timeline/TweetsDataSource", "onTimelineUpdated!");
    	fetchTweets();
    }
    
    public void fetchTweets() {
    	mentions = d.getTimeline();
    	for(DataSetObserver d : observers) {
    		d.onChanged();
    	}
		ThumbnailAdapter ta = ((Timeline)owner).getTweetsTA();
		if(ta != null) ta.notifyDataSetChanged();
    }
    
    private DBStatus getStatus(long status_id) {
    	if(statusCache.containsKey(status_id)) {
    		return statusCache.get(status_id);
    	}
     	DBStatus dbs = d.getStatus(status_id);
     	if(dbs != null) {
     		statusCache.put(status_id, dbs);
     	}
        return dbs;
    }
    
	public boolean areAllItemsEnabled() {
		return true;
	}

	public boolean isEnabled(int arg0) {
		return true;
	}

	public DBStatus getStatusForPosition(int position) {
		return getStatus(mentions.get(position));
	}
	
	public int getCount() {
		return mentions.size();
	}

	public Object getItem(int position) {
		return mentions.get(position);
	}

	public long getItemId(int position) {
		return mentions.get(position);
	}

	public int getItemViewType(int position) {
		return 0;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		//TextView tv = (TextView) convertView;
		View row = convertView;
		TweetWrapper wrapper = null;
		if(row == null) { // || !convertView.getClass().equals(TextView.class)
			row = LayoutInflater.from(this.owner).inflate(R.layout.tweet, null).getRootView();
			wrapper = new TweetWrapper(row);
			row.setTag(wrapper);
		} else{
			wrapper = (TweetWrapper)row.getTag();
		}
		wrapper.populateFrom(getStatus(mentions.get(position)));
		return row;
	}

	public int getViewTypeCount() {
		return 1;
	}

	public boolean hasStableIds() {
		return true;
	}

	public boolean isEmpty() {
		return mentions.isEmpty();
	}

	public void registerDataSetObserver(DataSetObserver observer) {
		observers.add(observer);
	}

	public void unregisterDataSetObserver(DataSetObserver observer) {
		observers.remove(observers);
	}

	public void setOwner(Activity owner) {
		this.owner = owner;
	}

	public Activity getOwner() {
		return owner;
	}

}
