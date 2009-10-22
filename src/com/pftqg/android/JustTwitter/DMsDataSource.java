package com.pftqg.android.JustTwitter;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.database.DataSetObserver;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;

import com.commonsware.cwac.thumbnail.ThumbnailAdapter;

public class DMsDataSource implements ListAdapter {
	private static final String LOG_TAG = "com.pftqg.android.JustTwitter.DMsDataSource";
    
	private Activity owner;
	
    private DataSetObserver observedby;
    
    List<Long> mentions;
    
    HashMap<Long,DBDirectMessage> statusCache;

    private DatabaseHelper d; 
    
    public DMsDataSource(Activity newOwner, DatabaseHelper d) {
		owner = newOwner;
	    mentions = new ArrayList<Long>(); 
	    statusCache = new HashMap<Long,DBDirectMessage>();
		this.d = d;
		fetchTweets();
    }
    
    public void onDMsClearCache() {
    	statusCache.clear();
    	fetchTweets();
    }
    
    public void onDMsUpdated() {
    	Log.e(LOG_TAG, "onDMsUpdated!");
    	fetchTweets();
    }
    
    public void fetchTweets() {
    	mentions = d.getDMs();
		if(observedby != null) {
			observedby.onChanged();
		}		
		ThumbnailAdapter ta = ((Timeline)owner).getDMsTA();
		if(ta != null) ta.notifyDataSetChanged();
    }
    
    private DBDirectMessage getStatus(long status_id) {
    	if(statusCache.containsKey(status_id)) {
    		return statusCache.get(status_id);
    	}
    	DBDirectMessage dbs = d.getDM(status_id);
     	if(dbs != null) {
     		statusCache.put(status_id, dbs);
     	}
        return dbs;
    }
    
    public String formatDate(Date date) {
    	return d.sdf.format(date);
    }
    
	public boolean areAllItemsEnabled() {
		return true;
	}

	public boolean isEnabled(int arg0) {
		return true;
	}

	public DBDirectMessage getStatusForPosition(int position) {
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
		View row = convertView;
		DMWrapper wrapper = null;
		if(row == null) {
			row = LayoutInflater.from(this.owner).inflate(R.layout.tweet, null).getRootView();
			wrapper = new DMWrapper(row);
			row.setTag(wrapper);
		} else{
			wrapper = (DMWrapper)row.getTag();
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
		Log.d(LOG_TAG, "registerDataSetObserver: " + observer.toString());
		observedby = observer;
	}

	public void unregisterDataSetObserver(DataSetObserver observer) {
		Log.d(LOG_TAG, "unregisterDataSetObserver: " + observer.toString());
		observedby = null;		
	}

	public void setOwner(Activity owner) {
		this.owner = owner;
	}

	public Activity getOwner() {
		return owner;
	}

}
