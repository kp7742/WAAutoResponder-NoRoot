package com.kmods.waautoresponder;

import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;

public class NotificationReceiver extends NotificationListenerService {
    static boolean isRunning = false;//is Running Bool
    static Notification notification;

    SharedPreferences sharedPreferences;

    @Override
    public void onNotificationPosted(final StatusBarNotification sbn) {
        //super.onNotificationPosted(sbn); //Not Needed
        if(Settings.Secure.getString(getContentResolver(),"enabled_notification_listeners").contains(getPackageName())// Check for Notification Permission
                && isOn() && sbn != null && !sbn.isOngoing() && sbn.getPackageName().equals(Const.PKG)){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try{
                        notification = sbn.getNotification();//Latest Notification of WhatsApp
                        if(notification != null){
                            Bundle bundle = notification.extras;
                            logBundle(bundle);
                            toastMsg("WAAutoResponder Read Notification");
                        }
                    } catch (Exception e){
                        notification = null;
                        toastMsg("WAAutoResponder Fails to Read Last Notification!");
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = true;
        sharedPreferences = getSharedPreferences(Const.BOT, Context.MODE_PRIVATE);
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        super.onDestroy();
    }

    /*
     * Get Status of Toggle
     */
    private boolean isOn(){
        return sharedPreferences.getBoolean(Const.STATUS, false);
    }

    /*
     * Toast Received Message
     */
    private void toastMsg(String s){
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }

    /*
     * Log Notification Bundle
     */
    private void logBundle(Bundle bundle){
        try {
            FileOutputStream out = new FileOutputStream(getCacheDir() + "/BundleDump-" + System.currentTimeMillis() + ".txt");
            for (String key : bundle.keySet()) {
                String data = key + " | " + bundle.get(key) + "\n";
                out.write(data.getBytes());
            }
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        /*
        Android 9.0 Pie Group Chat Notification:
        android.title | WAChatBot
        android.android.reduced.images | true
        android.conversationTitle | WAChatBot
        android.subText | null
        android.car.EXTENSIONS | Bundle[mParcelledData.dataSize=1028]
        android.template | android.aandroid.progress | 0
        android.progressMax | 0
        android.appInfo | ApplicationInfo{f965cc9 com.whatsapp}
        android.showWhen | true
        android.largeIcon | null
        android.infoText | null
        android.progressIndeterminate | false
        android.remoteInputHistory android.messages | [Landroid.os.Parcelable;@58b0eef
        android.showWhen | true
        android.largeIcon | android.graphics.Bitmap@69c5dfc
        android.messagingUser | android.app.Person@21efc85
        android.infoText | null
        android.wearable.EXTENSIONS | Bundle[mParcelledData.dataSize=996]
        android.progressIndeterminate | false
        android.remoteInputHistory | null
        android.isGroupConversation | true

        Android 9.0 Pie Personal Chat Notification:
        android.title | My Jio
        android.reduced.images | true
        android.subText | null
        android.car.EXTENSIONS | Bundle[mParcelledData.dataSize=1020]
        android.template | android.app.Notification$MessagingStyle
        android.showChronometer | false
        android.icon | 2131231578
        android.text | Hello
        android.progress | 0
        android.progressMax | 0
        android.selfDisplayName | You
        android.appInfo | ApplicationInfo{6c058a7 com.whatsapp}
        android.messages | [Landroid.os.Parcelable;@312d154
        android.showWhen | true
        android.largeIcon | android.graphics.Bitmap@5c2e8fd
        android.messagingUser | android.app.Person@7a879f2
        android.infoText | null
        android.wearable.EXTENSIONS | Bundle[mParcelledData.dataSize=980]
        android.progressIndeterminate | false
        android.remoteInputHistory | null
        android.isGroupConversation | false
        */
    }
}
