package hsb.ess.chat.ui;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;

public class Utils {
//	public static final String HOST_DATA ="192.168.1.73";    //Local
	public static final String HOST_DATA ="182.73.73.181";  //Public
//	public static final String HOST_DATA ="132.132.2.234";   //MAC
	
	
	public static boolean isMyServiceRunning(Activity activity ,Class<?> serviceClass) {
		ActivityManager manager = (ActivityManager) activity
				.getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			if (serviceClass.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}
}
