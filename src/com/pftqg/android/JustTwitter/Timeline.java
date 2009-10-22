package com.pftqg.android.JustTwitter;

import android.app.TabActivity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TabHost.OnTabChangeListener;

import com.commonsware.cwac.thumbnail.ThumbnailAdapter;

public class Timeline extends TabActivity {
	
	private static final String packageName = "com.pftqg.android.JustTwitter";
	private static final String sourcingName = "com.pftqg.android.JustTwitter.TwitterSourcing";

	private static final int[] IMAGE_IDS={R.id.avatar};
	
	private static final String LOG_TAG = packageName + ".Timeline";
	private ITwitterSourcingService twitterService;
	private CounterServiceConnection conn;
	private boolean started = false;
	private Handler mHandler = null;
	
    private SharedPreferences mPrefs;
    
    private MentionsDataSource mentionsDS;
    private TweetsDataSource tweetsDS;
    private DMsDataSource DMsDS;
    
    private ThumbnailAdapter mentionsTA;
    private ThumbnailAdapter tweetsTA;
    private ThumbnailAdapter DMsTA;

	private DatabaseHelper d;
    
	private TabHost.TabSpec newTab(String tag, int view, String indicator, int icon) {
		return getTabHost().newTabSpec(tag).setContent(view).setIndicator(indicator, getResources().getDrawable(icon));	
	}
	
	private void configureTabs() {
		getTabHost().addTab(newTab("tweets", R.id.timeline, "Friends", android.R.drawable.sym_action_chat));
		getTabHost().addTab(newTab("mentions", R.id.mentions, "Mentions", android.R.drawable.sym_action_chat));
		getTabHost().addTab(newTab("dms", R.id.directmessages, "DMs", android.R.drawable.sym_action_email));
		
		getTabHost().setCurrentTab(mPrefs.getInt("view_mode", 0));		
		
		getTabHost().setOnTabChangedListener(new OnTabChangeListener(){
			public void onTabChanged(String arg0) {
//				Log.d(LOG_TAG, "Clicked on the tab widget, should go to ... " + arg0);
				if(arg0.equals("tweets")) {
					tweetsDS.fetchTweets();
				}
				if(arg0.equals("mentions")) {
					mentionsDS.fetchTweets();
				}
				if(arg0.equals("dms")) {
					DMsDS.fetchTweets();
				}
			}
		});
	}
	
	public ThumbnailAdapter getMentionsTA() {
		return mentionsTA;
	}

	public ThumbnailAdapter getTweetsTA() {
		return tweetsTA;
	}

	public ThumbnailAdapter getDMsTA() {
		return DMsTA;
	}
	
	private ListView mentions;
	private ListView timeline;
	private ListView directmessages;
	
