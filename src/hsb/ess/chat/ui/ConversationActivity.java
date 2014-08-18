package hsb.ess.chat.ui;

import hsb.ess.chat.R;
import hsb.ess.chat.entities.Conversation;
import hsb.ess.chat.entities.Message;
import hsb.ess.chat.services.ImageProvider;
import hsb.ess.chat.services.XmppConnectionService;
import hsb.ess.chat.sync.LockScreenActivity;
import hsb.ess.chat.utils.UIHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.filetransfer.FileTransfer.Status;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.filetransfer.OutgoingFileTransfer;

import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.widget.SlidingPaneLayout;
import android.support.v4.widget.SlidingPaneLayout.PanelSlideListener;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TextView;
import android.widget.Toast;

public class ConversationActivity extends XmppActivity {

	public static final String VIEW_CONVERSATION = "viewConversation";
	public static final String CONVERSATION = "conversationUuid";
	public static final String TEXT = "text";
	public static final String PRESENCE = "eu.siacs.conversations.presence";

	// public static String INVITE_STRING="";

	public static final int REQUEST_SEND_MESSAGE = 0x75441;
	public static final int REQUEST_DECRYPT_PGP = 0x76783;
	private static final int REQUEST_ATTACH_FILE_DIALOG = 0x48502;
	private static final int REQUEST_IMAGE_CAPTURE = 0x33788;
	private static final int REQUEST_RECORD_AUDIO = 0x46189;
	private static final int REQUEST_SEND_PGP_IMAGE = 0x53883;
	public static final int REQUEST_ENCRYPT_MESSAGE = 0x378018;

	private static final int ATTACHMENT_CHOICE_CHOOSE_IMAGE = 0x92734;
	private static final int ATTACHMENT_CHOICE_TAKE_PHOTO = 0x84123;
	private static final int ATTACHMENT_CHOICE_RECORD_VOICE = 0x75291;

	// variable for audio transfer
	// private static final int ATTACHMENT_CHOICE_AUDIO = 777787;

	private static ConversationActivity instance = null;
	protected SlidingPaneLayout spl;

	private List<Conversation> conversationList = new ArrayList<Conversation>();
	private Conversation selectedConversation = null;
	private ListView listView;

	private boolean paneShouldBeOpen = true;
	private boolean useSubject = true;
	private boolean showLastseen = false;
	private ArrayAdapter<Conversation> listAdapter;

	public static ConversationActivity getInstance() {
		return instance;
	}

