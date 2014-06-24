/**Ford Motor Company
 * September 2012
 * Elizabeth Halash
 */

package hsb.ess.chat.sync;

import hsb.ess.chat.R;
import hsb.ess.chat.ui.ConversationActivity;
import hsb.ess.chat.ui.ManageAccountActivity;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.ford.syncV4.proxy.SyncProxyALM;

public class LockScreenActivity extends Activity {
	int itemcmdID = 0;
	int subMenuId = 0;
	private static LockScreenActivity instance = null;
	Button _syncExitButton;

	public static LockScreenActivity getInstance() {
		return instance;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		instance = this;
		setContentView(R.layout.lockscreen);
		_syncExitButton = (Button) findViewById(R.id.lockreset);

		_syncExitButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// if not already started, show main activity and end lock
				// screen activity
				// if(MainActivity.getInstance() == null) {
				// Intent i = new Intent(getBaseContext(), MainActivity.class);
				// i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				// getApplication().startActivity(i);
				// }

				// reset proxy; do not shut down service
				AppLinkService serviceInstance = AppLinkService.getInstance();
				if (serviceInstance != null) {
					SyncProxyALM proxyInstance = serviceInstance.getProxy();
					if (proxyInstance != null) {
						serviceInstance.reset();
					} else {
						serviceInstance.startProxy();
					}
				}
				if (ManageAccountActivity.getInstance() != null)
					ManageAccountActivity.getInstance().finish();

				if (ConversationActivity.getInstance() != null)
					ConversationActivity.getInstance().finish();

				exit();
			}
		});
	}

	// disable back button on lockscreen
	@Override
	public void onBackPressed() {
	}

	public void exit() {
		super.finish();
	}

	public void onDestroy() {
		super.onDestroy();
		instance = null;
	}
}