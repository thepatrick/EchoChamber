package com.pftqg.android.JustTwitter;

import java.util.Random;

import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.util.Log;

import com.commonsware.cwac.cache.SimpleWebImageCache;
import com.commonsware.cwac.thumbnail.ThumbnailBus;
import com.commonsware.cwac.thumbnail.ThumbnailMessage;
import com.pftqg.android.JustTwitter.util.DesEncrypter;

public class Application extends android.app.Application {
	private static String TAG="com.pftqg.android.JustTwitter.Application";
	private ThumbnailBus bus = new ThumbnailBus();
	private SimpleWebImageCache<ThumbnailBus, ThumbnailMessage> cache=null;

    private SharedPreferences mPrefs;
    
    private DesEncrypter vault;
	
	public Application() {
		super();
		Thread.setDefaultUncaughtExceptionHandler(onBlooey);
		cache = new SimpleWebImageCache<ThumbnailBus, ThumbnailMessage>(null, null, 101, bus);
		
        
	}
	
	void goBlooey(Throwable t) {
		AlertDialog.Builder builder=new AlertDialog.Builder(this);
		
		builder
			.setTitle(R.string.exception)
			.setMessage(t.toString())
			.setPositiveButton(R.string.ok, null)
			.show();
	}
	
	ThumbnailBus getBus() {
		return(bus);
	}
	
	SimpleWebImageCache<ThumbnailBus, ThumbnailMessage> getCache() {
		return(cache);
	}
	
	private Thread.UncaughtExceptionHandler onBlooey=
		new Thread.UncaughtExceptionHandler() {
		public void uncaughtException(Thread thread, Throwable ex) {
			Log.e(TAG, "Uncaught exception", ex);
			goBlooey(ex);
		}
	};
	
	public SharedPreferences getPreferences() {
		if(mPrefs == null) 
			mPrefs = getSharedPreferences("JustTwitterPrefs", 0);
		return mPrefs;
	}
	

	public DesEncrypter getVault() {
		if(vault == null) {
			String base = TAG;
			
			// look for a passphrase
			String authkey = this.getPreferences().getString("auth.key", "");
			if(authkey == "") {
				Random random =  new Random();
		        long r1 = random.nextLong();
		        long r2 = random.nextLong();
		        String hash1 = Long.toHexString(r1);
		        String hash2 = Long.toHexString(r2);
		        authkey = hash1 + hash2;
		    	SharedPreferences.Editor ed = mPrefs.edit();
		    	ed.putString("auth.key",authkey);
		    	ed.commit();
			}
			vault = new DesEncrypter(base);
		}
		return vault;
	}
	
	public boolean isLoggedIn() {
		return getPreferences().getBoolean("auth.is_logged_in_2", false);
	}
	
	public Credentials getCredentials() {
		if(!isLoggedIn()) return null;

//		Log.d(TAG,"getCredentials() making with username: " + mPrefs.getString("auth.username", ""));
//		Log.d(TAG,"getCredentials() making with password: " + this.getPassword());
//		
		return new UsernamePasswordCredentials(mPrefs.getString("auth.username", ""), this.getPassword());
	}
	
	public String getPassword() {
//		Log.d(TAG,"Decrypting " + mPrefs.getString("auth.password", ""));
		return (mPrefs.getString("auth.password", "")); //getVault().decrypt
	}
	
	public void savePassword(String pass) {
    	SharedPreferences.Editor ed = mPrefs.edit();
    	ed.putString("auth.password", pass); //getVault().encrypt(pass)
    	ed.commit();
		return;
	}
	
	public void setCredentials(String username, String password) {
    	SharedPreferences.Editor ed = mPrefs.edit();
    	ed.putBoolean("auth.is_logged_in_2", true);
    	ed.putString("auth.username", username);
    	ed.commit();
    	savePassword(password);
	}
}
