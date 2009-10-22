package com.pftqg.android.JustTwitter;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.SpannableStringBuilder;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class DMWrapper {

	@SuppressWarnings("unused")
	private static final String LOG_TAG = "com.pftqg.android.JustUpdate.DMWrapper";
	
	private TextView username = null;
	private TextView message = null;
	private TextView about = null;
	private ImageView avatar = null;
	private View row = null;
	
	DMWrapper(View row) {
		this.row = row;
	}
	
	public void populateFrom(DBDirectMessage s) {
		DBUser u;		
		DBUser avatarUser;
		if(s.isWasReceived()) {
			u = s.getSender();
			avatarUser = s.getSender();
		} else {
			u = s.getRecipient();
			avatarUser = s.getSender();
		}
		
		if(u == null) {
			getMessage().setText("Error!");
			return;
		}
		
		
		getUsername().setText((s.isWasReceived() ? "" : "sent to ") + (u.getName().equals("") ? u.getScreenName() : u.getName() + " (" + u.getScreenName() + ")"));
		getMessage().setText(s.getText());
		
		SpannableStringBuilder ssb = new SpannableStringBuilder(s.getDatabaseHelper().sdf.format(s.getCreatedAt()));
		getAbout().setText(ssb.toString());
		
		GradientDrawable background = null;
		if(s.isWasReceived()) {
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
		
		getAvatar().setImageResource(R.drawable.placeholder);
		if(avatarUser.getProfileImageURL() != null && avatarUser.getProfileImageURL() != "") {
			getAvatar().setTag(avatarUser.getProfileImageURL());
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
