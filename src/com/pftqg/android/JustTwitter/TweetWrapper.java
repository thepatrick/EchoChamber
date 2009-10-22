package com.pftqg.android.JustTwitter;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class TweetWrapper {

	private static final String LOG_TAG = "com.pftqg.android.JustUpdate.TweetWrapper";
	
	private TextView username = null;
	private TextView message = null;
	private TextView about = null;
	private ImageView avatar = null;
	private View row = null;
	TweetWrapper(View row) {
		this.row = row;
	}
	
	public void populateFrom(DBStatus s) {
		if(s == null) {
			Log.e(LOG_TAG, "status is null! TweetsDataSource:118");
			getMessage().setText("Error!");
			return;
		}
		
		DBUser u = s.getUser();
		if(u == null) {
			Log.e(LOG_TAG, "user is null! TweetsDataSource:118");
			getMessage().setText("Error!");
			return;
		}
		
		getUsername().setText((u.getName().equals("") ? u.getScreenName() : u.getName() + " (" + u.getScreenName() + ")"));
		getMessage().setText(s.getText());
		
		SpannableStringBuilder ssb = new SpannableStringBuilder(s.getDatabaseHelper().sdf.format(s.getCreatedAt()));
		ssb.append(" from ");
		ssb.append(Html.fromHtml(s.getSource()));
		getAbout().setText(ssb.toString());
		
		GradientDrawable background = null;
		if(s.isMention()) {
			if(!s.isViewed()) {
				background = JTActivity.normalGradientWithColors(Color.parseColor("#396E45"), Color.parseColor("#25472D"));
			} else {
				background = JTActivity.normalGradientWithColors(Color.parseColor("#1F3B25"), Color.parseColor("#122517"));				
			}
		} else {
			if(!s.isViewed()) {
				background = JTActivity.normalGradientWithColors(Color.parseColor("#5c5d70"), Color.parseColor("#3f404e"));				
			} else {
				background = JTActivity.normalGradientWithColors(Color.parseColor("#383946"), Color.parseColor("#22232B"));				
			}
		}
		row.setBackgroundDrawable(background);
		
		//new GradientDrawable(Orientation.TOP_BOTTOM, new int[] {start, end});
		
		getAvatar().setImageResource(R.drawable.placeholder);
		if(u.getProfileImageURL() != null && u.getProfileImageURL() != "") {
			getAvatar().setTag(u.getProfileImageURL());
		}
		
	}
	
	private TextView getUsername() {
		if(username == null) {
			username = (TextView)row.findViewById(R.id.username);
		}
		return username;
	}
	
	private TextView getMessage() {
		if(message == null) {
			message = (TextView)row.findViewById(R.id.message);
		}
		return message;
	}
	
	private TextView getAbout() {
		if(about == null) {
			about = (TextView)row.findViewById(R.id.aboutmessage);
		}
		return about;
	}
	
	private ImageView getAvatar() {
		if(avatar == null) {
			avatar = (ImageView)row.findViewById(R.id.avatar);
		}
		return avatar;
	}
}
