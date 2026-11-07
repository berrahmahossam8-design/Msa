package com.example.app; // <-- غيّره إلى appId تطبيقك

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import androidx.core.content.ContextCompat;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;
import org.json.JSONArray;
import org.json.JSONObject;

@CapacitorPlugin(name = "AdhanScheduler", permissions = {
    @Permission(strings = {"android.permission.POST_NOTIFICATIONS"}, alias = "notif")})
public class AdhanSchedulerPlugin extends Plugin {
  static final String PREFS = "adhan";

  @PluginMethod
  public void schedule(PluginCall c) {
    try {
      JSArray items = c.getArray("items");
      getContext().getSharedPreferences(PREFS, 0).edit()
          .putString("items", items == null ? "[]" : items.toString())
          .putString("voice", c.getString("voice", "")).apply();
      scheduleAll(getContext());
      c.resolve();
    } catch (Exception e) { c.reject(String.valueOf(e.getMessage())); }
  }

  static void scheduleAll(Context ctx) throws Exception {
    SharedPreferences sp = ctx.getSharedPreferences(PREFS, 0);
    AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
    for (int i = 0, o = sp.getInt("n", 0); i < o; i++) am.cancel(pi(ctx, i, "", "", false));
    JSONArray a = new JSONArray(sp.getString("items", "[]"));
    int n = Math.min(a.length(), 60);
    boolean exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms();
    for (int i = 0; i < n; i++) {
      JSONObject o = a.getJSONObject(i);
      long at = o.getLong("at");
      if (at <= System.currentTimeMillis()) continue;
      PendingIntent p = pi(ctx, i, o.optString("name"), o.optString("time"), o.optBoolean("silent"));
      if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p);
      else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p);
    }
    sp.edit().putInt("n", n).apply();
  }

  static PendingIntent pi(Context ctx, int i, String name, String time, boolean silent) {
    Intent it = new Intent(ctx, AdhanReceiver.class)
        .putExtra("name", name).putExtra("time", time).putExtra("silent", silent);
    return PendingIntent.getBroadcast(ctx, 2000 + i, it,
        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
  }

  @PluginMethod
  public void requestNotif(PluginCall c) {
    if (Build.VERSION.SDK_INT >= 33 && getPermissionState("notif") != PermissionState.GRANTED)
      requestPermissionForAlias("notif", c, "notifCb");
    else c.resolve();
  }

  @PermissionCallback
  private void notifCb(PluginCall c) { c.resolve(); }

  @PluginMethod
  public void status(PluginCall c) {
    AlarmManager am = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
    boolean ok = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms();
    JSObject r = new JSObject();
    r.put("overlay", ok); // الواجهة تستخدم هذا الحقل: true = كل شيء جاهز
    r.put("exact", ok);
    c.resolve(r);
  }

  @PluginMethod
  public void openOverlaySettings(PluginCall c) { // يفتح إذن «المنبّهات والتذكيرات»
    if (Build.VERSION.SDK_INT >= 31)
      open(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
          Uri.parse("package:" + getContext().getPackageName())));
    c.resolve();
  }

  @PluginMethod
  public void openBatterySettings(PluginCall c) {
    open(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
    c.resolve();
  }

  @PluginMethod
  public void testNow(PluginCall c) {
    Context ctx = getContext();
    Intent s = new Intent(ctx, AdhanService.class)
        .putExtra("name", c.getString("name", "")).putExtra("time", c.getString("time", ""));
    ContextCompat.startForegroundService(ctx, s);
    c.resolve();
  }

  @PluginMethod
  public void updateWidget(PluginCall c) { c.resolve(); }

  private void open(Intent i) {
    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    try { getContext().startActivity(i); } catch (Exception ignored) {}
  }
}
