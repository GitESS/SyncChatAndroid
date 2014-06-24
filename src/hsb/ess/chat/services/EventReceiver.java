package hsb.ess.chat.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
public class EventReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		Intent mIntentForService = new Intent(context,
				XmppConnectionService.class);
		if (intent.getAction() != null) {
			mIntentForService.setAction(intent.getAction());
		} else {
			mIntentForService.setAction("other");
		}
		context.startService(mIntentForService);
	}

}
