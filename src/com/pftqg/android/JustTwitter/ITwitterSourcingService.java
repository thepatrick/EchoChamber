/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /Users/patrick/dev/android/Workspace/JustTwitter/src/com/pftqg/android/JustTwitter/ITwitterSourcingService.aidl
 */
package com.pftqg.android.JustTwitter;
import java.lang.String;
import android.os.RemoteException;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Binder;
import android.os.Parcel;
public interface ITwitterSourcingService extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.pftqg.android.JustTwitter.ITwitterSourcingService
{
private static final java.lang.String DESCRIPTOR = "com.pftqg.android.JustTwitter.ITwitterSourcingService";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an ITwitterSourcingService interface,
 * generating a proxy if needed.
 */
public static com.pftqg.android.JustTwitter.ITwitterSourcingService asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = (android.os.IInterface)obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.pftqg.android.JustTwitter.ITwitterSourcingService))) {
return ((com.pftqg.android.JustTwitter.ITwitterSourcingService)iin);
}
return new com.pftqg.android.JustTwitter.ITwitterSourcingService.Stub.Proxy(obj);
}
public android.os.IBinder asBinder()
{
return this;
}
public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_refreshTweets:
{
data.enforceInterface(DESCRIPTOR);
this.refreshTweets();
reply.writeNoException();
return true;
}
case TRANSACTION_refreshMentions:
{
data.enforceInterface(DESCRIPTOR);
this.refreshMentions();
reply.writeNoException();
return true;
}
case TRANSACTION_refreshDMs:
{
data.enforceInterface(DESCRIPTOR);
this.refreshDMs();
reply.writeNoException();
return true;
}
case TRANSACTION_postTweet:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
long _arg1;
_arg1 = data.readLong();
this.postTweet(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_postDM:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.lang.String _arg1;
_arg1 = data.readString();
this.postDM(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_getTweet:
{
data.enforceInterface(DESCRIPTOR);
long _arg0;
_arg0 = data.readLong();
this.getTweet(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_verifyCredentials:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.lang.String _arg1;
_arg1 = data.readString();
this.verifyCredentials(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_isBackgroundWorking:
{
data.enforceInterface(DESCRIPTOR);
boolean _result = this.isBackgroundWorking();
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.pftqg.android.JustTwitter.ITwitterSourcingService
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
public void refreshTweets() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_refreshTweets, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public void refreshMentions() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_refreshMentions, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public void refreshDMs() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_refreshDMs, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public void postTweet(java.lang.String tweet, long inReplyTo) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(tweet);
_data.writeLong(inReplyTo);
mRemote.transact(Stub.TRANSACTION_postTweet, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public void postDM(java.lang.String message, java.lang.String to) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(message);
_data.writeString(to);
mRemote.transact(Stub.TRANSACTION_postDM, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public void getTweet(long message_id) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeLong(message_id);
mRemote.transact(Stub.TRANSACTION_getTweet, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public void verifyCredentials(java.lang.String username, java.lang.String password) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(username);
_data.writeString(password);
mRemote.transact(Stub.TRANSACTION_verifyCredentials, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public boolean isBackgroundWorking() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_isBackgroundWorking, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
}
static final int TRANSACTION_refreshTweets = (IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_refreshMentions = (IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_refreshDMs = (IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_postTweet = (IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_postDM = (IBinder.FIRST_CALL_TRANSACTION + 4);
static final int TRANSACTION_getTweet = (IBinder.FIRST_CALL_TRANSACTION + 5);
static final int TRANSACTION_verifyCredentials = (IBinder.FIRST_CALL_TRANSACTION + 6);
static final int TRANSACTION_isBackgroundWorking = (IBinder.FIRST_CALL_TRANSACTION + 7);
}
public void refreshTweets() throws android.os.RemoteException;
public void refreshMentions() throws android.os.RemoteException;
public void refreshDMs() throws android.os.RemoteException;
public void postTweet(java.lang.String tweet, long inReplyTo) throws android.os.RemoteException;
public void postDM(java.lang.String message, java.lang.String to) throws android.os.RemoteException;
public void getTweet(long message_id) throws android.os.RemoteException;
public void verifyCredentials(java.lang.String username, java.lang.String password) throws android.os.RemoteException;
public boolean isBackgroundWorking() throws android.os.RemoteException;
}
