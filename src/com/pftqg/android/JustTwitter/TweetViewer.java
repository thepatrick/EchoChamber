package com.pftqg.android.JustTwitter;

import java.util.LinkedList;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.text.Html;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.commonsware.cwac.cache.SimpleWebImageCache;
import com.commonsware.cwac.thumbnail.ThumbnailBus;
import com.commonsware.cwac.thumbnail.ThumbnailMessage;

public class TweetViewer extends JTActivity {
	
	private static final String LOG_TAG = packageName + ".TweetViewer";

	private Handler mHandler = null;
	
	private DatabaseHelper d;
	
	private DBStatus currentStatus;

	private ProgressDialog dialog;
	private TextView realName;
	private TextView tweetContents;
	private TextView screenName;
	private TextView tweetSource;
	private TextView tweetTime;
	private Button inReplyTo;
	private Button nextReplyTo;
	private ImageView avatar;
	
	private SimpleWebImageCache<ThumbnailBus, ThumbnailMessage> cache=null;
	
	private LinkedList<DBStatus> history;
	
	private long waitingOnTweet = -1;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.tweetview);
        
		startSourcing();
		mHandler = new Handler(); 
		

		Button info = (Button)findViewById(R.id.tweetViewInfoButton);
		inReplyTo = (Button)findViewById(R.id.tweetViewInReplyTo);
		nextReplyTo = (Button)findViewById(R.id.tweetViewWasInReplyTo);
		Button makeFav = (Button)findViewById(R.id.tweetViewMakeFavourite);
		Button reply = (Button)findViewById(R.id.tweetViewReply);
		Button retweet = (Button)findViewById(R.id.tweetViewRetweet);
		Button sendDm = (Button)findViewById(R.id.tweetViewSendDM);
		
		avatar = (ImageView)findViewById(R.id.tweetViewAvatar);
		realName = (TextView)findViewById(R.id.tweetViewRealName);
		screenName = (TextView)findViewById(R.id.tweetViewScreenName);
		tweetContents = (TextView)findViewById(R.id.tweetViewTweetContents);
		tweetSource = (TextView)findViewById(R.id.tweetViewTweetSource);
		tweetTime = (TextView)findViewById(R.id.tweetViewTweetTime);
		
		tweetContents.setAutoLinkMask(Linkify.ALL);

		setTitle("JustTwitter");
		
        d = new DatabaseHelper(this);	
        
        cache = ((Application)getApplication()).getCache();
		cache.getBus().register(getBusKey(), onCache);        
        
        inReplyTo.setOnClickListener(tweetViewerClicks);
        nextReplyTo.setOnClickListener(tweetViewerClicks);
        
        history = new LinkedList<DBStatus>();       
        
		IntentFilter f = new IntentFilter();
		f.addAction("com.pftqg.android.JustTwitter.IndividualTweetFailed");
		f.addAction("com.pftqg.android.JustTwitter.IndividualTweetSucceeded");
		f.addAction("com.pftqg.android.JustTwitter.BackgroundWorking");
		f.addAction("com.pftqg.android.JustTwitter.BackgroundDone");
		this.registerReceiver(serviceUpdates, f);

		normalGradientWithColors(R.id.tweetViewToolbar, Color.rgb(0x61, 0x61, 0x61), Color.rgb(0x37, 0x37, 0x37));
		normalGradientWithColors(R.id.tweetViewInfoBar, Color.rgb(207, 207, 207), Color.rgb(168, 168, 168));
		
    }
    
    private void showProgress() {
    	dialog = ProgressDialog.show(this, "", "Loading tweet...", true);
    }
    
    private OnClickListener tweetViewerClicks = new OnClickListener() {
		public void onClick(View v) {
			if (v == inReplyTo) {
				showProgress();
				DBStatus ds = d.getStatus(currentStatus.getInReplyToStatusId());
				if(ds != null) {
					history.addFirst(currentStatus);
					currentStatus = ds;
					populateTweet();
				} else {
					
					waitingOnTweet = currentStatus.getInReplyToStatusId();
					if(getConnection() != null) {
						try {
							getConnection().getTweet(waitingOnTweet);
						} catch (RemoteException e) {
							Log.e(LOG_TAG,"getTweet(waitingOnTweet) failed", e);
						}
					}
				}
			} else if (v == nextReplyTo) {
				currentStatus = history.poll();
				showProgress();
				populateTweet();
			}
		}
    };
    
    private void serviceHasObtainedTweetWeAreWaitingFor() {
    	if(waitingOnTweet == -1) return;
    	waitingOnTweet = -1;
		DBStatus ds = d.getStatus(waitingOnTweet);
		if(ds != null) {
			history.addFirst(currentStatus);
			currentStatus = ds;
			populateTweet();
		} else {
			Toast.makeText(getApplicationContext(), "Failed to retrieve tweet.", Toast.LENGTH_LONG).show();
		}
    }
    
	private String getBusKey() {
		return(toString());
	}
    
    private void populateTweet() {
    	dialog.dismiss();
    	tweetContents.setText(currentStatus.getText());
    	tweetSource.setText(Html.fromHtml(currentStatus.getSource()));
    	tweetTime.setText(d.sdf.format(currentStatus.getCreatedAt()));
    	
    	DBUser u = currentStatus.getUser();
    	realName.setText(u.getName());
    	screenName.setText("@" + u.getScreenName());
    	
    	if(cache == null) {
    		Log.e(LOG_TAG, "Cache is null");
    	}
    	if(cache.getBus() == null) {
    		Log.e(LOG_TAG, "Cache.getBus() is null");
    	}
    	if(getBusKey() == null) {
    		Log.e(LOG_TAG, "getBusKey is null");
    	}
	
    	ThumbnailMessage msg=cache.getBus().createMessage(getBusKey());
    	msg.setImageView(avatar);
    	msg.setUrl(u.getProfileImageURL());

		try {
			cache.notify(msg.getUrl(), msg);
		}
		catch (Throwable t) {
			Log.e(LOG_TAG, "Exception trying to fetch image", t);
		}
    	
    	if(currentStatus.getInReplyToStatusId() != 0) {
    		inReplyTo.setVisibility(View.VISIBLE);
    		DBUser inReplyToUser = d.getUser(currentStatus.getInReplyToUserId());
    		inReplyTo.setText("In reply to @" + inReplyToUser.getScreenName() + " ↪");
    	} else {
    		inReplyTo.setVisibility(View.INVISIBLE);
    	}
    	
    	if(history.isEmpty()) {
    		nextReplyTo.setVisibility(View.INVISIBLE);
    	} else {
    		nextReplyTo.setVisibility(View.VISIBLE);
    		nextReplyTo.setText("Replied to by @" + history.peek().getUser().getScreenName());
    	}
    }
    

    public void onStart() {
        initSourcing();
		try {
			if(getConnection() != null)
				this.setProgressBarIndeterminateVisibility(getConnection().isBackgroundWorking());
		} catch (RemoteException e) {
			Log.e(LOG_TAG,"twitterService.isBackgroundWorking() failed!", e);
		}
		super.onStart();

		try {
			if(getConnection() != null)
				this.setProgressBarIndeterminateVisibility(getConnection().isBackgroundWorking());
		} catch (RemoteException e) {
			Log.e(LOG_TAG,"twitterService.isBackgroundWorking() failed!", e);
		}
		
    	showProgress();
        currentStatus = d.getStatus(this.getIntent().getExtras().getLong("tweet_id", 0));
        populateTweet();
    }
    
    public void onResume() {
    	super.onResume();
    	setProgressBarIndeterminateVisibility(false);
		try {
			if(getConnection() != null)
				this.setProgressBarIndeterminateVisibility(getConnection().isBackgroundWorking());
		} catch (RemoteException e) {
			Log.e(LOG_TAG,"twitterService.isBackgroundWorking() failed!", e);
		}
    }
    
    public void onDestroy() {
    	d.close();
		cache.getBus().unregister(onCache);
    	this.unregisterReceiver(serviceUpdates);
    	releaseSourcing();
    	super.onDestroy();
    }
    
	private ThumbnailBus.Receiver<ThumbnailMessage> onCache=
		new ThumbnailBus.Receiver<ThumbnailMessage>() {
		public void onReceive(final ThumbnailMessage message) {
			final ImageView image=message.getImageView();
 
			TweetViewer.this.runOnUiThread(new Runnable() {
				public void run() {
					if (currentStatus != null &&
							currentStatus.getUser().getProfileImageURL().equals(message.getUrl())) {
						image.setImageDrawable(cache.get(message.getUrl()));
					}
				}
			});
		}
	};
	
    /**
     * Recieve android broadcasts. Used by us purely to get notifications
     * from our sourcing service.
     */
	private BroadcastReceiver serviceUpdates = new BroadcastReceiver(){
		public void onReceive(Context context, Intent intent) {	
			long targetTweet = -1;
			if(intent.getData() != null && intent.getData().getHost() != null)
				targetTweet = Long.parseLong( intent.getData().getHost());
			
			if(intent.getAction().equals("com.pftqg.android.JustTwitter.IndividualTweetFailed")) {
				mHandler.post(new Runnable(){
					public void run() {
						dialog.hide();
						Toast.makeText(getApplicationContext(), "Failed to retrieve tweet.", Toast.LENGTH_LONG).show();

					}
				});
			} else if(intent.getAction().equals("com.pftqg.android.JustTwitter.IndividualTweetSucceeded") &&
					targetTweet == waitingOnTweet) {
					mHandler.post(new Runnable(){
						public void run() {
							serviceHasObtainedTweetWeAreWaitingFor();
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
			} else {
				Log.d(LOG_TAG, "Unknown broadcast received: " + intent.getAction());
			}
		}
	}; 
}
