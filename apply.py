# يُشغَّل تلقائيًا داخل GitHub بعد `npx cap add android`
import os, re, shutil
PKG = "com.almuslim.student"
base = "android/app/src/main"
jd = f"{base}/java/{PKG.replace('.', '/')}"
os.makedirs(jd, exist_ok=True)

for f in ["AdhanSchedulerPlugin", "AdhanReceiver", "AdhanService", "BootReceiver"]:
    t = open(f + ".java", encoding="utf-8").read()
    t = re.sub(r"^package [^;]+;.*$", "package " + PKG + ";", t, count=1, flags=re.M)
    open(f"{jd}/{f}.java", "w", encoding="utf-8").write(t)

open(f"{jd}/MainActivity.java", "w", encoding="utf-8").write(f"""package {PKG};

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {{
  @Override
  public void onCreate(Bundle b) {{
    registerPlugin(AdhanSchedulerPlugin.class);
    super.onCreate(b);
  }}
}}
""")

mp = f"{base}/AndroidManifest.xml"
m = open(mp, encoding="utf-8").read()
perms = ["INTERNET", "POST_NOTIFICATIONS", "SCHEDULE_EXACT_ALARM", "RECEIVE_BOOT_COMPLETED",
         "WAKE_LOCK", "FOREGROUND_SERVICE", "FOREGROUND_SERVICE_MEDIA_PLAYBACK",
         "ACCESS_FINE_LOCATION", "ACCESS_COARSE_LOCATION"]
add = "".join(f'    <uses-permission android:name="android.permission.{p}"/>\n'
              for p in perms if f"android.permission.{p}" not in m)
comp = """    <receiver android:name=".AdhanReceiver" android:exported="false"/>
    <receiver android:name=".BootReceiver" android:exported="true">
      <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED"/>
        <action android:name="android.intent.action.MY_PACKAGE_REPLACED"/>
      </intent-filter>
    </receiver>
    <service android:name=".AdhanService" android:exported="false"
             android:foregroundServiceType="mediaPlayback"/>
"""
m = m.replace("<application", add + "    <application", 1)
m = m.replace("</application>", comp + "  </application>", 1)
open(mp, "w", encoding="utf-8").write(m)

if os.path.exists("adhan.mp3"):
    os.makedirs(f"{base}/res/raw", exist_ok=True)
    shutil.copy("adhan.mp3", f"{base}/res/raw/adhan.mp3")
print("native patch applied")
