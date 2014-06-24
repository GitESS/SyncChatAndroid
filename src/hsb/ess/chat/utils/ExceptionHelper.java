package hsb.ess.chat.utils;

import hsb.ess.chat.R;
import hsb.ess.chat.entities.Account;
import hsb.ess.chat.entities.Conversation;
import hsb.ess.chat.entities.Message;
import hsb.ess.chat.services.XmppConnectionService;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class ExceptionHelper {
	public static void init(Context context) {
		if(!(Thread.getDefaultUncaughtExceptionHandler() instanceof ExceptionHandler)) {
		    Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(context));
		}
	}
	
	public static void checkForCrash(Context context, final XmppConnectionService service) {
		try {
			final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
			boolean neverSend = preferences.getBoolean("never_send",false);
			if (neverSend) {
				return;
			}
			List<Account> accounts = service.getAccounts();
			Account account = null;
			for(int i = 0; i < accounts.size(); ++i) {
				if (!accounts.get(i).isOptionSet(Account.OPTION_DISABLED)) {
					account = accounts.get(i);
					break;
				}
			}
			if (account==null) {
				return;
			}
			final Account finalAccount = account;
			FileInputStream file = context.openFileInput("stacktrace.txt");
			InputStreamReader inputStreamReader = new InputStreamReader(
                    file);
            BufferedReader bufferedReader = new BufferedReader(
                    inputStreamReader);
            final StringBuilder stacktrace = new StringBuilder();
            String line;
            while((line = bufferedReader.readLine()) != null) {
            	stacktrace.append(line);
            	stacktrace.append('\n');
            }
            file.close();
            context.deleteFile("stacktrace.txt");
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder.setTitle(context.getString(R.string.crash_report_title));
		//	builder.setMessage(context.getText(R.string.crash_report_message));
			builder.setPositiveButton(context.getText(R.string.send_now), new OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					
						Log.d("xmppService","using account="+finalAccount.getJid()+" to send in stack trace");
						Conversation conversation = service.findOrCreateConversation(finalAccount, "bugs@siacs.eu", false);
						Message message = new Message(conversation, stacktrace.toString(), Message.ENCRYPTION_NONE);
						service.sendMessage(message);
				}
			});
			builder.setNegativeButton(context.getText(R.string.send_never),new OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					preferences.edit().putBoolean("never_send", true).commit();
				}
			});
			builder.create().show();
		} catch (FileNotFoundException e) {
			return;
		} catch (IOException e) {
			return;
		}
		
	}
}
