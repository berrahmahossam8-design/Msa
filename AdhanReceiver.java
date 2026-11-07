package com.example.app; // <-- غيّره

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.content.ContextCompat;

public class AdhanReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context c, Intent i) {
    ContextCompat.startForegroundService(c, new Intent(c, AdhanService.class).putExtras(i));
  }
}
