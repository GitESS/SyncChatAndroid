package hsb.ess.chat.ui;

import hsb.ess.chat.R;
import hsb.ess.chat.entities.Account;
import hsb.ess.chat.sync.AppLinkService;
import hsb.ess.chat.sync.LockScreenActivity;
import hsb.ess.chat.ui.EditAccount.EditAccountListener;
import hsb.ess.chat.xmpp.OnTLSExceptionReceived;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.ford.syncV4.proxy.SyncProxyALM;

public class ManageAccountActivity extends XmppActivity {

	protected boolean isActionMode = false;
	protected ActionMode actionMode;
	protected Account selectedAccountForActionMode = null;
	protected ManageAccountActivity activity = this;
	public boolean isMyAccountIsOnline = false;
	private static ManageAccountActivity instance = null;

	public boolean isMyAccountIsOnline() {
		return isMyAccountIsOnline;
	}

	public static ManageAccountActivity getInstance() {
		return instance;
	}

	public void setMyAccountIsOnline(boolean isMyAccountIsOnline) {
		this.isMyAccountIsOnline = isMyAccountIsOnline;
	}

	protected boolean firstrun = true;
	protected String FromServerice = "";
	protected List<Account> accountList = new ArrayList<Account>();
	protected ListView accountListView;
	protected ArrayAdapter<Account> accountListViewAdapter;
	protected OnAccountListChangedListener accountChanged = new OnAccountListChangedListener() {

		@Override
		public void onAccountListChangedListener() {
			accountList.clear();
			accountList.addAll(xmppConnectionService.getAccounts());
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					accountListViewAdapter.notifyDataSetChanged();
				}
			});
		}
	};

	protected OnTLSExceptionReceived tlsExceptionReceived = new OnTLSExceptionReceived() {

		@Override
		public void onTLSExceptionReceived(final String fingerprint,
				final Account account) {
			activity.runOnUiThread(new Runnable() {

				@Override
				public void run() {

					Log.i("fingerprint", fingerprint);
					account.setSSLCertFingerprint(fingerprint);
					activity.xmppConnectionService.updateAccount(account);
					// activity.xmppConnectionService.reconnectAccount(account,
					// true);
					// AlertDialog.Builder builder = new
					// AlertDialog.Builder(activity);
					// builder.setTitle(getString(R.string.account_status_error));
					// builder.setIconAttribute(android.R.attr.alertDialogIcon);
					// View view = (View)
					// getLayoutInflater().inflate(R.layout.cert_warning, null);
					// TextView sha = (TextView) view.findViewById(R.id.sha);
					// TextView hint = (TextView) view.findViewById(R.id.hint);
					// StringBuilder humanReadableSha = new StringBuilder();
					// humanReadableSha.append(fingerprint);
					// for(int i = 2; i < 59; i += 3) {
					// if ((i==14)||(i==29)||(i==44)) {
					// humanReadableSha.insert(i, "\n");
					// } else {
					// humanReadableSha.insert(i, ":");
					// }
					//
					// }
					// hint.setText(getString(R.string.untrusted_cert_hint,account.getServer()));
					// sha.setText(humanReadableSha.toString());
					// builder.setView(view);
					// builder.setNegativeButton(getString(R.string.certif_no_trust),
					// null);
					// builder.setPositiveButton(getString(R.string.certif_trust),
					// new OnClickListener() {
					//
					// @Override
					// public void onClick(DialogInterface dialog, int which) {
					// account.setSSLCertFingerprint(fingerprint);
					// activity.xmppConnectionService.updateAccount(account);
					// }

				}
			});
			// builder.create().show();
		}
	};

	// });

	// }
	// };

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setContentView(R.layout.manage_accounts);
		instance = this;
		boolean finish = getIntent().getBooleanExtra("finish", false);
		Bundle b = getIntent().getExtras();
		Log.i("hemant", "Bundle" + b);
		if (b != null) {
			Log.i("hemant", "Bundle not null" + b.getString("ServiceIntent"));
			FromServerice = b.getString("ServiceIntent");
		}
		if (finish) {
			// startActivity(new Intent(mContext, LoginActivity.class));
			finish();
			return;
		}

		accountListView = (ListView) findViewById(R.id.account_list);
		accountListViewAdapter = new ArrayAdapter<Account>(
				getApplicationContext(), R.layout.account_row, this.accountList) {
			@Override
			public View getView(int position, View view, ViewGroup parent) {
				Account account = getItem(position);
				if (view == null) {
					LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					view = (View) inflater.inflate(R.layout.account_row, null);
				}
				((TextView) view.findViewById(R.id.account_jid))
						.setText(account.getJid());
				TextView statusView = (TextView) view
						.findViewById(R.id.account_status);
				switch (account.getStatus()) {
				case Account.STATUS_DISABLED:
					statusView
							.setText(getString(R.string.account_status_disabled));
					statusView.setTextColor(0xFF1da9da);
					setMyAccountIsOnline(false);
					break;
				case Account.STATUS_ONLINE:
					statusView
							.setText(getString(R.string.account_status_online));
					statusView.setTextColor(0xFF83b600);
					setMyAccountIsOnline(true);
					Log.i("hemant",
							"manage Conversation Screen is "
									+ ConversationFragment.getInstance()
									+ " Is my account is online? "
									+ isMyAccountIsOnline()
									+ "is service running?"
									+ Utils.isMyServiceRunning(
											ManageAccountActivity.this,
											AppLinkService.class));
					if (Utils.isMyServiceRunning(ManageAccountActivity.this,
							AppLinkService.class)) {
						// AppLinkService serviceInstance = AppLinkService
						// .getInstance();
						// if (serviceInstance != null) {
						// SyncProxyALM proxyInstance = serviceInstance
						// .getProxy();
						Log.i("hemant", "Online bundle value" + FromServerice);
						if (FromServerice.equalsIgnoreCase("start")) {
							Intent i = new Intent(ManageAccountActivity.this,
									LockScreenActivity.class);
							startActivity(i);
						}
						// } else {
						// serviceInstance.startProxy();
						// }
						// }

					}
					break;
				case Account.STATUS_CONNECTING:
					statusView
							.setText(getString(R.string.account_status_connecting));
					statusView.setTextColor(0xFF1da9da);
					setMyAccountIsOnline(false);
					break;
				case Account.STATUS_OFFLINE:
					statusView
							.setText(getString(R.string.account_status_offline));
					statusView.setTextColor(0xFFe92727);
					setMyAccountIsOnline(false);
					LockScreenActivity.getInstance().finish();
					break;
				case Account.STATUS_UNAUTHORIZED:
					statusView
							.setText(getString(R.string.account_status_unauthorized));
					statusView.setTextColor(0xFFe92727);
					setMyAccountIsOnline(false);
					break;
				case Account.STATUS_SERVER_NOT_FOUND:
					statusView
							.setText(getString(R.string.account_status_not_found));
					statusView.setTextColor(0xFFe92727);
					break;
				case Account.STATUS_NO_INTERNET:
					statusView
							.setText(getString(R.string.account_status_no_internet));
					statusView.setTextColor(0xFFe92727);
					break;
				case Account.STATUS_SERVER_REQUIRES_TLS:
					statusView
							.setText(getString(R.string.account_status_requires_tls));
					statusView.setTextColor(0xFFe92727);
					break;
				case Account.STATUS_TLS_ERROR:
					statusView
							.setText(getString(R.string.account_status_error));
					statusView.setTextColor(0xFFe92727);
					break;
				case Account.STATUS_REGISTRATION_FAILED:
					statusView
							.setText(getString(R.string.account_status_regis_fail));
					statusView.setTextColor(0xFFe92727);
					break;
				case Account.STATUS_REGISTRATION_CONFLICT:
					statusView
							.setText(getString(R.string.account_status_regis_conflict));
					statusView.setTextColor(0xFFe92727);
					break;
				case Account.STATUS_REGISTRATION_SUCCESSFULL:
					statusView
							.setText(getString(R.string.account_status_regis_success));
					statusView.setTextColor(0xFF83b600);
					break;
				case Account.STATUS_REGISTRATION_NOT_SUPPORTED:
					statusView
							.setText(getString(R.string.account_status_regis_not_sup));
					statusView.setTextColor(0xFFe92727);
					setMyAccountIsOnline(false);
					break;
				default:
					statusView.setText("");
					break;
				}

				return view;
			}
		};
		final XmppActivity activity = this;
		accountListView.setAdapter(this.accountListViewAdapter);
		accountListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View view,
					int position, long arg3) {
				if (!isActionMode) {
					Account account = accountList.get(position);
					if ((account.getStatus() == Account.STATUS_OFFLINE)
							|| (account.getStatus() == Account.STATUS_TLS_ERROR)) {
						activity.xmppConnectionService.reconnectAccount(
								accountList.get(position), true);
					} else if (account.getStatus() == Account.STATUS_ONLINE) {
						activity.startActivity(new Intent(activity
								.getApplicationContext(),
								ContactsActivity.class));
					} else if (account.getStatus() != Account.STATUS_DISABLED) {
						editAccount(account);
					}
				} else {
					selectedAccountForActionMode = accountList.get(position);
					actionMode.invalidate();
				}
			}
		});
		accountListView
				.setOnItemLongClickListener(new OnItemLongClickListener() {

					@Override
					public boolean onItemLongClick(AdapterView<?> arg0,
							View view, int position, long arg3) {
						if (!isActionMode) {
							accountListView
									.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
							accountListView.setItemChecked(position, true);
							selectedAccountForActionMode = accountList
									.get(position);
							actionMode = activity
									.startActionMode((new ActionMode.Callback() {

										@Override
										public boolean onPrepareActionMode(
												ActionMode mode, Menu menu) {
											if (selectedAccountForActionMode
													.isOptionSet(Account.OPTION_DISABLED)) {
												// menu.findItem(
												// R.id.mgmt_account_enable)
												// .setVisible(true);
												// menu.findItem(
												// R.id.mgmt_account_disable)
												// .setVisible(false);
											} else {
												// menu.findItem(
												// R.id.mgmt_account_disable)
												// .setVisible(true);
												// menu.findItem(
												// R.id.mgmt_account_enable)
												// .setVisible(false);
											}
											return true;
										}

										@Override
										public void onDestroyActionMode(
												ActionMode mode) {
											// TODO Auto-generated method stub

										}

										@Override
										public boolean onCreateActionMode(
												ActionMode mode, Menu menu) {
											MenuInflater inflater = mode
													.getMenuInflater();
											// inflater.inflate(
											// R.menu.manageaccounts_context,
											// menu);
											return true;
										}

										@Override
										public boolean onActionItemClicked(
												final ActionMode mode,
												MenuItem item) {
											// if (item.getItemId() ==
											// R.id.mgmt_account_edit) {
											// editAccount(selectedAccountForActionMode);
											// } else if (item.getItemId() ==
											// R.id.mgmt_account_disable) {
											// selectedAccountForActionMode
											// .setOption(
											// Account.OPTION_DISABLED,
											// true);
											// xmppConnectionService
											// .updateAccount(selectedAccountForActionMode);
											// mode.finish();
											// } else if (item.getItemId() ==
											// R.id.mgmt_account_enable) {
											// selectedAccountForActionMode
											// .setOption(
											// Account.OPTION_DISABLED,
											// false);
											// xmppConnectionService
											// .updateAccount(selectedAccountForActionMode);
											// mode.finish();
											// } else if (item.getItemId() ==
											// R.id.mgmt_account_delete) {
											// AlertDialog.Builder builder = new
											// AlertDialog.Builder(
											// activity);
											// builder.setTitle(getString(R.string.mgmt_account_are_you_sure));
											// builder.setIconAttribute(android.R.attr.alertDialogIcon);
											// builder.setMessage(getString(R.string.mgmt_account_delete_confirm_text));
											// builder.setPositiveButton(
											// getString(R.string.delete),
											// new OnClickListener() {
											//
											// @Override
											// public void onClick(
											// DialogInterface dialog,
											// int which) {
											// xmppConnectionService
											// .deleteAccount(selectedAccountForActionMode);
											// selectedAccountForActionMode =
											// null;
											// mode.finish();
											// }
											// });
											// builder.setNegativeButton(
											// getString(R.string.cancel),
											// null);
											// builder.create().show();
											// } else if (item.getItemId() ==
											// R.id.mgmt_account_announce_pgp) {
											// if (activity.hasPgp()) {
											// mode.finish();
											// announcePgp(
											// selectedAccountForActionMode,
											// null);
											// } else {
											// activity.showInstallPgpDialog();
											// }
											// } else if (item.getItemId() ==
											// R.id.mgmt_otr_key) {
											// AlertDialog.Builder builder = new
											// AlertDialog.Builder(
											// activity);
											// builder.setTitle("OTR Fingerprint");
											// String fingerprintTxt =
											// selectedAccountForActionMode
											// .getOtrFingerprint(getApplicationContext());
											// View view = (View)
											// getLayoutInflater()
											// .inflate(
											// R.layout.otr_fingerprint,
											// null);
											// if (fingerprintTxt != null) {
											// TextView fingerprint = (TextView)
											// view
											// .findViewById(R.id.otr_fingerprint);
											// TextView noFingerprintView =
											// (TextView) view
											// .findViewById(R.id.otr_no_fingerprint);
											// fingerprint
											// .setText(fingerprintTxt);
											// fingerprint
											// .setVisibility(View.VISIBLE);
											// noFingerprintView
											// .setVisibility(View.GONE);
											// }
											// builder.setView(view);
											// builder.setPositiveButton(
											// getString(R.string.done),
											// null);
											// builder.create().show();
											// } else if (item.getItemId() ==
											// R.id.mgmt_account_info) {
											// AlertDialog.Builder builder = new
											// AlertDialog.Builder(
											// activity);
											// builder.setTitle(getString(R.string.account_info));
											// if (selectedAccountForActionMode
											// .getStatus() ==
											// Account.STATUS_ONLINE) {
											// XmppConnection xmpp =
											// selectedAccountForActionMode
											// .getXmppConnection();
											// long connectionAge = (SystemClock
											// .elapsedRealtime() -
											// xmpp.lastConnect) / 60000;
											// long sessionAge = (SystemClock
											// .elapsedRealtime() -
											// xmpp.lastSessionStarted) / 60000;
											// long connectionAgeHours =
											// connectionAge / 60;
											// long sessionAgeHours = sessionAge
											// / 60;
											// View view = (View)
											// getLayoutInflater()
											// .inflate(
											// R.layout.server_info,
											// null);
											// TextView connection = (TextView)
											// view
											// .findViewById(R.id.connection);
											// TextView session = (TextView)
											// view
											// .findViewById(R.id.session);
											// TextView pcks_sent = (TextView)
											// view
											// .findViewById(R.id.pcks_sent);
											// TextView pcks_received =
											// (TextView) view
											// .findViewById(R.id.pcks_received);
											// TextView carbon = (TextView) view
											// .findViewById(R.id.carbon);
											// TextView stream = (TextView) view
											// .findViewById(R.id.stream);
											// TextView roster = (TextView) view
											// .findViewById(R.id.roster);
											// TextView presences = (TextView)
											// view
											// .findViewById(R.id.number_presences);
											// presences.setText(selectedAccountForActionMode
											// .countPresences()
											// + "");
											// pcks_received.setText(""
											// + xmpp.getReceivedStanzas());
											// pcks_sent.setText(""
											// + xmpp.getSentStanzas());
											// if (connectionAgeHours >= 2) {
											// connection
											// .setText(connectionAgeHours
											// + " "
											// + getString(R.string.hours));
											// } else {
											// connection
											// .setText(connectionAge
											// + " "
											// + getString(R.string.mins));
											// }
											// if
											// (xmpp.hasFeatureStreamManagment())
											// {
											// if (sessionAgeHours >= 2) {
											// session.setText(sessionAgeHours
											// + " "
											// + getString(R.string.hours));
											// } else {
											// session.setText(sessionAge
											// + " "
											// + getString(R.string.mins));
											// }
											// stream.setText(getString(R.string.yes));
											// } else {
											// stream.setText(getString(R.string.no));
											// session.setText(connection
											// .getText());
											// }
											// if (xmpp.hasFeaturesCarbon()) {
											// carbon.setText(getString(R.string.yes));
											// } else {
											// carbon.setText(getString(R.string.no));
											// }
											// if
											// (xmpp.hasFeatureRosterManagment())
											// {
											// roster.setText(getString(R.string.yes));
											// } else {
											// roster.setText(getString(R.string.no));
											// }
											// builder.setView(view);
											// } else {
											// builder.setMessage(getString(R.string.mgmt_account_account_offline));
											// }
											// builder.setPositiveButton(
											// getString(R.string.hide),
											// null);
											// builder.create().show();
											// }
											return true;
										}

									}));
							return true;
						} else {
							return false;
						}
					}
				});
		if (!Utils.isMyServiceRunning(this, AppLinkService.class))
			startSyncProxyService();

	}

	@Override
	protected void onStop() {
		if (xmppConnectionServiceBound) {
			xmppConnectionService.removeOnAccountListChangedListener();
			xmppConnectionService.removeOnTLSExceptionReceivedListener();
		}
		super.onStop();
	}

	@Override
	void onBackendConnected() {
		xmppConnectionService.setOnAccountListChangedListener(accountChanged);
		xmppConnectionService
				.setOnTLSExceptionReceivedListener(tlsExceptionReceived);
		this.accountList.clear();
		this.accountList.addAll(xmppConnectionService.getAccounts());
		accountListViewAdapter.notifyDataSetChanged();
		if ((this.accountList.size() == 0) && (this.firstrun)) {
			getActionBar().setDisplayHomeAsUpEnabled(false);
			getActionBar().setHomeButtonEnabled(false);
			addAccount();

			this.firstrun = false;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.manageaccounts, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		// case R.id.action_add_account:
		// addAccount();
		// break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onNavigateUp() {
		if (xmppConnectionService.getConversations().size() == 0) {
			Intent contactsIntent = new Intent(this, ContactsActivity.class);
			contactsIntent.setFlags(
			// if activity exists in stack, pop the stack and go back to it
					Intent.FLAG_ACTIVITY_CLEAR_TOP |
					// otherwise, make a new task for it
							Intent.FLAG_ACTIVITY_NEW_TASK |
							// don't use the new activity animation; finish
							// animation runs instead
							Intent.FLAG_ACTIVITY_NO_ANIMATION);
			startActivity(contactsIntent);
			finish();
			return true;
		} else {
			return super.onNavigateUp();
		}
	}

	private void editAccount(Account account) {
		EditAccount dialog = new EditAccount();
		dialog.setAccount(account);
		dialog.setEditAccountListener(new EditAccountListener() {

			@Override
			public void onAccountEdited(Account account) {
				xmppConnectionService.updateAccount(account);
				if (actionMode != null) {
					actionMode.finish();
				}
			}
		});
		dialog.show(getFragmentManager(), "edit_account");

	}

	protected void addAccount() {
		final Activity activity = this;
		EditAccount dialog = new EditAccount();

		Intent i = new Intent(ManageAccountActivity.this,
				UserAuthicationActivity.class);
		startActivity(i);
		// dialog.setEditAccountListener(new EditAccountListener() {
		//
		// @Override
		// public void onAccountEdited(Account account) {
		// xmppConnectionService.createAccount(account);
		// activity.getActionBar().setDisplayHomeAsUpEnabled(true);
		// activity.getActionBar().setHomeButtonEnabled(true);
		// }
		// });
		// dialog.show(getFragmentManager(), "add_account");
	}

	@Override
	public void onActionModeStarted(ActionMode mode) {
		super.onActionModeStarted(mode);
		this.isActionMode = true;
	}

	@Override
	public void onActionModeFinished(ActionMode mode) {
		super.onActionModeFinished(mode);
		this.isActionMode = false;
		accountListView.clearChoices();
		accountListView.requestLayout();
		accountListView.post(new Runnable() {
			@Override
			public void run() {
				accountListView.setChoiceMode(ListView.CHOICE_MODE_NONE);
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			if (requestCode == REQUEST_ANNOUNCE_PGP) {
				announcePgp(selectedAccountForActionMode, null);
			}
		}
	}

	public void startSyncProxyService() {
		boolean isSYNCpaired = false;
		// Get the local Bluetooth adapter
		BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();

		// BT Adapter exists, is enabled, and there are paired devices with the
		// name SYNC
		// Ideally start service and start proxy if already connected to sync
		// but, there is no way to tell if a device is currently connected (pre
		// OS 4.0)

		if (mBtAdapter != null) {
			if ((mBtAdapter.isEnabled() && mBtAdapter.getBondedDevices()
					.isEmpty() == false)) {
				// Get a set of currently paired devices
				Set<BluetoothDevice> pairedDevices = mBtAdapter
						.getBondedDevices();

				// Check if there is a paired device with the name "SYNC"
				if (pairedDevices.size() > 0) {
					for (BluetoothDevice device : pairedDevices) {
						if (device.getName().toString().contains("SYNC")) {
							isSYNCpaired = true;
							break;
						}
					}
				} else {
					Log.i("TAG", "A No Paired devices with the name sync");
				}

				if (isSYNCpaired == true) {
					if (AppLinkService.getInstance() == null) {
						Intent startIntent = new Intent(this,
								AppLinkService.class);
						this.startService(startIntent);
					} else {
						// if the service is already running and proxy is up,
						// set this as current UI activity
						AppLinkService serviceInstance = AppLinkService
								.getInstance();
						// serviceInstance.setCurrentActivity(getActivity());

						SyncProxyALM proxyInstance = serviceInstance.getProxy();
						if (proxyInstance != null) {
							serviceInstance.reset();
						} else {
							Log.i("TAG", "proxy is null");
							serviceInstance.startProxy();
						}
						Log.i("TAG", " proxyAlive == true success");
					}
				}
			}
		}
	}
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		Bundle bs = getIntent().getExtras();
		Log.i("hemant", "Bundle onresume" + bs);
		if (bs != null) {
			Log.i("hemant", "onresume Bundle not null" + bs.getString("ServiceIntent"));
			FromServerice = bs.getString("ServiceIntent");
		}
	}
}
