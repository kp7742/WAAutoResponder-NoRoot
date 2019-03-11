package com.kmods.waautoresponder;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends AppCompatActivity {
    ToggleButton bot_status;//Toggle to turn on/off the Bot
    SharedPreferences sharedPreferences;//Shared Prefrence for Bot

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //initialization
        bot_status = findViewById(R.id.toggleButton);
        sharedPreferences = getSharedPreferences(Const.BOT, Context.MODE_PRIVATE);

        //Ask for Notification Permission
        if(!Settings.Secure.getString(getContentResolver(),"enabled_notification_listeners").contains(getPackageName())){
            Toast.makeText(this, "Please Enable Notification Access", Toast.LENGTH_LONG).show();
            startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
        }

        bot_status.setChecked(getStatus());
        bot_status.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setStatus(!getStatus());//Invert or Toggle :P
            }
        });
    }

    /*
    * Get Status of Toggle
    */
    private boolean getStatus(){
        return sharedPreferences.getBoolean(Const.STATUS, false);
    }

    /*
     * Set Status of Toggle
     */
    private void setStatus(boolean status){
        sharedPreferences.edit().putBoolean(Const.STATUS, status).apply();
    }
}
