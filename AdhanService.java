package com.example.app; // <-- غيّره

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import androidx.core.app.NotificationCompat;

public class AdhanService extends Service {
  static final String CH = "adhan_channel";
  MediaPlayer mp;

  @Override
  public int onStartCommand(Intent in, int flags, int startId) {
    if (in != null && "STOP".equals(in.getAction())) { stopSelf(); return START_NOT_STICKY; }
    String name = in == null ? "" : in.getStringExtra("name");
    String time = in == null ? "" : in.getStringExtra("time");
    boolean silent = in != null && in.getBooleanExtra("silent", false);

    if (Build.VERSION.SDK_INT >= 26) {
      NotificationChannel ch = new NotificationChannel(CH, "الأذان", NotificationManager.IMPORTANCE_HIGH);
      ch.setSound(null, null);
      getSystemService(NotificationManager.class).createNotificationChannel(ch);
    }
    PendingIntent open = PendingIntent.getActivity(this, 0,
        getPackageManager().getLaunchIntentForPackage(getPackageName()), PendingIntent.FLAG_IMMUTABLE);
    PendingIntent stop = PendingIntent.getService(this, 1,
        new Intent(this, AdhanService.class).setAction("STOP"), PendingIntent.FLAG_IMMUTABLE);
    android.app.Notification n = new NotificationCompat.Builder(this, CH)
        .setSmallIcon(getApplicationInfo().icon)
        .setContentTitle("حان وقت صلاة " + name)
        .setContentText(time)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setContentIntent(open)
        .addAction(0, "إيقاف الأذان", stop)
        .build();
    if (Build.VERSION.SDK_INT >= 29)
      startForeground(7, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
    else startForeground(7, n);

    if (silent) { // وقت الدراسة: إشعار بدون صوت
      stopForeground(false);
      new Handler(Looper.getMainLooper()).postDelayed(this::stopSelf, 3000);
      return START_NOT_STICKY;
    }
    play();
    return START_NOT_STICKY;
  }

  void play() {
    try {
      if (mp != null) mp.release();
      mp = new MediaPlayer();
      mp.setAudioAttributes(new AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_ALARM)
          .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
      int raw = getResources().getIdentifier("adhan", "raw", getPackageName());
      if (raw != 0) {
        AssetFileDescriptor f = getResources().openRawResourceFd(raw);
        mp.setDataSource(f.getFileDescriptor(), f.getStartOffset(), f.getLength());
        f.close();
      } else {
        mp.setDataSource(getSharedPreferences("adhan", 0).getString("voice", ""));
      }
      mp.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
      mp.setOnPreparedListener(MediaPlayer::start);
      mp.setOnCompletionListener(m -> stopSelf());
      mp.setOnErrorListener((m, w, e) -> { stopSelf(); return true; });
      mp.prepareAsync();
    } catch (Exception e) { stopSelf(); }
  }

  @Override public void onDestroy() { if (mp != null) { mp.release(); mp = null; } super.onDestroy(); }
  @Override public IBinder onBind(Intent i) { return null; }
}
