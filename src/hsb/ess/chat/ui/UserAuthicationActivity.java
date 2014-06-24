package hsb.ess.chat.ui;

import hsb.ess.chat.R;
import hsb.ess.chat.entities.Account;
import hsb.ess.chat.utils.Validator;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class UserAuthicationActivity extends XmppActivity {

	Button _signinButton;
	EditText _usernameEditText, _passwordEditText;
	protected Account account;

	public void setAccount(Account account) {
		this.account = account;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.user_login);

		_signinButton = (Button) findViewById(R.id.btn_signin);
		_usernameEditText = (EditText) findViewById(R.id.username_edittxt);
		_passwordEditText = (EditText) findViewById(R.id.password_edittxt);

		_signinButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String username = _usernameEditText.getText().toString();
				String password = _passwordEditText.getText().toString();
				checkValidation(username, password);
			}
		});

	}

	private void checkValidation(String username, String password) {

		//if (username.length() > 0 && password.length() > 5) {
		if (username.length() > 0 ) {
			Log.i("hemant", "valid");

			// EditText jidEdit = (EditText) d.findViewById(R.id.account_jid);
			// String jid = jidEdit.getText().toString();
			// EditText passwordEdit = (EditText) d
			// .findViewById(R.id.account_password);
			// EditText passwordConfirmEdit = (EditText)
			// d.findViewById(R.id.account_password_confirm2);
			// String password = passwordEdit.getText().toString();
			// String passwordConfirm =
			// passwordConfirmEdit.getText().toString();
			// CheckBox register = (CheckBox)
			// d.findViewById(R.id.edit_account_register_new);
			// String username;
			username = username + "@" + Utils.HOST_DATA;
			String server;
			if (Validator.isValidJid(username)) {
				String[] parts = username.split("@");
				username = parts[0];
				server = parts[1];
			} else {
				// .setError(getString(R.string.invalid_jid));
				return;
			}
			// if (register.isChecked()) {
			// if (!passwordConfirm.equals(password)) {
			// passwordConfirmEdit.setError(getString(R.string.passwords_do_not_match));
			// return;
			// }
			// }
			if (account != null) {
				account.setPassword(password);
				account.setUsername(username);
				account.setServer(server);
			} else {
				account = new Account(username, server, password);
				account.setOption(Account.OPTION_USETLS, true);
				account.setOption(Account.OPTION_USECOMPRESSION, true);
			}
			account.setOption(Account.OPTION_REGISTER, false);
			xmppConnectionService.createAccount(account);
			Intent i = new Intent(UserAuthicationActivity.this,
					ConversationActivity.class);
			startActivity(i);
			finish();
			// if (listener != null) {
			// listener.onAccountEdited(account);
			// d.dismiss();
			// }

		}
	}

	@Override
	void onBackendConnected() {

	}
}
