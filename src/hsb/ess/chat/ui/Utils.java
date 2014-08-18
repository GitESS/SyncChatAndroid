package hsb.ess.chat.ui;

import hsb.ess.chat.services.XmppConnectionService;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;

public class Utils {
	//public static final String HOST_DATA ="192.168.1.73"; 
	
	//132.132.2.234//Local
	public static final String HOST_DATA = "jabber.ru.com";
	//	public static final String HOST_DATA ="182.73.73.181";  //Public
//	public static final String HOST_DATA ="132.132.2.234";   //MAC
	
	
	//public static final String HOST_DATA = "jabber.ru.com";
		//	public static final String HOST_DATA ="182.73.73.181";  //Public
	//	public static final String HOST_DATA ="132.132.2.234";   //MAC
	public static XmppConnectionService tempxmppConnectionService;
	public static final String LOG_IMAGE="Image_Transfer";
	public static final String LOG_AUDIO="Audio_Transfer";
		
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
