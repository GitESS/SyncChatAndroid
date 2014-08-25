package hsb.ess.chat.ui;

import hsb.ess.chat.R;
import hsb.ess.chat.entities.Account;
import hsb.ess.chat.entities.Contact;
import hsb.ess.chat.entities.Conversation;
import hsb.ess.chat.services.XmppConnectionService;
import hsb.ess.chat.utils.CryptoHelper;
import hsb.ess.chat.utils.UIHelper;
import hsb.ess.chat.utils.Validator;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ContactsActivity extends XmppActivity {

	protected List<Contact> rosterContacts = new ArrayList<Contact>();
	protected List<Contact> aggregatedContacts = new ArrayList<Contact>();
	protected ListView contactsView;
	protected Button createGroupButton;
	protected ArrayAdapter<Contact> contactsAdapter;

	protected EditText search;
	protected String searchString = "";
	private TextView contactsHeader;
	private List<Account> accounts;
	private List<Contact> selectedContacts = new ArrayList<Contact>();

	private ContactsActivity activity = this;

	private boolean useSubject = true;
	private boolean isActionMode = false;
	private boolean inviteIntent = false;
	private ActionMode actionMode = null;

	public String MucName;

	public String getMucName() {
		return MucName;
	}

	public void setMucName(String mucName) {
		MucName = mucName;
	}

	private AbsListView.MultiChoiceModeListener actionModeCallback = new AbsListView.MultiChoiceModeListener() {

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			menu.clear();
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.newconversation_context, menu);
			SparseBooleanArray checkedItems = contactsView
					.getCheckedItemPositions();
			selectedContacts.clear();
			for (int i = 0; i < aggregatedContacts.size(); ++i) {
				if (checkedItems.get(i, false)) {
					selectedContacts.add(aggregatedContacts.get(i));
				}
			}
			if (selectedContacts.size() == 0) {
				menu.findItem(R.id.action_start_conversation).setVisible(false);
				menu.findItem(R.id.action_contact_details).setVisible(false);
				menu.findItem(R.id.action_invite).setVisible(false);
				menu.findItem(R.id.action_invite_to_existing).setVisible(false);
			} else if ((selectedContacts.size() == 1) && (!inviteIntent)) {
				menu.findItem(R.id.action_start_conversation).setVisible(true);
				menu.findItem(R.id.action_contact_details).setVisible(true);
				menu.findItem(R.id.action_invite).setVisible(false);
				menu.findItem(R.id.action_invite_to_existing).setVisible(true);
			} else if (!inviteIntent) {
				menu.findItem(R.id.action_start_conversation).setVisible(true);
				menu.findItem(R.id.action_contact_details).setVisible(false);
				menu.findItem(R.id.action_invite).setVisible(false);
				menu.findItem(R.id.action_invite_to_existing).setVisible(true);
			} else {
				menu.findItem(R.id.action_invite).setVisible(true);
				menu.findItem(R.id.action_start_conversation).setVisible(false);
				menu.findItem(R.id.action_contact_details).setVisible(false);
				menu.findItem(R.id.action_invite_to_existing).setVisible(false);
			}
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			// TODO Auto-generated method stub

		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			return true;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			switch (item.getItemId()) {
			case R.id.action_start_conversation:
				if (selectedContacts.size() == 1) {
					startConversation(selectedContacts.get(0));
				} else {
					// startConference();
				}
				break;
			case R.id.action_contact_details:
				Intent intent = new Intent(getApplicationContext(),
						ContactDetailsActivity.class);
				intent.setAction(ContactDetailsActivity.ACTION_VIEW_CONTACT);
				intent.putExtra("account", selectedContacts.get(0).getAccount()
						.getJid());
				intent.putExtra("contact", selectedContacts.get(0).getJid());
				startActivity(intent);
				finish();
				break;
			case R.id.action_invite:
				invite();
				break;
			case R.id.action_invite_to_existing:
				final List<Conversation> mucs = new ArrayList<Conversation>();
				for (Conversation conv : xmppConnectionService
						.getConversations()) {
					if (conv.getMode() == Conversation.MODE_MULTI) {
						mucs.add(conv);
					}
				}
				AlertDialog.Builder builder = new AlertDialog.Builder(activity);
				builder.setTitle(getString(R.string.invite_contacts_to_existing));
				if (mucs.size() >= 1) {
					String[] options = new String[mucs.size()];
					for (int i = 0; i < options.length; ++i) {
						options[i] = mucs.get(i).getName(useSubject);
					}
					builder.setItems(options, new OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							Conversation conversation = mucs.get(which);
							if (isOnline(conversation.getAccount())) {
								xmppConnectionService.inviteToConference(
										conversation, selectedContacts);
								Toast.makeText(activity,
										getString(R.string.invitation_sent),
										Toast.LENGTH_SHORT).show();
								actionMode.finish();
							}
						}
					});
				} else {
					builder.setMessage(getString(R.string.no_open_mucs));
				}
				builder.setNegativeButton(getString(R.string.cancel), null);
				builder.create().show();
				break;
			default:
				break;
			}
			return false;
		}

		@Override
		public void onItemCheckedStateChanged(ActionMode mode, int position,
				long id, boolean checked) {
		}
	};

	private boolean isOnline(Account account) {
		if (account.getStatus() == Account.STATUS_ONLINE) {
			return true;
		} else {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(getString(R.string.account_offline));
			builder.setMessage(getString(R.string.cant_invite_while_offline));
			builder.setNegativeButton(getString(R.string.ok), null);
			builder.setIconAttribute(android.R.attr.alertDialogIcon);
			builder.create().show();
			return false;
		}
	}

	private void invite() {
		List<Conversation> conversations = xmppConnectionService
				.getConversations();
		Conversation conversation = null;
		for (Conversation tmpConversation : conversations) {
			if (tmpConversation.getUuid().equals(
					getIntent().getStringExtra("uuid"))) {
				conversation = tmpConversation;
				break;
			}
			// else if (!tmpConversation.getUuid().equals(
			// getIntent().getStringExtra("uuid"))
			// && ConversationActivity.INVITE_STRING
			// .equalsIgnoreCase("invite")) {
			// conversation = tmpConversation;
			// }

		}
		if (conversation != null) {
			xmppConnectionService.inviteToConference(conversation,
					selectedContacts);
		}
		finish();
	}

	public void startConference(View v) {
		if (accounts.size() > 1) {
			getAccountChooser(new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					startConference(accounts.get(which));
				}
			}).show();
		} else {
			startConference(accounts.get(0));
		}

	}

	private void startConference(final Account account) {
		if (isOnline(account)) {
			final Dialog d = new Dialog(ContactsActivity.this);
			d.setContentView(R.layout.dialog_new_group);
			d.setTitle("Create Group");

			final EditText name = (EditText) d
					.findViewById(R.id.edittext_group_name);
			Button create = (Button) d.findViewById(R.id.button_ok);
			Button cancel = (Button) d.findViewById(R.id.button_cancel);

			cancel.setOnClickListener(new android.view.View.OnClickListener() {

				@Override
				public void onClick(View v) {
					d.dismiss();
				}
			});

			create.setOnClickListener(new android.view.View.OnClickListener() {
				@Override
				public void onClick(View v) {
					// nameOfGroup=

					// String mucName = CryptoHelper
					// .randomMucName(xmppConnectionService
					// .getRNG());
					if (name.getText().toString().length() > 0) {
						String mucName = name.getText().toString();
						String serverName = account.getXmppConnection()
								.getMucServer();
						if (serverName == null) {
							List<String> servers = getMucServers();
							if (servers.size() >= 1) {
								serverName = servers.get(0);
							} else {
								displayErrorDialog(R.string.no_muc_server_found);
								return;
							}
						}
						String jid = mucName + "@" + serverName;
						Conversation conversation = xmppConnectionService
								.findOrCreateConversation(account, jid, true);
						StringBuilder subject = new StringBuilder();
						subject.append(account.getUsername() + ", ");
						for (int i = 0; i < selectedContacts.size(); ++i) {
							if (i + 1 != selectedContacts.size()) {
								subject.append(selectedContacts.get(i)
										.getDisplayName() + ", ");
							} else {
								subject.append(selectedContacts.get(i)
										.getDisplayName());
							}
						}
						// xmppConnectionService.sendConversationSubject(
						// conversation, subject.toString());
						xmppConnectionService.sendConversationSubject(
								conversation, mucName);
						xmppConnectionService.inviteToConference(conversation,
								selectedContacts);
						switchToConversation(conversation, null, false);

					} else {
						Toast.makeText(ContactsActivity.this,
								"Please enter valid name", Toast.LENGTH_SHORT)
								.show();
						return;
					}

				}
			});

			// AlertDialog.Builder builder = new AlertDialog.Builder(this);
			// builder.setTitle(getString(R.string.new_conference));
			// builder.setMessage(getString(R.string.new_conference_explained));
			// builder.setNegativeButton(getString(R.string.cancel), null);
			// builder.setPositiveButton(getString(R.string.create_invite),
			// new OnClickListener() {
			//
			// @Override
			// public void onClick(DialogInterface dialog, int which) {
			// String mucName = CryptoHelper
			// .randomMucName(xmppConnectionService
			// .getRNG());
			// String serverName = account.getXmppConnection()
			// .getMucServer();
			// if (serverName == null) {
			// List<String> servers = getMucServers();
			// if (servers.size() >= 1) {
			// serverName = servers.get(0);
			// } else {
			// displayErrorDialog(R.string.no_muc_server_found);
			// return;
			// }
			// }
			// String jid = mucName + "@" + serverName;
			// Conversation conversation = xmppConnectionService
			// .findOrCreateConversation(account, jid,
			// true);
			// StringBuilder subject = new StringBuilder();
			// subject.append(account.getUsername() + ", ");
			// for (int i = 0; i < selectedContacts.size(); ++i) {
			// if (i + 1 != selectedContacts.size()) {
			// subject.append(selectedContacts.get(i)
			// .getDisplayName() + ", ");
			// } else {
			// subject.append(selectedContacts.get(i)
			// .getDisplayName());
			// }
			// }
			// // xmppConnectionService.sendConversationSubject(
			// // conversation, subject.toString());
			// xmppConnectionService.sendConversationSubject(
			// conversation, mucName);
			// xmppConnectionService.inviteToConference(
			// conversation, selectedContacts);
			// switchToConversation(conversation, null, false);
			// }
			// });
			// builder.create().show();
			d.show();
		}
	}

	private String randomStringGen() {
		// class variable
		final String lexicon = "ABCDEFGHIJKLMNOPQRSTUVWXYZ12345674890";

		final java.util.Random rand = new java.util.Random();

		// consider using a Map<String,Boolean> to say whether the identifier is
		// being used or not
		final Set<String> identifiers = new HashSet<String>();

		StringBuilder builder = new StringBuilder();
		while (builder.toString().length() == 0) {
			int length = rand.nextInt(5) + 5;
			for (int i = 0; i < length; i++)
				builder.append(lexicon.charAt(rand.nextInt(lexicon.length())));
			if (identifiers.contains(builder.toString())) {
				builder = new StringBuilder();
			}

		}
		return builder.toString();

	}

	/**
	 * Will create a group from Service
	 * */
	public void startConferenceFromService(final Account account) {
		Conversation conversation;
		String mucName = randomStringGen();
		String serverName = account.getXmppConnection().getMucServer();
		 if (serverName == null) {
		 List<String> servers = getMucServers();
		 if (servers.size() >= 1) {
		 serverName = servers.get(0);
		 } else {
		 //displayErrorDialog(R.string.no_muc_server_found);
		 return;
		 }
		 }

		if (serverName == null) {
			Log.i("hemant", "severname is null");
		} else {
			Log.i("hemant", "severname is not null");
		}
		String jid = mucName + "@" + serverName;
		setMucName(jid);

		Log.i("ContactAct", "Account" + account + " Jid" + jid);
		if (xmppConnectionService != null) {
			conversation = ContactsActivity.this.xmppConnectionService
					.createNewConversation(account, jid, true);
		} else {
			xmppConnectionService = new XmppConnectionService();
			conversation = Utils.tempxmppConnectionService
					.createNewConversation(account, jid, true);
		}
		StringBuilder subject = new StringBuilder();
		subject.append(account.getUsername() + ", ");
		for (int i = 0; i < selectedContacts.size(); ++i) {
			if (i + 1 != selectedContacts.size()) {
				subject.append(selectedContacts.get(i).getDisplayName() + ", ");
			} else {
				subject.append(selectedContacts.get(i).getDisplayName());
			}
		}
		xmppConnectionService.sendConversationSubject(conversation,
				subject.toString());

	}

	/**
	 * Creates the Conversation
	 * */
	public Conversation createConversationGroup(Account account, String jid) {
		Conversation con;
		con = xmppConnectionService
				.findOrCreateConversation(account, jid, true);
		return con;
	}

	/**
	 * Sends invitation to the selected Contact
	 * 
	 * */
	public void inviteToGroupFromService(Account account, Contact contacts , String SENDERJID
			) {
		Utils.tempxmppConnectionService.inviteToConferenceWithAccount(account, contacts,
				SENDERJID);

		// List<Contact> contactselected = new ArrayList<Contact>();
		// contactselected.add(contact);
		// xmppConnectionService.inviteToConference(conversation,
		// contactselected);
	}

	protected void updateAggregatedContacts() {

		aggregatedContacts.clear();
		for (Contact contact : rosterContacts) {
			if (contact.match(searchString) && (contact.showInRoster()))
				aggregatedContacts.add(contact);
		}

		Collections.sort(aggregatedContacts, new Comparator<Contact>() {

			@SuppressLint("DefaultLocale")
			@Override
			public int compare(Contact lhs, Contact rhs) {
				return lhs.getDisplayName().toLowerCase()
						.compareTo(rhs.getDisplayName().toLowerCase());
			}
		});

		if (aggregatedContacts.size() == 0) {

			if (Validator.isValidJid(searchString)) {
				Contact newContact = new Contact(searchString);
				newContact.resetOption(Contact.Options.IN_ROSTER);
				aggregatedContacts.add(newContact);
				contactsHeader.setText(getString(R.string.new_contact));
			} else {
				contactsHeader.setText(getString(R.string.contacts));
			}
		} else {
			contactsHeader.setText(getString(R.string.contacts));
		}

		contactsAdapter.notifyDataSetChanged();
		contactsView.setScrollX(0);
	}

	private OnItemLongClickListener onLongClickListener = new OnItemLongClickListener() {

		@Override
		public boolean onItemLongClick(AdapterView<?> arg0, View view,
				int position, long arg3) {
			if (!isActionMode) {
				contactsView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
				contactsView.setItemChecked(position, true);
				actionMode = contactsView.startActionMode(actionModeCallback);
			}
			return true;
		}
	};

	@Override
	protected void onStart() {
		super.onStart();
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(activity);
		this.useSubject = preferences.getBoolean("use_subject_in_muc", true);
		// Log.i("hemant", "invite intent recieved" + getIntent().getAction());
		String inviteUser = "";
		// if (getIntent().getExtras() != null)
		// inviteUser = getIntent().getExtras().getString("Inviteuser");
		//
		inviteIntent = "invite".equals(getIntent().getAction());
		if (inviteIntent) {
			contactsHeader.setVisibility(View.GONE);
			actionMode = contactsView.startActionMode(actionModeCallback);
			createGroupButton.setVisibility(View.GONE);
			search.setVisibility(View.GONE);
			return;
		}

		// if (!inviteIntent
		// && ConversationActivity.INVITE_STRING
		// .equalsIgnoreCase("invite")) {
		// contactsHeader.setVisibility(View.GONE);
		// actionMode = contactsView.startActionMode(actionModeCallback);
		// createGroupButton.setVisibility(View.GONE);
		// search.setVisibility(View.GONE);
		// //ConversationActivity.INVITE_STRING = "";
		// }
		// else if (inviteUser.equals("invite")) {
		// contactsHeader.setVisibility(View.GONE);
		// actionMode = contactsView.startActionMode(actionModeCallback);
		// createGroupButton.setVisibility(View.GONE);
		// search.setVisibility(View.GONE);
		// }
	}

	// @Override
	// protected void onNewIntent(Intent intent) {
	// // TODO Auto-generated method stub
	// super.onNewIntent(intent);
	// Log.i("hemant", "invite on new intent recieved"
	// + getIntent().getAction());
	// inviteIntent = "invite".equals(getIntent().getAction());
	// if (inviteIntent) {
	// contactsHeader.setVisibility(View.GONE);
	// actionMode = contactsView.startActionMode(actionModeCallback);
	// createGroupButton.setVisibility(View.GONE);
	// search.setVisibility(View.GONE);
	// }
	// }

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_new_conversation);

		contactsHeader = (TextView) findViewById(R.id.contacts_header);
		createGroupButton = (Button) findViewById(R.id.button_conversation);
		search = (EditText) findViewById(R.id.new_conversation_search);
		search.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				searchString = search.getText().toString();
				updateAggregatedContacts();
			}

			@Override
			public void afterTextChanged(Editable s) {
				// TODO Auto-generated method stub

			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub

			}
		});

		contactsView = (ListView) findViewById(R.id.contactList);
		contactsAdapter = new ArrayAdapter<Contact>(getApplicationContext(),
				R.layout.contact, aggregatedContacts) {
			@Override
			public View getView(int position, View view, ViewGroup parent) {
				LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				Contact contact = getItem(position);
				if (view == null) {
					view = (View) inflater.inflate(R.layout.contact, null);
				}

				((TextView) view.findViewById(R.id.contact_display_name))
						.setText(getItem(position).getDisplayName());
				TextView contactJid = (TextView) view
						.findViewById(R.id.contact_jid);
				contactJid.setText(contact.getJid());
				ImageView imageView = (ImageView) view
						.findViewById(R.id.contact_photo);
				imageView.setImageBitmap(UIHelper.getContactPicture(contact,
						48, this.getContext(), false));
				return view;
			}
		};
		contactsView.setAdapter(contactsAdapter);
		contactsView.setMultiChoiceModeListener(actionModeCallback);
		contactsView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, final View view,
					int pos, long arg3) {
				if (!isActionMode) {
					Contact clickedContact = aggregatedContacts.get(pos);
					startConversation(clickedContact);
					// startConference();
				} else {
					actionMode.invalidate();
				}
			}
		});
		contactsView.setOnItemLongClickListener(this.onLongClickListener);
	}

	public void startConversation(final Contact contact) {
		if ((contact.getAccount() == null) && (accounts.size() > 1)) {
			getAccountChooser(new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					contact.setAccount(accounts.get(which));
					showIsMucDialogIfNeeded(contact);
				}
			}).show();
		} else {
			if (contact.getAccount() == null) {
				contact.setAccount(accounts.get(0));
			}
			showIsMucDialogIfNeeded(contact);
		}
	}

	protected AlertDialog getAccountChooser(OnClickListener listener) {
		String[] accountList = new String[accounts.size()];
		for (int i = 0; i < accounts.size(); ++i) {
			accountList[i] = accounts.get(i).getJid();
		}

		AlertDialog.Builder accountChooser = new AlertDialog.Builder(this);
		accountChooser.setTitle(getString(R.string.choose_account));
		accountChooser.setItems(accountList, listener);
		return accountChooser.create();
	}

	public void showIsMucDialogIfNeeded(final Contact clickedContact) {
		if (isMuc(clickedContact)) {
			startConversation(clickedContact, clickedContact.getAccount(), true);
		} else if (clickedContact.couldBeMuc()) {
			AlertDialog.Builder dialog = new AlertDialog.Builder(this);
			dialog.setTitle(getString(R.string.multi_user_conference));
			dialog.setMessage(getString(R.string.trying_join_conference));
			dialog.setPositiveButton(getString(R.string.yes),
					new OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							startConversation(clickedContact,
									clickedContact.getAccount(), true);
						}
					});
			dialog.setNegativeButton(getString(R.string.no),
					new OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							startConversation(clickedContact,
									clickedContact.getAccount(), false);
						}
					});
			dialog.create().show();
		} else {
			startConversation(clickedContact, clickedContact.getAccount(),
					false);
		}
	}

	private List<String> getMucServers() {
		ArrayList<String> mucServers = new ArrayList<String>();
		if (accounts == null) {
			List<Account> tempAccountList = new ArrayList<Account>();

			tempAccountList = Utils.tempxmppConnectionService.getAccounts();

			Account tempaccount = tempAccountList.get(0);
			String server = tempaccount.getXmppConnection().getMucServer();
			if (server != null) {
				mucServers.add(server);
			}
		} else {
			for (Account account : accounts) {
				if (account.getXmppConnection() != null) {
					String server = account.getXmppConnection().getMucServer();
					if (server != null) {
						mucServers.add(server);
					}
				}
			}
		}

		return mucServers;
	}

	private boolean isMuc(Contact contact) {
		String server = contact.getJid().split("@")[1];
		return getMucServers().contains(server);
	}

	public void startConversation(Contact contact, Account account, boolean muc) {
		if (!contact.getOption(Contact.Options.IN_ROSTER) && (!muc)) {
			xmppConnectionService.createContact(contact);
		}
		Conversation conversation = xmppConnectionService
				.findOrCreateConversation(account, contact.getJid(), muc);

		switchToConversation(conversation, null, false);
	}

	@Override
	void onBackendConnected() {
		this.accounts = xmppConnectionService.getAccounts();
		if (Intent.ACTION_SENDTO.equals(getIntent().getAction())) {
			getActionBar().setDisplayHomeAsUpEnabled(false);
			getActionBar().setHomeButtonEnabled(false);
			String jid;
			try {
				jid = URLDecoder.decode(getIntent().getData().getEncodedPath(),
						"UTF-8").split("/")[1];
			} catch (UnsupportedEncodingException e) {
				jid = null;
			}
			if (jid != null) {
				final String finalJid = jid;
				if (this.accounts.size() > 1) {
					getAccountChooser(new OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							Conversation conversation = xmppConnectionService
									.findOrCreateConversation(
											accounts.get(which), finalJid,
											false);
							switchToConversation(conversation, null, false);
							finish();
						}
					}).show();
				} else {
					Conversation conversation = xmppConnectionService
							.findOrCreateConversation(this.accounts.get(0),
									jid, false);
					switchToConversation(conversation, null, false);
					finish();
				}
			}
		}

		if (xmppConnectionService.getConversationCount() == 0) {
			getActionBar().setDisplayHomeAsUpEnabled(false);
			getActionBar().setHomeButtonEnabled(false);
		}
		this.rosterContacts.clear();
		for (Account account : accounts) {
			if (account.getStatus() != Account.STATUS_DISABLED) {
				rosterContacts.addAll(account.getRoster().getContacts());
			}
		}
		updateAggregatedContacts();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.newconversation, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onActionModeStarted(ActionMode mode) {
		super.onActionModeStarted(mode);
		this.isActionMode = true;
		search.setEnabled(false);
	}

	@Override
	public void onActionModeFinished(ActionMode mode) {
		super.onActionModeFinished(mode);
		if (inviteIntent) {
			finish();
		} else {
			this.isActionMode = false;
			contactsView.clearChoices();
			contactsView.requestLayout();
			contactsView.post(new Runnable() {
				@Override
				public void run() {
					contactsView.setChoiceMode(ListView.CHOICE_MODE_NONE);
				}
			});
			search.setEnabled(true);
		}
	}

	// private Dialog customDialog() {
	// Dialog d = new Dialog(ContactsActivity.this);
	// d.setContentView(R.layout.dialog_new_group);
	// EditText name = (EditText) d.findViewById(R.id.edittext_group_name);
	// Button create = (Button) d.findViewById(R.id.button_ok);
	// Button cancel = (Button) d.findViewById(R.id.button_cancel);
	//
	// cancel.setOnClickListener(new android.view.View.OnClickListener() {
	//
	// @Override
	// public void onClick(View v) {
	//
	// }
	// });
	//
	//
	// create.setOnClickListener(new android.view.View.OnClickListener()){
	// @Override
	// public void onClick(View v) {
	// name.getText().toString();
	// }
	// });
	// return d;
	// }
}
