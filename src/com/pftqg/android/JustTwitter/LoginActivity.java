package com.pftqg.android.JustTwitter;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


public class LoginActivity extends JTActivity implements OnClickListener {
	
	private static final String LOG_TAG = packageName + ".Timeline";

	private Handler mHandler = null;
	private EditText username;
	private EditText password;
	private Button loginButton;
	private ProgressDialog dialog;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.login_window);
        Log.d(LOG_TAG, "LoginActivity started");

		startSourcing();
		mHandler = new Handler();       

        username = (EditText)findViewById(R.id.loginUsername);
        password = (EditText)findViewById(R.id.loginPassword);
        loginButton = (Button)findViewById(R.id.loginInitiateLogin);
        loginButton.setOnClickListener(this);
        

		IntentFilter f = new IntentFilter();
		f.addAction("com.pftqg.android.JustTwitter.LoginSuccess");
		f.addAction("com.pftqg.android.JustTwitter.LoginFailure");
		f.addAction("com.pftqg.android.JustTwitter.BackgroundWorking");
		f.addAction("com.pftqg.android.JustTwitter.BackgroundDone");
		this.registerReceiver(serviceUpdates, f);
        
    }

    @Override
    public void onStart() {
        initSourcing();
		super.onStart();
    }

    @Override
    public void onDestroy() {
    	this.unregisterReceiver(serviceUpdates);
    	releaseSourcing();
    	super.onDestroy();
    }

	public void onClick(View v) {
		if(v == loginButton) {
	    	dialog = ProgressDialog.show(this, "Authenticating", "Please wait...", true);	 
			try {
				getConnection().verifyCredentials(username.getText().toString(), 
						password.getText().toString());
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				Log.e(LOG_TAG, "verify credentials failed", e);
				dialog.dismiss();
			}
		}
	}
	
	public void saveCredentials() {
		((Application)getApplication()).setCredentials(username.getText().toString(), password.getText().toString());
	}
	
	public void loginFailed() {
		new AlertDialog.Builder(this)
			.setTitle("Login failed")
			.setMessage("Twitter did not recognise your username and password")
			.setPositiveButton(R.string.ok, null)
			.show();
	}
    
    /**
     * Recieve android broadcasts. Used by us purely to get notifications
     * from our sourcing service.
     */
	private BroadcastReceiver serviceUpdates = new BroadcastReceiver(){
		public void onReceive(Context context, Intent intent) {			
			if(intent.getAction().equals("com.pftqg.android.JustTwitter.LoginSuccess")) {
				mHandler.post(new Runnable(){
					public void run() {
						Log.d(LOG_TAG,"Login ok!");
						dialog.dismiss();
						saveCredentials();
						Toast.makeText(getApplicationContext(), "Login succeeded.", Toast.LENGTH_SHORT).show();
						try { 
							getConnection().refreshTweets();
							getConnection().refreshMentions();
							getConnection().refreshDMs();
						} catch (RemoteException ex){
							Log.e(LOG_TAG,"Failed to call twitterService.refreshTweets()", ex);
						}
						finish();
					}
				});
			} else if(intent.getAction().equals("com.pftqg.android.JustTwitter.LoginFailure")) {
				mHandler.post(new Runnable(){
					public void run() {
						Log.d(LOG_TAG,"Login failed :(");
						dialog.dismiss();
						loginFailed();
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
