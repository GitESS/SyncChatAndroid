package hsb.ess.chat.ui;

import hsb.ess.chat.R;
import hsb.ess.chat.entities.Account;
import hsb.ess.chat.services.XmppConnectionService;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;

public class TestActivity extends Activity {
	public XmppConnectionService xmppConnectionService;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.test_activity);

		// contactsList = new ArrayList<Contact>();
		List<Account> accountList = new ArrayList<Account>();
		accountList
				.addAll(xmppConnectionService
						.getAccounts());

		Account Account_name = accountList.get(0);

		String name = Account_name.getUsername();
		// contactsList = accountList.get(0).getRoster().getContacts();
		// if(contactsList.size()!=0){
		// Log.i("service", "Contact List" + contactsList.size());
		// }
		// Log.i("service", "Contacts " +contactsList.size());
		// ONLINE_FRIENDS_ID = new ArrayList<Integer>();
		// for (int i = 0; i < contactsList.size(); i++) {
		// ONLINE_FRIENDS_ID.add(CHOICE_FRIENDS_ID + i);
		// Contact name = contactsList.get(i); 
		// String contactName = name.getDisplayName();
		// Choice one = new Choice();
		// one.setChoiceID(CHOICE_FRIENDS_ID + i);
		// one.setMenuName(contactName);
		// one.setVrCommands(new Vector<String>(Arrays
		// .asList(new String[] { contactName })));
		// one.setImage(null);
		// commands.add(one);

		// }
	}
}
