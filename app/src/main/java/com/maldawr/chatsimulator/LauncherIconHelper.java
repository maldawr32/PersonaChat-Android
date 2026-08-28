package com.maldawr.chatsimulator;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.widget.Toast;

public final class LauncherIconHelper {
    private LauncherIconHelper(){}
    public static void apply(Context c,String selected){PackageManager pm=c.getPackageManager();String pkg=c.getPackageName();String[] names={"LauncherGreen","LauncherBlue","LauncherPurple","LauncherOrange","LauncherGraphite"};for(String name:names)pm.setComponentEnabledSetting(new ComponentName(pkg,pkg+"."+name),PackageManager.COMPONENT_ENABLED_STATE_DISABLED,PackageManager.DONT_KILL_APP);String target="blue".equals(selected)?"LauncherBlue":"purple".equals(selected)?"LauncherPurple":"orange".equals(selected)?"LauncherOrange":"graphite".equals(selected)?"LauncherGraphite":"LauncherGreen";pm.setComponentEnabledSetting(new ComponentName(pkg,pkg+"."+target),PackageManager.COMPONENT_ENABLED_STATE_ENABLED,PackageManager.DONT_KILL_APP);Store.setIcon(c,selected);Toast.makeText(c,"Launcher icon updated",Toast.LENGTH_SHORT).show();}
}
