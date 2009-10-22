package com.pftqg.android.JustTwitter;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.os.IBinder;
import android.util.Log;

public class JTActivity extends Activity {
	protected static final String packageName = "com.pftqg.android.JustTwitter";
	protected static final String sourcingName = "com.pftqg.android.JustTwitter.TwitterSourcing";
	
	protected static String LOG_TAG = packageName + ".JTActivity";
	
	private ITwitterSourcingService twitterService;
	private CounterServiceConnection conn;
	private boolean started = false;

	public static GradientDrawable normalGradientWithColors (int start, int end) {
		return new GradientDrawable(Orientation.TOP_BOTTOM, new int[] {start, end});
	}
	
    public void normalGradientWithColors(int viewId, int start, int end) {
    	GradientDrawable d = new GradientDrawable(Orientation.TOP_BOTTOM, new int[] {start, end});
    	findViewById(viewId).setBackgroundDrawable(d);
    }
    
    /**
     * Bind to a sourcing service - it should already be started!
     */
    protected void initSourcing() {
    	if(conn == null) {
    		conn = new CounterServiceConnection();
    		Intent i = new Intent().setClassName(packageName, sourcingName);
    		bindService(i, conn, Context.BIND_AUTO_CREATE);
    		Log.d(LOG_TAG, "initSourcing()!");
    	}
    }
    
    /**
     * Unbind from the sourcing service
     */
    protected void releaseSourcing() {
    	if(conn != null) {
    		unbindService(conn);
    		conn = null;
    		Log.d(LOG_TAG, "releaseSourcing()");
    	}
    }

    /**
     * Start the sourcing service (if it hasn't already)
     */
    protected void startSourcing() {
    	if( !started ) { 
    		Intent i = new Intent().setClassName(packageName, sourcingName);
    		startService(i);
    		Log.d(LOG_TAG,"startSourcing!");
    		started = true;
    	}
    }

    /**
     * Stop the sourcing service (time to quit?)
     */
	protected void stopSourcing() {
    	if(!started) {
    		// toast already done
    	} else {
    		Intent i = new Intent().setClassName(packageName, sourcingName);
    		stopService(i);
    		Log.d(LOG_TAG, "stopSourcing()");
    		started = false;
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
     * Callback class to handle the sourcing service being connected
     * and disconnected (disconnected likely because it's crashed?)
     */
    class CounterServiceConnection implements ServiceConnection {
        public void onServiceConnected(ComponentName className, 
			IBinder boundService ) {
          twitterService = ITwitterSourcingService.Stub.asInterface((IBinder)boundService);
		  Log.d( LOG_TAG,"onServiceConnected" );
        }

        public void onServiceDisconnected(ComponentName className) {
          twitterService = null;
		  Log.d( LOG_TAG,"onServiceDisconnected" );
        }
    };
}
