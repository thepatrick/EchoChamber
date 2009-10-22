package com.pftqg.android.JustTwitter;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TextView.OnEditorActionListener;

public class TweetComposer extends JTActivity {
	
	private static final String LOG_TAG = packageName + ".TweetComposer";
	
	private Handler mHandler = null;
	private ProgressDialog dialog;
	
	private long inReplyTo = 0;
	private String screenName;
	
	private boolean isNormalTweet = true; // false = DM 
	
	private TextView tweetTextField;
	private TextView remainingCount;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.newtweet);
        
		startSourcing();
		mHandler = new Handler();        

		IntentFilter f = new IntentFilter();
		f.addAction("com.pftqg.android.JustTwitter.PostMessageSuccess");
		f.addAction("com.pftqg.android.JustTwitter.PostDirectMessageSuccess");
		f.addAction("com.pftqg.android.JustTwitter.PostMessageFailure");
		f.addAction("com.pftqg.android.JustTwitter.BackgroundWorking");
		f.addAction("com.pftqg.android.JustTwitter.BackgroundDone");
		this.registerReceiver(serviceUpdates, f);
		
		setup_newTweetCancel();		
		setup_newTweetDone();
		
		tweetTextField = (TextView)findViewById(R.id.newTweetText);
		remainingCount = (TextView)findViewById(R.id.newTweetRemainingCount);
		
		remainingCount.setText("140");
		tweetTextField.setOnKeyListener(new OnKeyListener(){
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				remainingCount.setText(Integer.toString(140 - tweetTextField.getText().length()));
				return false;
			}	
			
		});
		tweetTextField.setOnEditorActionListener(new OnEditorActionListener(){
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				remainingCount.setText(Integer.toString(140 - tweetTextField.getText().length()));
				return false;
			}
		});
		
		Bundle ext = getIntent().getExtras();
		boolean isReply = ext.getBoolean("isReply", false);
		isNormalTweet = !ext.getBoolean("isDirectMessage", false);

		setTitle("New Tweet");			

		if(isReply || !isNormalTweet) {
			inReplyTo = ext.getLong("inReplyTo", 0L);

			screenName = ext.getString("screenName");
			if(isReply) {
				setTitle("Reply to " + screenName);
			} else { // is a DM
				setTitle("Direct Message to " + screenName);			
			}
		}
		String defaultText = ext.getString("defaultText");
	
		if(defaultText != null) {
			tweetTextField.setText(defaultText);
		}

		this.normalGradientWithColors(R.id.newTweetTopBar, Color.rgb(0x61, 0x61, 0x61), Color.rgb(0x37, 0x37, 0x37));
		this.normalGradientWithColors(R.id.newTweetAccessoryView, Color.rgb(0x61, 0x61, 0x61), Color.rgb(0x37, 0x37, 0x37));
		
    }
    
    public void setup_newTweetCancel() {
    	Button ar = (Button)findViewById(R.id.newTweetCancel);
		ar.setOnClickListener(new OnClickListener(){
			public void onClick(View v) {
				finish();
			}
		});
    }

   public void setup_newTweetDone() {
    	Button ar = (Button)findViewById(R.id.newTweetPost);
		ar.setOnClickListener(new OnClickListener(){
			public void onClick(View v) {
				postTweet();
			}
		});
    }
    
    @Override
    public void onDestroy() {
    	//stopSourcing();
    	this.unregisterReceiver(serviceUpdates);
    	releaseSourcing();
    	super.onDestroy();
    }

    @Override
    public void onStart() {
        initSourcing();
		try {
			if(getConnection() != null)
				this.setProgressBarIndeterminateVisibility(getConnection().isBackgroundWorking());
		} catch (RemoteException e) {
			Log.e(LOG_TAG,"twitterService.isBackgroundWorking() failed!", e);
		}
		super.onStart();
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
    

    public void postTweet() {
    	dialog = ProgressDialog.show(this, "", "Posting. Please wait...", true);	 
    	try {
    		if(isNormalTweet) {
    			getConnection().postTweet(tweetTextField.getText().toString(), inReplyTo);
    		} else { // it's a DM
    			getConnection().postDM(tweetTextField.getText().toString(), screenName);
    		}
		} catch (RemoteException e) {
			Log.e(LOG_TAG,"postTweet failed", e);
		}
    }
    
    private void tweetFailed() {
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setMessage("Failed to publish tweet.")
    	       .setNegativeButton("OK", new DialogInterface.OnClickListener() {
    	           public void onClick(DialogInterface dialog, int id) {
    	                dialog.cancel();
    	           }
    	       });
    	builder.create().show();
    }
    
    /**
     * Recieve android broadcasts. Used by us purely to get notifications
     * from our sourcing service.
     */
	private BroadcastReceiver serviceUpdates = new BroadcastReceiver(){
		public void onReceive(Context context, Intent intent) {			
			if(intent.getAction().equals("com.pftqg.android.JustTwitter.PostMessageSuccess")) {
				mHandler.post(new Runnable(){
					public void run() {
						dialog.dismiss();
						Toast.makeText(getApplicationContext(), "Status updated.", Toast.LENGTH_SHORT).show();
						try { 
							getConnection().refreshTweets();
						} catch (RemoteException ex){
							Log.e(LOG_TAG,"Failed to call twitterService.refreshTweets()", ex);
						}
						finish();
					}
				});
			} else if(intent.getAction().equals("com.pftqg.android.JustTwitter.PostDirectMessageSuccess")) {
				mHandler.post(new Runnable(){
					public void run() {
						dialog.dismiss();
						Toast.makeText(getApplicationContext(), "Message sent.", Toast.LENGTH_SHORT).show();
						try { 
							getConnection().refreshDMs();
						} catch (RemoteException ex){
							Log.e(LOG_TAG,"Failed to call twitterService.refreshDMs()", ex);
						}
						finish();
					}
				});
			} else if(intent.getAction().equals("com.pftqg.android.JustTwitter.PostMessageFailure")) {
				mHandler.post(new Runnable(){
					public void run() {
						dialog.dismiss();
						tweetFailed();
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
