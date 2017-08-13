package id.wawestudio.ble_chat_copy;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import id.wawestudio.ble_chat_copy.activity.Main;

/**
 * Created by Dony on 7/27/2017.
 */

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Handler h = new Handler(){

            @Override
            public void handleMessage(Message msg) {
                    Intent i = new Intent(SplashActivity.this,Main.class);
                    startActivity(i);
                    finish();
            }
        };
        h.sendEmptyMessageDelayed(0, 1500); // 1500 is time in miliseconds
    }
}
