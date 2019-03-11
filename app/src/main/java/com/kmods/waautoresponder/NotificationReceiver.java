package com.kmods.waautoresponder;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;

import androidx.core.app.NotificationCompat;
import androidx.core.app.RemoteInput;

public class NotificationReceiver extends NotificationListenerService {
    static boolean isRunning = false;//is Running Bool
    static Notification notification;
    static Bundle bundle;
    static ArrayList<RemoteInput> remoteInputs;

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
                            bundle = notification.extras;
                            //logBundle(bundle);
                            remoteInputs = getRemoteInputs(notification);
                            if(remoteInputs != null && remoteInputs.size() > 0){
                                Object isGroupConversation = bundle.get("android.isGroupConversation");
                                String conversationTitle = bundle.getString("android.conversationTitle");

                                if(isGroupConversation != null){
                                    boolean isGroup = (((boolean) isGroupConversation) && (conversationTitle != null));//Group Params
                                    Object title = bundle.get("android.title");//Chat Title
                                    Object text = bundle.get("android.text");//Chat Text

                                    if(title != null && text != null) {
                                        //Common Replies
                                        if (text.equals("@helpkp")) {
                                            sendMsg("*KP's Bot Commands* :-\n\n@emailkp - to get my official EMail Address.\n\n@webkp - to get my offical website link.");
                                        } else if (text.equals("@emailkp")) {
                                            sendMsg("*KP's Official EMail* :- \n\npatel.kuldip91@gmail.com");
                                        } else if (text.equals("@webkp")) {
                                            sendMsg("*KP's Official Website* :- \n\nhttps://kuldippatel.dev");
                                        }

                                        //Group Specific Replies
                                        if(isGroup) {
                                            String[] title_split = parseTitle(String.valueOf(title));
                                            String group_name = title_split[0];
                                            String sender = title_split[(title_split.length - 1)];

                                            if(text.equals("@warnings")){
                                                sendMsg("Hello " +
                                                        sender +
                                                        " :-\n\n" +
                                                        "Sorry But This Feature is Still Under Construction!");
                                            }

                                            //for Comp162 and 16-Comp Group
                                            if(String.valueOf(title).contains("Comp162") || String.valueOf(title).contains("16-Comp")) {
                                                if (text.equals("@help")) {
                                                    sendMsg("*Comp162 Group Commands* :-\n" +
                                                            "\n" +
                                                            "@goodmorning and @goodnight - to greet group Members.");
                                                } else if(text.equals("@goodmorning")){
                                                    sendMsg("Good Morning All!");
                                                } else if(text.equals("@goodnight")){
                                                    sendMsg("Good Night All!");
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e){
                        notification = null;
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
     * get Remote Input of Notification
     */
    private ArrayList<RemoteInput> getRemoteInputs(Notification notification){
        ArrayList<RemoteInput> remoteInputs = new ArrayList<>();
        NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender(notification);
        for(NotificationCompat.Action act : wearableExtender.getActions()) {
            if(act != null && act.getRemoteInputs() != null) {
                remoteInputs.addAll(Arrays.asList(act.getRemoteInputs()));
            }
        }
        return remoteInputs;
    }

    /*
     * Send/Reply through Notification
     */
    private void sendMsg(String msg){
        RemoteInput[] allremoteInputs = new RemoteInput[remoteInputs.size()];
        Intent localIntent = new Intent();
        localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Iterator it = remoteInputs.iterator();
        int i=0;
        while (it.hasNext()) {
            allremoteInputs[i] = (RemoteInput) it.next();
            bundle.putCharSequence(allremoteInputs[i].getResultKey(), msg);//This work, apart from Hangouts as probably they need additional parameter (notification_tag?)
            i++;
        }
        RemoteInput.addResultsToIntent(allremoteInputs, localIntent, bundle);
        try {
            Objects.requireNonNull(replyAction(notification)).actionIntent.send(this, 0, localIntent);
        } catch (PendingIntent.CanceledException e) {
            Log.e(Const.LOG, "replyToLastNotification error: " + e.getLocalizedMessage());
        }
    }

    /*
     * Returns Reply Action of Notification
     */
    private NotificationCompat.Action replyAction(Notification notification) {
        NotificationCompat.Action action;
        for (NotificationCompat.Action action2 : new NotificationCompat.WearableExtender(notification).getActions()) {
            if (isAllowFreeFormInput(action2)) {
                return action2;
            }
        }
        if (!(notification == null || notification.actions == null)) {
            for (int i = 0; i < NotificationCompat.getActionCount(notification); i++) {
                action = NotificationCompat.getAction(notification, i);
                if (isAllowFreeFormInput(action)) {
                    return action;
                }
            }
        }
        return null;
    }

    /*
     * Checks for Text Input
     */
    private boolean isAllowFreeFormInput(NotificationCompat.Action action) {
        if (action.getRemoteInputs() == null) {
            return false;
        }
        for (RemoteInput allowFreeFormInput : action.getRemoteInputs()) {
            if (allowFreeFormInput.getAllowFreeFormInput()) {
                return true;
            }
        }
        return false;
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

    /*
     * Parse Group title with Combined Messages
     */
    private String[] parseTitle(String title){
        String[] title_split = String.valueOf(title).split("[^\\s\\w\\d]");
        StringBuilder builder = new StringBuilder();
        for(int i=0;i< title_split.length;i++){
            if(i>0){
                builder.append(",");
            }
            builder.append(title_split[i]);
        }
        return builder.toString().split(",");
    }
}