	/* <lifecycle> */
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.main);
		
        d = new DatabaseHelper(this);
		
        mPrefs = ((Application)getApplication()).getPreferences();
        
        mentionsDS = new MentionsDataSource(this,d);
        tweetsDS = new TweetsDataSource(this,d);
        DMsDS = new DMsDataSource(this,d);

        mentionsTA = new ThumbnailAdapter(this, mentionsDS, ((Application)getApplication()).getCache(),	IMAGE_IDS);
        tweetsTA = new ThumbnailAdapter(this, tweetsDS, ((Application)getApplication()).getCache(),	IMAGE_IDS);
        DMsTA = new ThumbnailAdapter(this, DMsDS, ((Application)getApplication()).getCache(),	IMAGE_IDS);
        
        mentions = (ListView)findViewById(R.id.mentions);
        timeline = (ListView)findViewById(R.id.timeline);
        directmessages = (ListView)findViewById(R.id.directmessages);
        
        mentions.setAdapter(mentionsTA);
        timeline.setAdapter(tweetsTA);
        directmessages.setAdapter(DMsTA);

        registerForContextMenu(mentions);
        registerForContextMenu(timeline);
        registerForContextMenu(directmessages);
        
        timeline.setOnItemClickListener(new OnItemClickListener(){
			public void onItemClick(AdapterView<?> parent, View view, int position, long rowId) {
				viewTweet(rowId);	
			}
        });
        mentions.setOnItemClickListener(new OnItemClickListener(){
			public void onItemClick(AdapterView<?> parent, View view, int position, long rowId) {
				viewTweet(rowId);	
			}
        });
        
        configureTabs();
        
		startSourcing();
		mHandler = new Handler();
    }
    
    @Override
    public void onDestroy() {
    	//stopSourcing();
    	releaseSourcing();
    	d.close();
    	super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        initSourcing();
		IntentFilter f = new IntentFilter();
		f.addAction("com.pftqg.android.JustTwitter.MentionsUpdate");
		f.addAction("com.pftqg.android.JustTwitter.TimelineUpdate");
		f.addAction("com.pftqg.android.JustTwitter.DMSUpdate");
		f.addAction("com.pftqg.android.JustTwitter.BackgroundWorking");
		f.addAction("com.pftqg.android.JustTwitter.BackgroundDone");
		f.addAction("com.pftqg.android.JustTwitter.RefreshAllDMs");
		f.addAction("com.pftqg.android.JustTwitter.RefreshAllStatuses");
		f.addAction("com.pftqg.android.JustTwitter.RefreshAllMentions");
		this.registerReceiver(serviceUpdates, f);
		try {
			if(twitterService != null)
				this.setProgressBarIndeterminateVisibility(twitterService.isBackgroundWorking());
		} catch (RemoteException e) {
			Log.e(LOG_TAG,"twitterService.isBackgroundWorking() failed!", e);
		}
		
		Application a = (Application)getApplication();
		if(!a.isLoggedIn()) {
	    	startActivity(new Intent(this, LoginActivity.class));
		}
    }

    @Override
    public void onResume() {
    	super.onResume();
    	setProgressBarIndeterminateVisibility(false);
		try {
			if(twitterService != null)
				this.setProgressBarIndeterminateVisibility(twitterService.isBackgroundWorking());
		} catch (RemoteException e) {
			Log.e(LOG_TAG,"twitterService.isBackgroundWorking() failed!", e);
		}
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    	//this.unregisterReceiver(serviceUpdates);
    	SharedPreferences.Editor ed = mPrefs.edit();
    	ed.putInt("view_mode", getTabHost().getCurrentTab());
    	ed.commit();
    }
    /* </lifecycle> */


    // Menu item ids
    public static final int MENU_ITEM_REFRESH  = Menu.FIRST;
    public static final int MENU_ITEM_POST     = Menu.FIRST + 1;
    public static final int MENU_ITEM_SETTINGS = Menu.FIRST + 2;
    public static final int MENU_ITEM_VIEWED   = Menu.FIRST + 3;
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        
        menu.add(0, MENU_ITEM_REFRESH, 0, R.string.menu_refresh).setShortcut('1', 'r')
        	.setIcon(android.R.drawable.ic_menu_revert);
        
        menu.add(0, MENU_ITEM_SETTINGS, 0, R.string.menu_settings).setShortcut('2', 's')
        	.setIcon(android.R.drawable.ic_menu_preferences);
        
        menu.add(0, MENU_ITEM_VIEWED, 0, R.string.menu_viewed).setShortcut('3', 'v')
    	.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
        
        Intent intent = new Intent(null, getIntent().getData());
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, Timeline.class), null, intent, 0, null);
        
        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        final String tab = getTabHost().getCurrentTabTag();
        final boolean showPost = (tab.equals("tweets") || tab.equals("mentions"));
        if(showPost) {
        	menu.removeItem(MENU_ITEM_POST);
        	menu.add(0, MENU_ITEM_POST, 0, R.string.menu_post).setShortcut('4', 'p')
        		.setIcon(android.R.drawable.ic_menu_edit);
        } else {
        	menu.removeItem(MENU_ITEM_POST);
        }
        
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final String tab = getTabHost().getCurrentTabTag();
        switch (item.getItemId()) {
	        case MENU_ITEM_REFRESH:
	            // Launch activity to insert a new item
	        	try {
		        	twitterService.refreshTweets();
		        	twitterService.refreshMentions();
		        	twitterService.refreshDMs();
	        	} catch(RemoteException ex) {
	        		Log.e(LOG_TAG, "CounterService failed.", ex);
	        	}
	            return true;
	        case MENU_ITEM_SETTINGS:
		    	startActivity(new Intent(this, Preferences.class));
	        	return true;
	        case MENU_ITEM_POST:
	        	newPosting("", false, 0, null);
	        	return true;
	        case MENU_ITEM_VIEWED:
	        	if((tab.equals("tweets") || tab.equals("mentions"))) {
	        		d.markAllMentionsViewed();
	        		d.markAllStatusesViewed();
	        	} else {
	        		d.markAllDMsViewed();
	        	}
	        	return true;
        }
        return super.onOptionsItemSelected(item);
    }
    

    private void viewTweet(long messageId) {
    	Bundle b = new Bundle();
    	b.putLong("tweet_id", messageId);
    	startActivity(new Intent(this, TweetViewer.class).putExtras(b));
    }
    
    private void newPosting(String defaultText, boolean isReply, long inReplyTo, String screenName) {
    	Bundle b = new Bundle();
    	b.putBoolean("isReply", isReply);
    	b.putString("defaultText", defaultText);
    	if(isReply)
    		b.putLong("inReplyTo", inReplyTo);
    	if(isReply)
    		b.putString("screenName", screenName);
        startActivity(new Intent(this, TweetComposer.class).putExtras(b));
    }
    
    private void newDMComposer(long inReplyTo, String screenName) {
    	Bundle b = new Bundle();
    	b.putBoolean("isDirectMessage", true);
		b.putLong("inReplyTo", inReplyTo);
		b.putString("screenName", screenName);
        startActivity(new Intent(this, TweetComposer.class).putExtras(b));
    }
    
    /**
     * Bind to a sourcing service - it should already be started!
     */
    private void initSourcing() {
    	if(conn == null) {
    		conn = new CounterServiceConnection();
    		Intent i = new Intent().setClassName(packageName, sourcingName);
    		bindService(i, conn, Context.BIND_AUTO_CREATE);
    		updateServiceStatus();
    		Log.d(LOG_TAG, "initSourcing()!");
    	}
    }
    
    /**
     * Unbind from the sourcing service
     */
    private void releaseSourcing() {
    	if(conn != null) {
    		unbindService(conn);
    		conn = null;
    		updateServiceStatus();
    		Log.d(LOG_TAG, "releaseSourcing()");
    	}
    }

    /**
     * Start the sourcing service (if it hasn't already)
     */
    private void startSourcing() {
    	if( !started ) { 
    		Intent i = new Intent().setClassName(packageName, sourcingName);
    		startService(i);
    		Log.d(LOG_TAG,"startSourcing!");
    		started = true;
    		updateServiceStatus();
    	}
    }

    /**
     * Stop the sourcing service (time to quit?)
     */
    @SuppressWarnings("unused")
	private void stopSourcing() {
    	if(!started) {
    		// toast already done
    	} else {
    		Intent i = new Intent().setClassName(packageName, sourcingName);
    		stopService(i);
    		Log.d(LOG_TAG, "stopSourcing()");
    		started = false;
    		updateServiceStatus();
    	}
    }
    
    /**
     * Convenience function to get the connection 
     * (if it is currently valid)
     */
    public ITwitterSourcingService getConnection() {
    	if(conn == null || twitterService == null) 
    		return null;
    	return twitterService;
    }
    
    /**
     * Left over sample code
     */
    private void updateServiceStatus() {}
    
    /**
     * Callback class to handle the sourcing service being connected
     * and disconnected (disconnected likely because it's crashed?)
     */
    class CounterServiceConnection implements ServiceConnection {
        public void onServiceConnected(ComponentName className,	IBinder boundService ) {
          twitterService = ITwitterSourcingService.Stub.asInterface((IBinder)boundService);
		  Log.d( LOG_TAG,"onServiceConnected" );
        }

        public void onServiceDisconnected(ComponentName className) {
          twitterService = null;
		  Log.d( LOG_TAG,"onServiceDisconnected" );
		  updateServiceStatus();
        }
    };
    
    /**
     * Recieve android broadcasts. Used by us purely to get notifications
     * from our sourcing service.
     */
	private BroadcastReceiver serviceUpdates = new BroadcastReceiver(){
		public void onReceive(Context context, Intent intent) {
			if(intent.getAction().equals("com.pftqg.android.JustTwitter.MentionsUpdate")) {
				mHandler.post(new Runnable(){
					public void run() {
						mentionsDS.onMentionsUpdated();						
					}
				});
			} else if(intent.getAction().equals("com.pftqg.android.JustTwitter.TimelineUpdate")) {
				mHandler.post(new Runnable(){
					public void run() {
						tweetsDS.onTimelineUpdated();
					}
				});
			} else if(intent.getAction().equals("com.pftqg.android.JustTwitter.DMSUpdate")) {
				mHandler.post(new Runnable(){
					public void run() {
						DMsDS.onDMsUpdated();
					}
				});
			} else if(intent.getAction().equals("com.pftqg.android.JustTwitter.BackgroundWorking")) {
				mHandler.post(new Runnable() {
					public void run() {
						setProgressBarIndeterminateVisibility(true);
					}
				});
			} else if(intent.getAction().equals("com.pftqg.android.JustTwitter.BackgroundDone")) {
				mHandler.post(new Runnable() {
					public void run() {
						setProgressBarIndeterminateVisibility(false);
					}
				});
			} else if(intent.getAction().equals("com.pftqg.android.JustTwitter.RefreshAllDMs")) {
				mHandler.post(new Runnable() {
					public void run() {
						DMsDS.onDMsClearCache();
					}
				});
			} else if(intent.getAction().equals("com.pftqg.android.JustTwitter.RefreshAllStatuses")) {
				mHandler.post(new Runnable() {
					public void run() {
						tweetsDS.onStatusesClearCache();
					}
				});
			} else if(intent.getAction().equals("com.pftqg.android.JustTwitter.RefreshAllMentions")) {
				mHandler.post(new Runnable() {
					public void run() {
						mentionsDS.onMentionsClearCache();
					}
				});
			} else {
				Log.d(LOG_TAG, "Unknown broadcast received: " + intent.getAction());
			}
		}
	}; 
	

    // Menu item ids
    public static final int MENU_CONTEXT_REPLY   = Menu.FIRST;
    public static final int MENU_CONTEXT_PROFILE = Menu.FIRST + 1;
    public static final int MENU_CONTEXT_FAVORITE = Menu.FIRST + 2;

    public static final int MENU_CONTEXT_MENTION_REPLY   = Menu.FIRST + 3;
    public static final int MENU_CONTEXT_MENTION_PROFILE = Menu.FIRST + 4;
    public static final int MENU_CONTEXT_MENTION_FAVORITE = Menu.FIRST + 5;
    
    public static final int MENU_CONTEXT_DM_REPLY   = Menu.FIRST + 6;
    public static final int MENU_CONTEXT_DM_PROFILE = Menu.FIRST + 7;
	
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
        ContextMenuInfo menuInfo) {
            super.onCreateContextMenu(menu, v, menuInfo);

            if(v == timeline) {
                    menu.add(0, MENU_CONTEXT_REPLY, 0, "Reply");
                    menu.add(0, MENU_CONTEXT_PROFILE, 0,  "Profile");
                    menu.add(0, MENU_CONTEXT_FAVORITE, 0,  "Favorite");
            } else if(v == mentions) {
                    menu.add(0, MENU_CONTEXT_MENTION_REPLY, 0, "Reply");
                    menu.add(0, MENU_CONTEXT_MENTION_PROFILE, 0,  "Profile");
                    menu.add(0, MENU_CONTEXT_MENTION_FAVORITE, 0,  "Favorite");
            } else if(v == directmessages) {
                    menu.add(0, MENU_CONTEXT_DM_REPLY, 0, "Reply");
                    menu.add(0, MENU_CONTEXT_DM_PROFILE, 0,  "Profile");
            }
    }
	
    @Override
    public boolean onContextItemSelected(MenuItem item) {
            AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

            DBStatus s = null;
            DBDirectMessage dm = null;
            String screenName = null;
            switch(item.getItemId()) {
                    case MENU_CONTEXT_REPLY:
                            s = tweetsDS.getStatusForPosition(info.position);
                            screenName = s.getUser().getScreenName();
                    newPosting("@" + screenName + " ", true, s.getId(), screenName);
                            return true;
                    case MENU_CONTEXT_PROFILE:
                            return true;
                    case MENU_CONTEXT_FAVORITE:
                            return true;
                    case MENU_CONTEXT_MENTION_REPLY:
                            s = mentionsDS.getStatusForPosition(info.position);
                            screenName = s.getUser().getScreenName();
                    newPosting("@" + screenName + " ", true, s.getId(), screenName);
                            return true;
                    case MENU_CONTEXT_MENTION_PROFILE:
                            return true;
                    case MENU_CONTEXT_MENTION_FAVORITE:
                            return true;
                    case MENU_CONTEXT_DM_REPLY:
                            dm = DMsDS.getStatusForPosition(info.position);
                            newDMComposer(dm.getSenderId(), dm.getSender().getScreenName());
                            return true;
                    case MENU_CONTEXT_DM_PROFILE:
                            return true;
            }

            return super.onContextItemSelected(item);
    }
	
}