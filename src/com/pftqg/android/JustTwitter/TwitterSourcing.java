package com.pftqg.android.JustTwitter;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.util.Log;


public class TwitterSourcing extends Service {
	
	private static final String LOG_TAG = "com.pftqg.android.JustUpdate.TwitterSourcing";
	
	PowerManager.WakeLock wl;
	private Handler serviceHandler = null;
	private boolean amStarted;
	
	@SuppressWarnings("unused")
	private long updateFrequency = 5 * 60 * 1000L; // 5 minutes
	private int fetchAtOnce = 200;

	private DatabaseHelper d;
	
	private int backgroundTasks = 0;
	
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		Log.d(LOG_TAG,"onStart");
		if(!amStarted) {
			serviceHandler = new Handler();

			d = new DatabaseHelper(this);
			
			serviceHandler.postDelayed( new RunTask(), 1L); // first update in one second...
			amStarted = true;
		}
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return binder;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(LOG_TAG,"onCreate");
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "com.pftqg.android.JustTwitter.SourcingLock");
	}
	
	@Override
	public void onDestroy() {
		if(d != null)
			d.close();
		super.onDestroy();
		Log.d(LOG_TAG,"onDestroy");
	}
	
	private boolean isBackgroundWorking() {
		return (backgroundTasks != 0);
	}
	
	private void startBackgroundWork() {
		if(!wl.isHeld())
			wl.acquire();
		backgroundTasks++;
		sendBroadcast(new Intent().setAction("com.pftqg.android.JustTwitter.BackgroundWorking"));
	}
	private void stopBackgroundWork() {
		backgroundTasks--;
		if(backgroundTasks <= 0) {
			if(wl.isHeld())
				wl.release();
			backgroundTasks = 0;
			sendBroadcast(new Intent().setAction("com.pftqg.android.JustTwitter.BackgroundDone"));
		}
	}
	
	public void updateDMs() {
		updateRecievedDMs();
		updateSentDMs();
	}
	
	private boolean isLoggedIn() {
		return ((Application)getApplication()).isLoggedIn();
	}
	
	private boolean updateDMsInProgress = false;
	public void updateRecievedDMs() {
		if(!this.isLoggedIn()) return;
		Log.d(LOG_TAG,"updateRecievedDMs!");
		if(updateDMsInProgress) return;
		updateDMsInProgress = true;
		startBackgroundWork();
    	new Thread(new Runnable(){
       		private long maxSinceID = -1;
    		private long minSinceID = -1;
    		public void run() {
    			try {
    				
    				if(minSinceID == -1) {
    					minSinceID = d.latestDM(true);
    				}
    				
    				String url = "direct_messages.json?count=" + Integer.toString(fetchAtOnce);

					boolean firstRun = (minSinceID == 0);
					if(!firstRun)
						url = url + "&since_id=" + Long.toString(minSinceID);

					if(maxSinceID != -1)
						url = url + "&max_id=" + Long.toString(maxSinceID);
					
					Log.d(LOG_TAG,"updateDMs start... minSinceID: " + Long.toString(minSinceID) + 
							", maxSinceID: "+ Long.toString(maxSinceID) + 
							", using URL: "+ url);

					InputStream t = twitterGet(url);
    				    				
					JsonFactory f = new JsonFactory();
					JsonParser p = f.createJsonParser(t);
    				ObjectMapper om = new ObjectMapper();

    				JsonNode n = om.readTree(p);
					if(!n.isArray()) {
						Log.e(LOG_TAG, "updateDMs failed, because the root node is NOT an array");	
						updateDMsInProgress = false;				
						return;
					}

					for(int i = 0; i < n.size(); i++) {
						maxSinceID = d.serializeDM(n.get(i), true);
					}
    				
    				
					sendBroadcast(new Intent().setAction("com.pftqg.android.JustTwitter.DMSUpdate"));
    				if(n.size() == fetchAtOnce && !firstRun) {
        				Log.d(LOG_TAG, "updateDMs has some data...");
    					run();
    				} else {
        				Log.d(LOG_TAG, "updateDMs has all data...");
    					sendBroadcast(new Intent().setAction("com.pftqg.android.JustTwitter.DMSUpdate"));
    					updateDMsInProgress = false;
    					stopBackgroundWork();
    				}
    				
    			} catch (Exception e) {
    				Log.e(LOG_TAG, "updateDMs encountered a problem...", e);
    				updateDMsInProgress = false;
					stopBackgroundWork();
    			}
    		}
    	}).start();
	}

	private boolean updateSentDMsInProgress = false;
	public void updateSentDMs() {
		if(!this.isLoggedIn()) return;
		Log.d(LOG_TAG,"updateDMs!");
		if(updateSentDMsInProgress) return;
		updateSentDMsInProgress = true;
		startBackgroundWork();
    	new Thread(new Runnable(){
       		private long maxSinceID = -1;
    		private long minSinceID = -1;
    		public void run() {
    			try {
    				
    				if(minSinceID == -1) {
    					minSinceID = d.latestDM(false);
    				}
    				
    				String url = "direct_messages/sent.json?count=" + Integer.toString(fetchAtOnce);

					boolean firstRun = (minSinceID == 0);
					if(!firstRun)
						url = url + "&since_id=" + Long.toString(minSinceID);

					if(maxSinceID != -1)
						url = url + "&max_id=" + Long.toString(maxSinceID);
					
					Log.d(LOG_TAG,"updateSentDMs start... minSinceID: " + Long.toString(minSinceID) + 
							", maxSinceID: "+ Long.toString(maxSinceID) + 
							", using URL: "+ url);

					InputStream t = twitterGet(url);
    				    				
					JsonFactory f = new JsonFactory();
					JsonParser p = f.createJsonParser(t);
    				ObjectMapper om = new ObjectMapper();

    				JsonNode n = om.readTree(p);
					if(!n.isArray()) {
						Log.e(LOG_TAG, "updateSentDMs failed, because the root node is NOT an array");	
						updateSentDMsInProgress = false;				
						return;
					}

					for(int i = 0; i < n.size(); i++) {
						maxSinceID = d.serializeDM(n.get(i), false);
					}
    				
    				
					sendBroadcast(new Intent().setAction("com.pftqg.android.JustTwitter.DMSUpdate"));
    				if(n.size() == fetchAtOnce && !firstRun) {
        				Log.d(LOG_TAG, "updateSentDMs has some data...");
    					run();
    				} else {
        				Log.d(LOG_TAG, "updateSentDMs has all data...");
    					sendBroadcast(new Intent().setAction("com.pftqg.android.JustTwitter.DMSUpdate"));
    					updateSentDMsInProgress = false;
    					stopBackgroundWork();
    				}
    				
    			} catch (Exception e) {
    				Log.e(LOG_TAG, "updateSentDMs encountered a problem...", e);
    				updateSentDMsInProgress = false;
					stopBackgroundWork();
    			}
    		}
    	}).start();
	}

	private boolean updateMentionsInProgress = false;
    public void updateMentions() {
		if(!this.isLoggedIn()) return;
    	Log.d(LOG_TAG,"updateMentions!");
    	if(updateMentionsInProgress) return;
    	updateMentionsInProgress = true;
		startBackgroundWork();
    	new Thread(new Runnable() {
       		private long maxSinceID = -1;
    		private long minSinceID = -1;
    		public void run() {
    			try {
    				
       				if(minSinceID == -1) {
    					minSinceID = d.latestStatus(true);
    				}
       				
    				String url = "statuses/mentions.json?count=" + Integer.toString(fetchAtOnce);

					boolean firstRun = (minSinceID == 0);
					if(!firstRun)
						url = url + "&since_id=" + Long.toString(minSinceID);

					if(maxSinceID != -1)
						url = url + "&max_id=" + Long.toString(maxSinceID);
					
					Log.d(LOG_TAG,"updateMentions start... minSinceID: " + Long.toString(minSinceID) + 
							", maxSinceID: "+ Long.toString(maxSinceID) + 
							", using URL: "+ url);

					InputStream t = twitterGet(url);
    				    				
					JsonFactory f = new JsonFactory();
					JsonParser p = f.createJsonParser(t);
    				ObjectMapper om = new ObjectMapper();

    				JsonNode n = om.readTree(p);
					if(!n.isArray()) {
						Log.e(LOG_TAG, "updateMentions failed, because the root node is NOT an array");	
						updateMentionsInProgress = false;				
						return;
					}

					for(int i = 0; i < n.size(); i++) {
						maxSinceID = d.serializeMention(n.get(i));
					}

					sendBroadcast(new Intent().setAction("com.pftqg.android.JustTwitter.MentionsUpdate"));
    				if(n.size() == fetchAtOnce && !firstRun) {
	    				Log.d(LOG_TAG, "updateMentions has some data...");
						run();
					} else {
	    				Log.d(LOG_TAG, "updateMentions has all data...");
						updateMentionsInProgress = false;
						stopBackgroundWork();
					}
    				
    			} catch (Exception e) {
    				Log.e(LOG_TAG, "updateMentions encountered a problem...", e);
					updateMentionsInProgress = false;
					stopBackgroundWork();
    			}
    		}
    	}).start();
    }

	private boolean updateTimelineInProgress = false;
    public void updateTimeline() {
		if(!this.isLoggedIn()) return;
    	Log.d(LOG_TAG,"updateTimeline!");
    	if(updateTimelineInProgress) {
    		return;
    	}
    	updateTimelineInProgress = true;
		startBackgroundWork();
    	new Thread(new Runnable(){
    		
    		private long maxSinceID = -1;
    		private long minSinceID = -1;
    		public void run() {
    			try {
    				
    				if(minSinceID == -1) {
    					minSinceID = d.latestStatus(false);
    				}
    				
    				
    				String url = "statuses/friends_timeline.json?count=" + Integer.toString(fetchAtOnce);

					boolean firstRun = (minSinceID == 0);
					if(!firstRun)
						url = url + "&since_id=" + Long.toString(minSinceID);

					if(maxSinceID != -1)
						url = url + "&max_id=" + Long.toString(maxSinceID);
					
					Log.d(LOG_TAG,"updateTimeline start... minSinceID: " + Long.toString(minSinceID) + 
							", maxSinceID: "+ Long.toString(maxSinceID) + 
							", using URL: "+ url);

					InputStream t = twitterGet(url);
    				    				
					JsonFactory f = new JsonFactory();
					JsonParser p = f.createJsonParser(t);
    				ObjectMapper om = new ObjectMapper();

    				JsonNode n = om.readTree(p);
					if(!n.isArray()) {
						Log.e(LOG_TAG, "fetchTwitterURL failed, because the root node is NOT an array");	
    			    	updateTimelineInProgress = false;				
						return;
					}

					for(int i = 0; i < n.size(); i++) {
						maxSinceID = d.serializeStatus(n.get(i));
					}

					sendBroadcast(new Intent().setAction("com.pftqg.android.JustTwitter.TimelineUpdate"));
    				if(n.size() == fetchAtOnce && !firstRun) {
	    				Log.d(LOG_TAG, "updateTimeline has some data...");
						run();
					} else {
	    				Log.d(LOG_TAG, "updateTimeline has all data...");
				    	updateTimelineInProgress = false;
						stopBackgroundWork();
					}
    				
    			} catch (Exception e) {
    				Log.e(LOG_TAG, "updateTimeline encountered a problem...", e);
    		    	updateTimelineInProgress = false;
					stopBackgroundWork();
    			}
    		}
    	}).start();
    }
    
    class FetchOtherTweetThread implements Runnable {
    	private long messageId;

    	public FetchOtherTweetThread setMessageId(long messageId) {
    		this.messageId = messageId;
    		return this;
    	}
    	
    	public void broadcastResult(String substring) {
			sendBroadcast(new Intent().setAction("com.pftqg.android.JustTwitter." + substring).
					setData(Uri.parse("twitter-status://" + Long.toString(messageId))));    		
    	}
   
    	public void run() {
			try {

				String url = "statuses/show/" + Long.toString(this.messageId) + ".json";

				InputStream t = twitterGet(url);
				    				
				JsonFactory f = new JsonFactory();
				JsonParser p = f.createJsonParser(t);
				ObjectMapper om = new ObjectMapper();

				JsonNode n = om.readTree(p);
				if(!n.isObject()) {
					Log.e(LOG_TAG, "fetchOtherTweet failed, because the root node is NOT an array");	
					this.broadcastResult("IndividualTweetFailed");
					return;
				}
				
				d.serializeStatusOther(n); 
				
				Log.d(LOG_TAG, "fetchOtherTweet has all data...");
				this.broadcastResult("IndividualTweetSucceeded");
				stopBackgroundWork();
				
			} catch (Exception e) {
				Log.e(LOG_TAG, "fetchOtherTweet encountered a problem...", e);
				this.broadcastResult("IndividualTweetFailed");
				stopBackgroundWork();
			}
		}
    }

    public void fetchOtherTweet(long message_id) {
		if(!this.isLoggedIn()) return;
    	Log.d(LOG_TAG,"fetchOtherTweet!");
		startBackgroundWork();
    	new Thread(new FetchOtherTweetThread().setMessageId(message_id)).start();
    }
    
    class PostTweetThread implements Runnable {
    	private String message;
    	private long inReplyTo;
    	
    	public PostTweetThread setMessage(String message) {
    		this.message = message;
    		return this;
    	}

    	public PostTweetThread setInReplyTo(long inReplyTo) {
    		this.inReplyTo = inReplyTo;
    		return this;
    	}
    	
    	public void run() {
			try {
				String url = "statuses/update.json";
				
				Log.d(LOG_TAG,"postTweetToTwitter start... using URL: "+ url);

				Map<String,String> params = new HashMap<String,String>();
				params.put("status", message);
				params.put("source", "justupdate");
				if(inReplyTo != 0) {
					params.put("in_reply_to_status_id", Long.toString(inReplyTo));
				}
				
				InputStream t = twitterPost(url, params);
				
				JsonParser p = (new JsonFactory()).createJsonParser(t);
				(new ObjectMapper()).readTree(p);
				sendBroadcast(new Intent().setAction("com.pftqg.android.JustTwitter.PostMessageSuccess"));
				
			} catch (Exception e) {
				Log.e(LOG_TAG, "postTweetToTwitter encountered a problem...", e);
				sendBroadcast(new Intent().setAction("com.pftqg.android.JustTwitter.PostMessageFailure"));
				stopBackgroundWork();
			}
		}
    }
    
    public void postTweetToTwitter(String message, long inReplyTo) {
		if(!this.isLoggedIn()) return;
    	Log.d(LOG_TAG,"postTweetToTwitter!");
		startBackgroundWork();
    	new Thread(new PostTweetThread().setMessage(message).setInReplyTo(inReplyTo)).start();
    }

    class PostDMThread implements Runnable {
    	private String message;
    	private String sendTo;
    	
    	public PostDMThread setMessage(String message) {
    		this.message = message;
    		return this;
    	}

    	public PostDMThread setSendTo(String sendTo) {
    		this.sendTo = sendTo;
    		return this;
    	}
    	
    	public void run() {
			try {
				String url = "direct_messages/new.json";
				
				Log.d(LOG_TAG,"postTweetToTwitter start... using URL: "+ url);

				Map<String,String> params = new HashMap<String,String>();
				params.put("text", message);
				params.put("source", "justupdate");
				params.put("user", sendTo);
				
				InputStream t = twitterPost(url, params);
				
				JsonParser p = (new JsonFactory()).createJsonParser(t);
				(new ObjectMapper()).readTree(p);
				sendBroadcast(new Intent().setAction("com.pftqg.android.JustTwitter.PostDirectMessageSuccess"));
				
			} catch (Exception e) {
				Log.e(LOG_TAG, "postTweetToTwitter encountered a problem...", e);
				sendBroadcast(new Intent().setAction("com.pftqg.android.JustTwitter.PostMessageFailure"));
				stopBackgroundWork();
			}
		}
    }
    
    public void postDMToTwitter(String message, String sendTo) {
		if(!this.isLoggedIn()) return;
    	Log.d(LOG_TAG,"postDMToTwitter!");
		startBackgroundWork();
    	new Thread(new PostDMThread().setMessage(message).setSendTo(sendTo)).start();
    }
	
    
    
    class CheckCredentialsThread implements Runnable {
    	private String username;
    	private String password;
    	
    	public CheckCredentialsThread setUsername(String username) {
    		this.username = username;
    		return this;
    	}

    	public CheckCredentialsThread setPassword(String password) {
    		this.password = password;
    		return this;
    	}
    	
    	public void run() {
			try {
				String url = "account/verify_credentials.json";
				
				Log.d(LOG_TAG,"checkCredentials start... using URL: "+ url);
				
				Credentials c = new UsernamePasswordCredentials(username, password);
				
				InputStream t = twitterGet(url, c);
				
				JsonParser p = (new JsonFactory()).createJsonParser(t);
				(new ObjectMapper()).readTree(p);
				sendBroadcast(new Intent().setAction("com.pftqg.android.JustTwitter.LoginSuccess"));
				
			} catch (Exception e) {
				Log.e(LOG_TAG, "checkCredentials encountered a problem...", e);
				sendBroadcast(new Intent().setAction("com.pftqg.android.JustTwitter.LoginFailure"));
				stopBackgroundWork();
			}
		}
    }
    
    public void checkCredentials(String username, String password) {
    	Log.d(LOG_TAG,"checkCredentials!");
		startBackgroundWork();
    	new Thread(new CheckCredentialsThread().setUsername(username).setPassword(password)).start();
    }
    
	private final ITwitterSourcingService.Stub binder = new ITwitterSourcingService.Stub() {

		public void refreshMentions() throws RemoteException {
			updateMentions();
		}

		public void refreshDMs() throws RemoteException {
			updateDMs();
		}
		
		public void refreshTweets() throws RemoteException {
			updateTimeline();			
		}
		
		public void postTweet(String tweet, long inReplyTo) throws RemoteException {
			postTweetToTwitter(tweet, inReplyTo);
		}
		
		public void postDM(String message, String to) throws RemoteException {
			postDMToTwitter(message, to);
		}
		
		public void getTweet(long message_id) throws RemoteException {
			fetchOtherTweet(message_id);
		}
		
		public boolean isBackgroundWorking() {
			return TwitterSourcing.this.isBackgroundWorking();
		}

		public void verifyCredentials(String username, String password) throws RemoteException {
			checkCredentials(username, password);			
		}
	};
	

	class RunTask implements Runnable {
		public void run() {
			updateTimeline();
			updateMentions();
			updateDMs();
		}
	}

	public InputStream twitterGet(String method) {
		return twitterGet(method, ((Application)getApplication()).getCredentials());
	}
	

	public InputStream twitterGet(String method, Credentials c) {
		Log.d(LOG_TAG, "Starting twitterGet " + method + " at " + new Date());
		InputStream is = null;
		HttpClient hc = new DefaultHttpClient();
		HttpGet hg = new HttpGet("https://twitter.com/" + method);
		hg.addHeader(BasicScheme.authenticate(c, "utf-8", false));
		hg.getParams().setBooleanParameter("http.protocol.expect-continue", false);
		try {
			is = hc.execute(hg).getEntity().getContent();
		} catch(Exception ex) {
			Log.e(LOG_TAG, "twitterGet failed!", ex);
		}

		Log.d(LOG_TAG, "Finishing twitterGet " + method + " at " + new Date());
		return is;
	}
	
	public InputStream twitterPost(String method, Map<String, String> kvPairs) {
		Log.d(LOG_TAG, "Starting twitterPost " + method + " at " + new Date());
		InputStream is = null;
		HttpClient hc = new DefaultHttpClient();
		HttpPost hg = new HttpPost("https://twitter.com/" + method);
		hg.addHeader(BasicScheme.authenticate(((Application)getApplication()).getCredentials(), "utf-8", false));
		hg.getParams().setBooleanParameter("http.protocol.expect-continue", false);
		try {
			if (kvPairs != null && kvPairs.isEmpty() == false) { 
			    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>( 
			                kvPairs.size()); 
			    String k, v; 
			    Iterator<String> itKeys = kvPairs.keySet().iterator(); 
			    while (itKeys.hasNext()) { 
			         k = itKeys.next(); 
			         v = kvPairs.get(k); 
			         nameValuePairs.add(new BasicNameValuePair(k, v)); 
			         //Log.d(LOG_TAG,"Setting: " + k + " to " + v);
			    } 
			    hg.setEntity(new UrlEncodedFormEntity(nameValuePairs)); 
			} 
			
			hg.getEntity().getContent();
			is = hc.execute(hg).getEntity().getContent();
		} catch(Exception ex) {
			Log.e(LOG_TAG, "twitterPost failed!", ex);
		}


		Log.d(LOG_TAG, "Finishing twitterPost " + method + " at " + new Date());
		return is;
	}

}

// Take an inputstream (e.g. http response) and turn it in to a string
//BufferedInputStream bis = new BufferedInputStream(is);
//ByteArrayBuffer baf = new ByteArrayBuffer(50000);
//
//int current = 0;
//while((current = bis.read()) != -1) {
//	baf.append((byte)current);
//}
//
//text = new String(baf.toByteArray());