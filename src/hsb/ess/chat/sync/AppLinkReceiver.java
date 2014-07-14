/**Ford Motor Company
 * September 2012
 * Elizabeth Halash
 */

package hsb.ess.chat.sync;

import hsb.ess.chat.ui.ConversationActivity;
import hsb.ess.chat.ui.ManageAccountActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AppLinkReceiver extends BroadcastReceiver {
	private BluetoothAdapter mBtAdapter;

	public void onReceive(Context context, Intent intent) {
		final BluetoothDevice bluetoothDevice = (BluetoothDevice) intent
				.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
		// Log.v(TAG, "R intent Event: " + intent.getAction());

		// if SYNC connected to phone via bluetooth, start service (which starts
		// proxy)
		if (intent.getAction().compareTo(BluetoothDevice.ACTION_ACL_CONNECTED) == 0) {
			if (bluetoothDevice.getName() != null) {
				if (bluetoothDevice.getName().contains("SYNC")) {
					AppLinkService serviceInstance = AppLinkService
							.getInstance();
					if (serviceInstance == null) {
						Intent startIntent = new Intent(context,
								AppLinkService.class);
						startIntent.putExtras(intent);
						context.startService(startIntent);
					}
				}
			}

			// if SYNC is disconnected from phone or BT disabled, stop service
			// (and thus the proxy)
		} else if (intent.getAction().equals(
				BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
			if (bluetoothDevice.getName().contains("SYNC")) {
				AppLinkService serviceInstance = AppLinkService.getInstance();
				if (serviceInstance != null) {
					Intent stopIntent = new Intent(context,
							AppLinkService.class);
					stopIntent.putExtras(intent);
					context.stopService(stopIntent);
					Log.i("Reciever", "Action_acl_dis Service Stopped");
				}
				if (LockScreenActivity.getInstance() != null)
					LockScreenActivity.getInstance().finish();
				
				if (ManageAccountActivity.getInstance() != null)
					ManageAccountActivity.getInstance().finish();

				if (ConversationActivity.getInstance() != null)
					ConversationActivity.getInstance().finish();

				
//				Log.i("Reciever", "is my service is running?"+Utils.isMyServiceRunning(AppLinkReceiver.this, AppLinkService.class));
			}

			// Listen for STATE_CHANGED as double-check when BT turned off & not
			// connected to BT
		} else if (intent.getAction().equals(
				BluetoothAdapter.ACTION_STATE_CHANGED)) {
			if ((intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == (BluetoothAdapter.STATE_TURNING_OFF))) {
				AppLinkService serviceInstance = AppLinkService.getInstance();
				if (serviceInstance != null) {
					Intent stopIntent = new Intent(context,
							AppLinkService.class);
					stopIntent.putExtras(intent);
					context.stopService(stopIntent);
					Log.i("Reciever", "Service Stopped");
				}
				if (LockScreenActivity.getInstance() != null)
					LockScreenActivity.getInstance().finish();
				
				if (ManageAccountActivity.getInstance() != null)
					ManageAccountActivity.getInstance().finish();

				if (ConversationActivity.getInstance() != null)
					ConversationActivity.getInstance().finish();

			
			}

			// Listen for phone reboot and start service
		} else if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
			mBtAdapter = BluetoothAdapter.getDefaultAdapter();
			if (mBtAdapter != null) {
				if (mBtAdapter.isEnabled()) {
					Intent startIntent = new Intent(context,
							AppLinkService.class);
					startIntent.putExtras(intent);
					context.startService(startIntent);
				}
			}
		} else if (intent.getAction().equals(
				android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
			// signal your service to stop playback
		}
	}
}