	private OnConversationListChangedListener onConvChanged = new OnConversationListChangedListener() {

		@Override
		public void onConversationListChanged() {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					updateConversationList();
					if (paneShouldBeOpen) {
						if (conversationList.size() >= 1) {
							swapConversationFragment();
						} else {
							startActivity(new Intent(getApplicationContext(),
									ContactsActivity.class));
							finish();
						}
					}
					ConversationFragment selectedFragment = (ConversationFragment) getFragmentManager()
							.findFragmentByTag("conversation");
					if (selectedFragment != null) {
						selectedFragment.updateMessages();
					}
				}
			});
		}
	};

	protected ConversationActivity activity = this;
	private DisplayMetrics metrics;
	private Toast prepareImageToast;

	public List<Conversation> getConversationList() {
		return this.conversationList;
	}

	public Conversation getSelectedConversation() {
		return this.selectedConversation;
	}

	public void setSelectedConversation(Conversation conversation) {
		this.selectedConversation = conversation;
	}

	public ListView getConversationListView() {
		return this.listView;
	}

	public SlidingPaneLayout getSlidingPaneLayout() {
		return this.spl;
	}

	public boolean shouldPaneBeOpen() {
		return paneShouldBeOpen;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		metrics = getResources().getDisplayMetrics();

		super.onCreate(savedInstanceState);
		this.instance = this;
		setContentView(R.layout.fragment_conversations_overview);

		if (LockScreenActivity.getInstance() != null) {
			Intent i = new Intent(this, LockScreenActivity.class);
			startActivity(i);

		}

		listView = (ListView) findViewById(R.id.list);

		this.listAdapter = new ArrayAdapter<Conversation>(this,
				R.layout.conversation_list_row, conversationList) {
			@Override
			public View getView(int position, View view, ViewGroup parent) {
				if (view == null) {
					LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					view = (View) inflater.inflate(
							R.layout.conversation_list_row, null);
				}
				Conversation conv;
				if (conversationList.size() > position) {
					conv = getItem(position);
				} else {
					return view;
				}
				if (!spl.isSlideable()) {
					if (conv == getSelectedConversation()) {
						view.setBackgroundColor(0xffdddddd);
					} else {
						view.setBackgroundColor(Color.TRANSPARENT);
					}
				} else {
					view.setBackgroundColor(Color.TRANSPARENT);
				}
				TextView convName = (TextView) view
						.findViewById(R.id.conversation_name);
				convName.setText(conv.getName(useSubject));
				TextView convLastMsg = (TextView) view
						.findViewById(R.id.conversation_lastmsg);
				ImageView imagePreview = (ImageView) view
						.findViewById(R.id.conversation_lastimage);

				Message latestMessage = conv.getLatestMessage();
				// Log.i("hemant", "Latest message" + latestMessage);

				if (latestMessage.getType() == Message.TYPE_TEXT) {
					if ((latestMessage.getEncryption() != Message.ENCRYPTION_PGP)
							&& (latestMessage.getEncryption() != Message.ENCRYPTION_DECRYPTION_FAILED)) {
						convLastMsg.setText(conv.getLatestMessage().getBody());
						Log.i("hemant", "Message"
								+ conv.getLatestMessage().getBody());
					} else {
						convLastMsg
								.setText(getText(R.string.encrypted_message_received));
					}
					convLastMsg.setVisibility(View.VISIBLE);
					imagePreview.setVisibility(View.GONE);
				} else if (latestMessage.getType() == Message.TYPE_IMAGE) {
					if (latestMessage.getStatus() >= Message.STATUS_RECIEVED) {
						convLastMsg.setVisibility(View.GONE);
						imagePreview.setVisibility(View.VISIBLE);
						// loadBitmap(latestMessage, imagePreview);
						loadAudio(latestMessage);
						Log.i(Utils.LOG_IMAGE, "Load Message , getView");
					} else {
						convLastMsg.setVisibility(View.VISIBLE);
						imagePreview.setVisibility(View.GONE);
						if (latestMessage.getStatus() == Message.STATUS_RECEIVED_OFFER) {
							convLastMsg
									.setText(getText(R.string.image_offered_for_download));
						} else if (latestMessage.getStatus() == Message.STATUS_RECIEVING) {
							convLastMsg
									.setText(getText(R.string.receiving_image));
						} else {
							convLastMsg.setText("");
						}
					}
				} else if (latestMessage.getType() == Message.TYPE_AUDIO) {
					if (latestMessage.getStatus() >= Message.STATUS_RECIEVED) {
						convLastMsg.setVisibility(View.GONE);
						imagePreview.setVisibility(View.VISIBLE);
						// loadBitmap(latestMessage, imagePreview);
						loadAudio(latestMessage);
					} else {
						convLastMsg.setVisibility(View.VISIBLE);
						imagePreview.setVisibility(View.GONE);
						if (latestMessage.getStatus() == Message.STATUS_RECEIVED_OFFER) {
							convLastMsg
									.setText(getText(R.string.image_offered_for_download));
						} else if (latestMessage.getStatus() == Message.STATUS_RECIEVING) {
							convLastMsg
									.setText(getText(R.string.receiving_image));
						} else {
							convLastMsg.setText("");
						}
					}
				}

				if (!conv.isRead()) {
					convName.setTypeface(null, Typeface.BOLD);
					convLastMsg.setTypeface(null, Typeface.BOLD);
				} else {
					convName.setTypeface(null, Typeface.NORMAL);
					convLastMsg.setTypeface(null, Typeface.NORMAL);
				}

				((TextView) view.findViewById(R.id.conversation_lastupdate))
						.setText(UIHelper.readableTimeDifference(getContext(),
								conv.getLatestMessage().getTimeSent()));

				ImageView profilePicture = (ImageView) view
						.findViewById(R.id.conversation_image);
				profilePicture.setImageBitmap(UIHelper.getContactPicture(conv,
						56, activity.getApplicationContext(), false));

				return view;
			}

		};

		listView.setAdapter(this.listAdapter);

		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View clickedView,
					int position, long arg3) {
				paneShouldBeOpen = false;
				if (getSelectedConversation() != conversationList.get(position)) {
					setSelectedConversation(conversationList.get(position));
					swapConversationFragment(); // .onBackendConnected(conversationList.get(position));
				} else {
					spl.closePane();
				}
			}
		});
		spl = (SlidingPaneLayout) findViewById(R.id.slidingpanelayout);
		spl.setParallaxDistance(150);
		spl.setShadowResource(R.drawable.es_slidingpane_shadow);
		spl.setSliderFadeColor(0);
		spl.setPanelSlideListener(new PanelSlideListener() {

			@Override
			public void onPanelOpened(View arg0) {
				paneShouldBeOpen = true;
				getActionBar().setDisplayHomeAsUpEnabled(false);
				getActionBar().setHomeButtonEnabled(false);
				getActionBar().setTitle(R.string.app_name);
				invalidateOptionsMenu();
				hideKeyboard();
			}

			@Override
			public void onPanelClosed(View arg0) {
				paneShouldBeOpen = false;
				if ((conversationList.size() > 0)
						&& (getSelectedConversation() != null)) {
					getActionBar().setDisplayHomeAsUpEnabled(true);
					getActionBar().setHomeButtonEnabled(true);
					getActionBar().setTitle(
							getSelectedConversation().getName(useSubject));
					invalidateOptionsMenu();
					if (!getSelectedConversation().isRead()) {
						xmppConnectionService
								.markRead(getSelectedConversation());
						Log.i("notifiChat", "update notification");
						UIHelper.updateNotification(getApplicationContext(),
								getConversationList(), null, false);
						listView.invalidateViews();
					}
				}
			}

			@Override
			public void onPanelSlide(View arg0, float arg1) {
				// TODO Auto-generated method stub

			}
		});
		// Utils.tempxmppConnectionService = xmppConnectionService;
		// Log.i(Utils.LOG_IMAGE, "temp xmpp connection service oncreate "
		// +Utils.tempxmppConnectionService);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.conversations, menu);
		// MenuItem menuSecure = (MenuItem) menu.findItem(R.id.action_security);
		MenuItem menuArchive = (MenuItem) menu.findItem(R.id.action_archive);
		MenuItem menuMucDetails = (MenuItem) menu
				.findItem(R.id.action_muc_details);
		MenuItem menuContactDetails = (MenuItem) menu
				.findItem(R.id.action_contact_details);
		MenuItem menuInviteContacts = (MenuItem) menu
				.findItem(R.id.action_invite);
		MenuItem menuAttach = (MenuItem) menu.findItem(R.id.action_attach_file);
		MenuItem menuClearHistory = (MenuItem) menu
				.findItem(R.id.action_clear_history);

		if ((spl.isOpen() && (spl.isSlideable()))) {
			menuArchive.setVisible(false);
			menuMucDetails.setVisible(false);
			menuContactDetails.setVisible(false);
			// menuSecure.setVisible(false);
			menuInviteContacts.setVisible(false);
			menuAttach.setVisible(false);
			menuClearHistory.setVisible(false);
		} else {
			((MenuItem) menu.findItem(R.id.action_add)).setVisible(!spl
					.isSlideable());
			if (this.getSelectedConversation() != null) {
				if (this.getSelectedConversation().getLatestMessage()
						.getEncryption() != Message.ENCRYPTION_NONE) {
					// menuSecure.setIcon(R.drawable.ic_action_secure);
				}
				if (this.getSelectedConversation().getMode() == Conversation.MODE_MULTI) {
					menuContactDetails.setVisible(false);
					menuAttach.setVisible(false);
				} else {
					menuMucDetails.setVisible(false);
					menuInviteContacts.setVisible(false);
				}
			}
		}
		return true;
	}

	private void selectPresenceToAttachFile(final int attachmentChoice) {
		selectPresence(getSelectedConversation(), new OnPresenceSelected() {

			@Override
			public void onPresenceSelected() {
				if (attachmentChoice == ATTACHMENT_CHOICE_TAKE_PHOTO) {
					Intent takePictureIntent = new Intent(
							MediaStore.ACTION_IMAGE_CAPTURE);
					takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
							ImageProvider.getIncomingContentUri());
					if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
						startActivityForResult(takePictureIntent,
								REQUEST_IMAGE_CAPTURE);
					}
				} else if (attachmentChoice == ATTACHMENT_CHOICE_CHOOSE_IMAGE) {
					Intent attachFileIntent = new Intent();
					attachFileIntent.setType("image/*");
					attachFileIntent.setAction(Intent.ACTION_GET_CONTENT);
					Intent chooser = Intent.createChooser(attachFileIntent,
							getString(R.string.attach_file));
					startActivityForResult(chooser, REQUEST_ATTACH_FILE_DIALOG);
				} else if (attachmentChoice == ATTACHMENT_CHOICE_RECORD_VOICE) {
					Intent intent = new Intent(
							MediaStore.Audio.Media.RECORD_SOUND_ACTION);
					startActivityForResult(intent, REQUEST_RECORD_AUDIO);
				}
			}
		});
	}

	private void attachFile(final int attachmentChoice) {
		final Conversation conversation = getSelectedConversation();
		// if (conversation.getNextEncryption() == Message.ENCRYPTION_PGP) {
		// if (hasPgp()) {
		// if (conversation.getContact().getPgpKeyId() != 0) {
		// xmppConnectionService.getPgpEngine().hasKey(
		// conversation.getContact(),
		// new UiCallback<Contact>() {
		//
		// @Override
		// public void userInputRequried(PendingIntent pi,
		// Contact contact) {
		// ConversationActivity.this.runIntent(pi,
		// attachmentChoice);
		// }
		//
		// @Override
		// public void success(Contact contact) {
		// selectPresenceToAttachFile(attachmentChoice);
		// }
		//
		// @Override
		// public void error(int error, Contact contact) {
		// displayErrorDialog(error);
		// }
		// });
		// } else {
		// final ConversationFragment fragment = (ConversationFragment)
		// getFragmentManager()
		// .findFragmentByTag("conversation");
		// if (fragment != null) {
		// fragment.showNoPGPKeyDialog(false,
		// new OnClickListener() {
		//
		// @Override
		// public void onClick(DialogInterface dialog,
		// int which) {
		// conversation
		// .setNextEncryption(Message.ENCRYPTION_NONE);
		// selectPresenceToAttachFile(attachmentChoice);
		// }
		// });
		// }
		// }
		// } else {
		// showInstallPgpDialog();
		// }
		// } else
		if (getSelectedConversation().getNextEncryption() == Message.ENCRYPTION_NONE) {
			selectPresenceToAttachFile(attachmentChoice);
		} else {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(getString(R.string.otr_file_transfer));
			builder.setMessage(getString(R.string.otr_file_transfer_msg));
			builder.setNegativeButton(getString(R.string.cancel), null);
			if (conversation.getContact().getPgpKeyId() == 0) {
				builder.setPositiveButton(getString(R.string.send_unencrypted),
						new OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								conversation
										.setNextEncryption(Message.ENCRYPTION_NONE);
								attachFile(attachmentChoice);
							}
						});
			} else {
				builder.setPositiveButton(
						getString(R.string.use_pgp_encryption),
						new OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								conversation
										.setNextEncryption(Message.ENCRYPTION_PGP);
								attachFile(attachmentChoice);
							}
						});
			}
			builder.create().show();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			spl.openPane();
			break;
		case R.id.action_attach_file:
			Toast.makeText(ConversationActivity.this, "Attach files ",
					Toast.LENGTH_SHORT).show();

			View menuAttachFile = findViewById(R.id.action_attach_file);
			PopupMenu attachFilePopup = new PopupMenu(this, menuAttachFile);
			attachFilePopup.inflate(R.menu.attachment_choices);
			attachFilePopup
					.setOnMenuItemClickListener(new OnMenuItemClickListener() {

						@Override
						public boolean onMenuItemClick(MenuItem item) {
							switch (item.getItemId()) {
							case R.id.attach_choose_picture:
								attachFile(ATTACHMENT_CHOICE_CHOOSE_IMAGE);
								break;
							case R.id.attach_take_picture:
								attachFile(ATTACHMENT_CHOICE_TAKE_PHOTO);
								break;
							case R.id.attach_record_voice:
								attachFile(ATTACHMENT_CHOICE_RECORD_VOICE);
								break;

							case R.id.attach_record_audio:

								// ArrayList<String> sdcardfiles =
								// GetFiles("/sdcard");
								// FileTransferManager = new
								String pathSong = "/sdcard/last.mp3";
								Uri uri = Uri.parse(pathSong);

								// Connection connection;
								// Connection connection =
								// getSelectedConversation().getAccount().getXmppConnection();

								// FileTransferManager manager = new
								// FileTransferManager(getSelectedConversation().getAccount().getXmppConnection().);
								// OutgoingFileTransfer transfer =
								// manager.createOutgoingFileTransfer(getSelectedConversation().getContactJid());
								// File file = new File(pathSong);
								// try {
								// transfer.sendFile(file, "test_file");
								// } catch (XMPPException e) {
								// e.printStackTrace();
								// }
								// while(!transfer.isDone()) {
								// if(transfer.getStatus().equals(Status.error))
								// {
								// System.out.println("ERROR!!! " +
								// transfer.getError());
								// } else if
								// (transfer.getStatus().equals(Status.cancelled)
								// ||
								// transfer.getStatus().equals(Status.refused))
								// {
								// System.out.println("Cancelled!!! " +
								// transfer.getError());
								// }
								// try {
								// Thread.sleep(1000L);
								// } catch (InterruptedException e) {
								// e.printStackTrace();
								// }
								// }
								// if(transfer.getStatus().equals(Status.refused)
								// || transfer.getStatus().equals(Status.error)
								// ||
								// transfer.getStatus().equals(Status.cancelled)){
								// System.out.println("refused cancelled error "
								// + transfer.getError());
								// } else {
								// System.out.println("Success");
								// }
								attachAudioToConversation(
										getSelectedConversation(), uri , ConversationActivity.getInstance().xmppConnectionService);
								// attachImageToConversation(
								// getSelectedConversation(), uri);
								// attachFile(ATTACHMENT_CHOICE_AUDIO);
								break;
							}
							return false;
						}
					});
			attachFilePopup.show();
			break;
		case R.id.action_add:
			startActivity(new Intent(this, ContactsActivity.class));
			// Toast.makeText(ConversationActivity.this, "ADD ",
			// Toast.LENGTH_SHORT).show();
			break;
		case R.id.action_archive:
			this.endConversation(getSelectedConversation());
			break;
		case R.id.action_contact_details:
			Toast.makeText(ConversationActivity.this, "Contact Details ",
					Toast.LENGTH_SHORT).show();
			// Contact contact = this.getSelectedConversation().getContact();
			// if (contact.showInRoster()) {
			// Intent intent = new Intent(this, ContactDetailsActivity.class);
			// intent.setAction(ContactDetailsActivity.ACTION_VIEW_CONTACT);
			// intent.putExtra("account", this.getSelectedConversation()
			// .getAccount().getJid());
			// intent.putExtra("contact", contact.getJid());
			// startActivity(intent);
			// } else {
			// showAddToRosterDialog(getSelectedConversation());
			// }
			break;

		case R.id.action_muc_details:
			Toast.makeText(ConversationActivity.this, "Details ",
					Toast.LENGTH_SHORT).show();
			// Intent intent = new Intent(this, MucDetailsActivity.class);
			// intent.setAction(MucDetailsActivity.ACTION_VIEW_MUC);
			// intent.putExtra("uuid", getSelectedConversation().getUuid());
			// startActivity(intent);
			break;
		case R.id.action_invite:

			// Toast.makeText(ConversationActivity.this, "Invite ",
			// Toast.LENGTH_SHORT).show();
			Intent inviteIntent = new Intent(getApplicationContext(),
					ContactsActivity.class);
			inviteIntent.setAction("invite");
			// INVITE_STRING ="invite";
			inviteIntent.putExtra("uuid", getSelectedConversation().getUuid());
			// inviteIntent.putExtra("Inviteuser", "invite");
			// inviteIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP |
			// Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(inviteIntent);
			Log.i("hemant", "action Invite action  " + inviteIntent.getAction()
					+ "uuid " + getSelectedConversation().getUuid());
			break;
		// case R.id.action_security:
		// final Conversation conversation = getSelectedConversation();
		// View menuItemView = findViewById(R.id.action_security);
		// PopupMenu popup = new PopupMenu(this, menuItemView);
		// final ConversationFragment fragment = (ConversationFragment)
		// getFragmentManager()
		// .findFragmentByTag("conversation");
		// // if (fragment != null) {
		// // popup.setOnMenuItemClickListener(new OnMenuItemClickListener() {
		// //
		// // @Override
		// // public boolean onMenuItemClick(MenuItem item) {
		// // switch (item.getItemId()) {
		// // case R.id.encryption_choice_none:
		// // conversation
		// // .setNextEncryption(Message.ENCRYPTION_NONE);
		// // item.setChecked(true);
		// // break;
		// // case R.id.encryption_choice_otr:
		// // conversation
		// // .setNextEncryption(Message.ENCRYPTION_OTR);
		// // item.setChecked(true);
		// // break;
		// // case R.id.encryption_choice_pgp:
		// // if (hasPgp()) {
		// // if (conversation.getAccount().getKeys()
		// // .has("pgp_signature")) {
		// // conversation
		// // .setNextEncryption(Message.ENCRYPTION_PGP);
		// // item.setChecked(true);
		// // } else {
		// // announcePgp(conversation.getAccount(),
		// // conversation);
		// // }
		// // } else {
		// // showInstallPgpDialog();
		// // }
		// // break;
		// // default:
		// // conversation
		// // .setNextEncryption(Message.ENCRYPTION_NONE);
		// // break;
		// // }
		// // fragment.updateChatMsgHint();
		// // return true;
		// // }
		// // });
		// // popup.inflate(R.menu.encryption_choices);
		// // MenuItem otr = popup.getMenu().findItem(
		// // R.id.encryption_choice_otr);
		// // if (conversation.getMode() == Conversation.MODE_MULTI) {
		// // otr.setEnabled(false);
		// // }
		// // switch (conversation.getNextEncryption()) {
		// // case Message.ENCRYPTION_NONE:
		// // popup.getMenu().findItem(R.id.encryption_choice_none)
		// // .setChecked(true);
		// // break;
		// // case Message.ENCRYPTION_OTR:
		// // otr.setChecked(true);
		// // break;
		// // case Message.ENCRYPTION_PGP:
		// // popup.getMenu().findItem(R.id.encryption_choice_pgp)
		// // .setChecked(true);
		// // break;
		// // default:
		// // popup.getMenu().findItem(R.id.encryption_choice_none)
		// // .setChecked(true);
		// // break;
		// // }
		// // popup.show();
		// // }
		//
		// break;
		case R.id.action_clear_history:
			clearHistoryDialog(getSelectedConversation());
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	public ArrayList<String> GetFiles(String DirectoryPath) {
		ArrayList<String> MyFiles = new ArrayList<String>();
		File f = new File(DirectoryPath);

		f.mkdirs();
		File[] files = f.listFiles();
		if (files.length == 0)
			return null;
		else {
			for (int i = 0; i < files.length; i++)
				MyFiles.add(files[i].getName());
		}

		return MyFiles;
	}

	private void endConversation(Conversation conversation) {
		conversation.setStatus(Conversation.STATUS_ARCHIVED);
		paneShouldBeOpen = true;
		spl.openPane();
		xmppConnectionService.archiveConversation(conversation);
		if (conversationList.size() > 0) {
			setSelectedConversation(conversationList.get(0));
		} else {
			setSelectedConversation(null);
		}
	}

	protected void clearHistoryDialog(final Conversation conversation) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.clear_conversation_history));
		View dialogView = getLayoutInflater().inflate(
				R.layout.dialog_clear_history, null);
		final CheckBox endConversationCheckBox = (CheckBox) dialogView
				.findViewById(R.id.end_conversation_checkbox);
		builder.setView(dialogView);
		builder.setNegativeButton(getString(R.string.cancel), null);
		builder.setPositiveButton(getString(R.string.delete_messages),
				new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						activity.xmppConnectionService
								.clearConversationHistory(conversation);
						if (endConversationCheckBox.isChecked()) {
							endConversation(conversation);
						}
					}
				});
		builder.create().show();
	}

	protected ConversationFragment swapConversationFragment() {
		ConversationFragment selectedFragment = new ConversationFragment();

		FragmentTransaction transaction = getFragmentManager()
				.beginTransaction();
		transaction.replace(R.id.selected_conversation, selectedFragment,
				"conversation");
		try {
			transaction.commit();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return selectedFragment;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (!spl.isOpen()) {
				spl.openPane();
				return false;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		if ((Intent.ACTION_VIEW.equals(intent.getAction()) && (VIEW_CONVERSATION
				.equals(intent.getType())))) {
			String convToView = (String) intent.getExtras().get(CONVERSATION);
			updateConversationList();
			for (int i = 0; i < conversationList.size(); ++i) {
				if (conversationList.get(i).getUuid().equals(convToView)) {
					setSelectedConversation(conversationList.get(i));
					break;
				}
			}
			paneShouldBeOpen = false;
			String text = intent.getExtras().getString(TEXT, null);
			swapConversationFragment().setText(text);
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		this.useSubject = preferences.getBoolean("use_subject_in_muc", true);
		this.showLastseen = preferences.getBoolean("show_last_seen", false);
		if (this.xmppConnectionServiceBound) {
			this.onBackendConnected();
		}
		if (conversationList.size() >= 1) {
			onConvChanged.onConversationListChanged();
		}
	}

	@Override
	protected void onStop() {
		if (xmppConnectionServiceBound) {
			xmppConnectionService.removeOnConversationListChangedListener();
		}
		super.onStop();
	}

	@Override
	void onBackendConnected() {
		this.registerListener();
		if (conversationList.size() == 0) {
			updateConversationList();
		}

		if ((getIntent().getAction() != null)
				&& (getIntent().getAction().equals(Intent.ACTION_VIEW) && (!handledViewIntent))) {
			if (getIntent().getType().equals(
					ConversationActivity.VIEW_CONVERSATION)) {
				handledViewIntent = true;

				String convToView = (String) getIntent().getExtras().get(
						CONVERSATION);

				for (int i = 0; i < conversationList.size(); ++i) {
					if (conversationList.get(i).getUuid().equals(convToView)) {
						setSelectedConversation(conversationList.get(i));
					}
				}
				paneShouldBeOpen = false;
				String text = getIntent().getExtras().getString(TEXT, null);
				swapConversationFragment().setText(text);
			}
		} else {
			if (xmppConnectionService.getAccounts().size() == 0) {
				startActivity(new Intent(this, ManageAccountActivity.class));
				finish();
			} else if (conversationList.size() <= 0) {
				// add no history
				startActivity(new Intent(this, ContactsActivity.class));
				finish();
			} else {
				spl.openPane();
				// find currently loaded fragment
				ConversationFragment selectedFragment = (ConversationFragment) getFragmentManager()
						.findFragmentByTag("conversation");
				if (selectedFragment != null) {
					selectedFragment.onBackendConnected();
				} else {
					setSelectedConversation(conversationList.get(0));
					swapConversationFragment();
				}
				// ExceptionHelper.checkForCrash(this,
				// this.xmppConnectionService);
			}
		}
	}

	public void registerListener() {
		if (xmppConnectionServiceBound) {
			xmppConnectionService
					.setOnConversationListChangedListener(this.onConvChanged);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			final Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			if (requestCode == REQUEST_DECRYPT_PGP) {
				ConversationFragment selectedFragment = (ConversationFragment) getFragmentManager()
						.findFragmentByTag("conversation");
				if (selectedFragment != null) {
					selectedFragment.hidePgpPassphraseBox();
				}
			} else if (requestCode == REQUEST_ATTACH_FILE_DIALOG) {
				attachImageToConversation(getSelectedConversation(),
						data.getData());
			} else if (requestCode == REQUEST_SEND_PGP_IMAGE) {

			} else if (requestCode == ATTACHMENT_CHOICE_CHOOSE_IMAGE) {
				attachFile(ATTACHMENT_CHOICE_CHOOSE_IMAGE);
			} else if (requestCode == ATTACHMENT_CHOICE_TAKE_PHOTO) {
				attachFile(ATTACHMENT_CHOICE_TAKE_PHOTO);
			} else if (requestCode == REQUEST_ANNOUNCE_PGP) {
				announcePgp(getSelectedConversation().getAccount(),
						getSelectedConversation());
			} else if (requestCode == REQUEST_ENCRYPT_MESSAGE) {
				// encryptTextMessage();
			} else if (requestCode == REQUEST_IMAGE_CAPTURE) {
				attachImageToConversation(getSelectedConversation(), null);
			} else if (requestCode == REQUEST_RECORD_AUDIO) {
				Log.d("xmppService", data.getData().toString());
//				attachAudioToConversation(getSelectedConversation(),
//						data.getData());
			} else {
				Log.d(LOGTAG, "unknown result code:" + requestCode);
			}
		}
	}

	public void attachAudioToConversation(Conversation conversation, Uri uri ,final XmppConnectionService xmppConnectionService ) {
		Utils.tempxmppConnectionService = xmppConnectionService;

		Log.i(Utils.LOG_IMAGE, "xmpp connection service "
				+ xmppConnectionService);

//		if (xmppConnectionService == null) {
//			// xmppConnectionService = new XmppConnectionService();
//			final XmppConnectionService xmppConnectionService = new XmppConnectionService();
//			// xmppConnectionService = Utils.tempxmppConnectionService;
//			xmppConnectionService.attachAudioToConversation(conversation, uri,
//					new UiCallback<Message>() {
//
//						@Override
//						public void userInputRequried(PendingIntent pi,
//								Message object) {
//							// hidePrepareImageToast();
//							ConversationActivity.this
//									.runIntent(
//											pi,
//											ConversationActivity.REQUEST_SEND_PGP_IMAGE);
//						}
//
//						@Override
//						public void success(Message message) {
//							xmppConnectionService.sendMessage(message);
//						}
//
//						@Override
//						public void error(int error, Message message) {
//							// hidePrepareImageToast();
//							displayErrorDialog(error);
//						}
//					});
//		} else {
		
			xmppConnectionService.attachAudioToConversation(
					conversation, uri, new UiCallback<Message>() {

						@Override
						public void userInputRequried(PendingIntent pi,
								Message object) {
							// hidePrepareImageToast();
							ConversationActivity.this
									.runIntent(
											pi,
											ConversationActivity.REQUEST_SEND_PGP_IMAGE);
						}

						@Override
						public void success(Message message) {
							xmppConnectionService.sendMessage(message);
							Log.i(Utils.LOG_IMAGE,
									"attachImage to Conversatoion  , Audio, Ui callback , success");
						}

						@Override
						public void error(int error, Message message) {
							// hidePrepareImageToast();
							displayErrorDialog(error);
							Log.i(Utils.LOG_IMAGE,
									"attachImage to Conversatoion ,  Audio , Ui callback , error");
						}
					});
	//	}

	}

	private void attachImageToConversation(Conversation conversation, Uri uri) {
		Log.i(Utils.LOG_IMAGE, "attachImage to Conversaton");
		Log.i(Utils.LOG_IMAGE, "xmpp connection service "
				+ xmppConnectionService);
		prepareImageToast = Toast.makeText(getApplicationContext(),
				getText(R.string.preparing_image), Toast.LENGTH_LONG);
		prepareImageToast.show();
		xmppConnectionService.attachImageToConversation(conversation, uri,
				new UiCallback<Message>() {

					@Override
					public void userInputRequried(PendingIntent pi,
							Message object) {
						hidePrepareImageToast();
						ConversationActivity.this.runIntent(pi,
								ConversationActivity.REQUEST_SEND_PGP_IMAGE);
						Log.i(Utils.LOG_IMAGE,
								"attachImage to Conversaton , uicallback , userinputRequired");
					}

					@Override
					public void success(Message message) {
						xmppConnectionService.sendMessage(message);
						Log.i(Utils.LOG_IMAGE,
								"attachImage to Conversatoion , Ui callback , success");
					}

					@Override
					public void error(int error, Message message) {
						hidePrepareImageToast();
						Log.i(Utils.LOG_IMAGE,
								"attachImage to Conversaton , uicallback , error");
						displayErrorDialog(error);
					}
				});
	}

	private void hidePrepareImageToast() {
		if (prepareImageToast != null) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					prepareImageToast.cancel();
				}
			});
		}
	}

	public void updateConversationList() {
		conversationList.clear();
		conversationList.addAll(xmppConnectionService.getConversations());
		listView.invalidateViews();
	}

	public boolean showLastseen() {
		if (getSelectedConversation() == null) {
			return false;
		} else {
			return this.showLastseen
					&& getSelectedConversation().getMode() == Conversation.MODE_SINGLE;
		}
	}

	public void runIntent(PendingIntent pi, int requestCode) {
		try {
			this.startIntentSenderForResult(pi.getIntentSender(), requestCode,
					null, 0, 0, 0);
		} catch (SendIntentException e1) {
			Log.d("xmppService", "failed to start intent to send message");
		}
	}

	class BitmapWorkerTask extends AsyncTask<Message, Void, Bitmap> {
		private final WeakReference<ImageView> imageViewReference;
		private Message message = null;

		public BitmapWorkerTask(ImageView imageView) {
			imageViewReference = new WeakReference<ImageView>(imageView);
		}

		@Override
		protected Bitmap doInBackground(Message... params) {
			message = params[0];
			try {
				return xmppConnectionService.getFileBackend().getThumbnail(
						message, (int) (metrics.density * 288), false);
			} catch (FileNotFoundException e) {
				Log.d("xmppService", "file not found!");
				return null;
			}
		}

		@Override
		protected void onPostExecute(Bitmap bitmap) {
			if (imageViewReference != null && bitmap != null) {
				final ImageView imageView = imageViewReference.get();
				if (imageView != null) {
					imageView.setImageBitmap(bitmap);
					imageView.setBackgroundColor(0x00000000);
				}
			}
		}
	}

	public void loadBitmap(Message message, ImageView imageView) {
		Bitmap bm;
		try {
			bm = xmppConnectionService.getFileBackend().getThumbnail(message,
					(int) (metrics.density * 288), true);
		} catch (FileNotFoundException e) {
			bm = null;
		}
		if (bm != null) {
			imageView.setImageBitmap(bm);
			imageView.setBackgroundColor(0x00000000);
		} else {
			if (cancelPotentialWork(message, imageView)) {
				imageView.setBackgroundColor(0xff333333);
				final BitmapWorkerTask task = new BitmapWorkerTask(imageView);
				final AsyncDrawable asyncDrawable = new AsyncDrawable(
						getResources(), null, task);
				imageView.setImageDrawable(asyncDrawable);
				task.execute(message);
			}
		}
	}

	public void loadAudio(Message message) {
		// Bitmap bm;
		try {
			xmppConnectionService.getFileBackend().getAudio(message,
					(int) (metrics.density * 288), true);
		} catch (FileNotFoundException e) {
			// bm = null;
		}
		// if (bm != null) {
		// imageView.setImageBitmap(bm);
		// imageView.setBackgroundColor(0x00000000);
		// } else {
		// if (cancelPotentialWork(message, imageView)) {
		// imageView.setBackgroundColor(0xff333333);
		// final BitmapWorkerTask task = new BitmapWorkerTask(imageView);
		// final AsyncDrawable asyncDrawable = new AsyncDrawable(
		// getResources(), null, task);
		// imageView.setImageDrawable(asyncDrawable);
		// task.execute(message);
		// }
		// }
	}

	public static boolean cancelPotentialWork(Message message,
			ImageView imageView) {
		final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

		if (bitmapWorkerTask != null) {
			final Message oldMessage = bitmapWorkerTask.message;
			if (oldMessage == null || message != oldMessage) {
				bitmapWorkerTask.cancel(true);
			} else {
				return false;
			}
		}
		return true;
	}

	private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
		if (imageView != null) {
			final Drawable drawable = imageView.getDrawable();
			if (drawable instanceof AsyncDrawable) {
				final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
				return asyncDrawable.getBitmapWorkerTask();
			}
		}
		return null;
	}

	static class AsyncDrawable extends BitmapDrawable {
		private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

		public AsyncDrawable(Resources res, Bitmap bitmap,
				BitmapWorkerTask bitmapWorkerTask) {
			super(res, bitmap);
			bitmapWorkerTaskReference = new WeakReference<BitmapWorkerTask>(
					bitmapWorkerTask);
		}

		public BitmapWorkerTask getBitmapWorkerTask() {
			return bitmapWorkerTaskReference.get();
		}
	}

	public void encryptTextMessage(Message message) {
		xmppConnectionService.getPgpEngine().encrypt(message,
				new UiCallback<Message>() {

					@Override
					public void userInputRequried(PendingIntent pi,
							Message message) {
						activity.runIntent(pi,
								ConversationActivity.REQUEST_SEND_MESSAGE);
					}

					@Override
					public void success(Message message) {
						xmppConnectionService.sendMessage(message);
					}

					@Override
					public void error(int error, Message message) {

					}
				});
	}
}
