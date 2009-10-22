package com.pftqg.android.JustTwitter;

interface ITwitterSourcingService {

  void refreshTweets();
  void refreshMentions();  
  void refreshDMs();
  void postTweet(String tweet, long inReplyTo);
  void postDM(String message, String to);
  void getTweet(long message_id);
  void verifyCredentials(String username, String password);
  boolean isBackgroundWorking();

}