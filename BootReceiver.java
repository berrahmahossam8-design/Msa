package com.example.app; // <-- غيّره

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context c, Intent i) {
    try { AdhanSchedulerPlugin.scheduleAll(c); } catch (Exception ignored) {}
  }
}
