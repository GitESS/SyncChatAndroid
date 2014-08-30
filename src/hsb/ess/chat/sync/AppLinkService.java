package hsb.ess.chat.sync;

import hsb.ess.chat.R;
import hsb.ess.chat.entities.Account;
import hsb.ess.chat.entities.Contact;
import hsb.ess.chat.entities.Conversation;
import hsb.ess.chat.entities.Message;
import hsb.ess.chat.entities.Presences;
import hsb.ess.chat.persistance.DatabaseBackend;
import hsb.ess.chat.ui.ContactsActivity;
import hsb.ess.chat.ui.ConversationActivity;
import hsb.ess.chat.ui.ConversationFragment;
import hsb.ess.chat.ui.ManageAccountActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.ford.syncV4.exception.SyncException;
import com.ford.syncV4.exception.SyncExceptionCause;
import com.ford.syncV4.proxy.SyncProxyALM;
import com.ford.syncV4.proxy.TTSChunkFactory;
import com.ford.syncV4.proxy.interfaces.IProxyListenerALM;
import com.ford.syncV4.proxy.rpc.AddCommandResponse;
import com.ford.syncV4.proxy.rpc.AddSubMenuResponse;
import com.ford.syncV4.proxy.rpc.Alert;
import com.ford.syncV4.proxy.rpc.AlertResponse;
import com.ford.syncV4.proxy.rpc.ChangeRegistrationResponse;
import com.ford.syncV4.proxy.rpc.Choice;
import com.ford.syncV4.proxy.rpc.CreateInteractionChoiceSet;
import com.ford.syncV4.proxy.rpc.CreateInteractionChoiceSetResponse;
import com.ford.syncV4.proxy.rpc.DeleteCommandResponse;
import com.ford.syncV4.proxy.rpc.DeleteFileResponse;
import com.ford.syncV4.proxy.rpc.DeleteInteractionChoiceSetResponse;
import com.ford.syncV4.proxy.rpc.DeleteSubMenuResponse;
import com.ford.syncV4.proxy.rpc.DialNumberResponse;
import com.ford.syncV4.proxy.rpc.EndAudioPassThruResponse;
import com.ford.syncV4.proxy.rpc.GenericResponse;
import com.ford.syncV4.proxy.rpc.GetDTCsResponse;
import com.ford.syncV4.proxy.rpc.GetVehicleDataResponse;
import com.ford.syncV4.proxy.rpc.ListFilesResponse;
import com.ford.syncV4.proxy.rpc.OnAudioPassThru;
import com.ford.syncV4.proxy.rpc.OnButtonEvent;
import com.ford.syncV4.proxy.rpc.OnButtonPress;
import com.ford.syncV4.proxy.rpc.OnCommand;
import com.ford.syncV4.proxy.rpc.OnDriverDistraction;
import com.ford.syncV4.proxy.rpc.OnHMIStatus;
import com.ford.syncV4.proxy.rpc.OnLanguageChange;
import com.ford.syncV4.proxy.rpc.OnPermissionsChange;
import com.ford.syncV4.proxy.rpc.OnVehicleData;
import com.ford.syncV4.proxy.rpc.PerformAudioPassThru;
import com.ford.syncV4.proxy.rpc.PerformAudioPassThruResponse;
import com.ford.syncV4.proxy.rpc.PerformInteraction;
import com.ford.syncV4.proxy.rpc.PerformInteractionResponse;
import com.ford.syncV4.proxy.rpc.PutFileResponse;
import com.ford.syncV4.proxy.rpc.ReadDIDResponse;
import com.ford.syncV4.proxy.rpc.ResetGlobalPropertiesResponse;
import com.ford.syncV4.proxy.rpc.ScrollableMessageResponse;
import com.ford.syncV4.proxy.rpc.SetAppIconResponse;
import com.ford.syncV4.proxy.rpc.SetDisplayLayoutResponse;
import com.ford.syncV4.proxy.rpc.SetGlobalPropertiesResponse;
import com.ford.syncV4.proxy.rpc.SetMediaClockTimerResponse;
import com.ford.syncV4.proxy.rpc.ShowResponse;
import com.ford.syncV4.proxy.rpc.SliderResponse;
import com.ford.syncV4.proxy.rpc.SoftButton;
import com.ford.syncV4.proxy.rpc.SpeakResponse;
import com.ford.syncV4.proxy.rpc.SubscribeButtonResponse;
import com.ford.syncV4.proxy.rpc.SubscribeVehicleDataResponse;
import com.ford.syncV4.proxy.rpc.TTSChunk;
import com.ford.syncV4.proxy.rpc.UnsubscribeButtonResponse;
import com.ford.syncV4.proxy.rpc.UnsubscribeVehicleDataResponse;
import com.ford.syncV4.proxy.rpc.enums.AudioType;
import com.ford.syncV4.proxy.rpc.enums.BitsPerSample;
import com.ford.syncV4.proxy.rpc.enums.ButtonName;
import com.ford.syncV4.proxy.rpc.enums.DriverDistractionState;
import com.ford.syncV4.proxy.rpc.enums.InteractionMode;
import com.ford.syncV4.proxy.rpc.enums.Result;
import com.ford.syncV4.proxy.rpc.enums.SamplingRate;
import com.ford.syncV4.proxy.rpc.enums.SoftButtonType;
import com.ford.syncV4.proxy.rpc.enums.SpeechCapabilities;
import com.ford.syncV4.proxy.rpc.enums.SystemAction;
import com.ford.syncV4.proxy.rpc.enums.TextAlignment;
import com.ford.syncV4.util.DebugTool;

/**
 * @author Hemant Bisht
 *
 *
 * @Project_Name @SyncChatDev
 * @File_Name @AppLinkService.java
 * @Created_on @Aug 27, 2014
 * */

public class AppLinkService extends Service implements IProxyListenerALM {

	String TAG = "SyncService";
	// variable used to increment correlation ID for every request sent to SYNC
	public int autoIncCorrId = 0;
	// variable to contain the current state of the service
	private static AppLinkService instance = null;
	// variable to contain the current state of the main UI ACtivity
	private ConversationFragment currentUIActivity;
	// variable to access the BluetoothAdapter
	private BluetoothAdapter mBtAdapter;
	// variable to create and call functions of the SyncProxy
	private SyncProxyALM proxy = null;
	// variable that keeps track of whether SYNC is sending driver distractions
	// (older versions of SYNC will not send this notification)
	private boolean driverdistrationNotif = false;
	// variable to contain the current state of the lockscreen
	private boolean lockscreenUP = false;
	ManageAccountActivity manageA;

	public boolean isSyncConnectedToChat = false;
	// variable that keeps track if Friend or Group is Clicked
	// private boolean isFriendIsClicked = false;
	private int VRCommandSelected = 0;
	private static final int vrFriend = 0;
	private static final int vrGroup = 1;
	private static final int vrContact = 2;
	Account account;

	// variable which keeps track of Conversation , If a conversation is already
	// in place or not
	private boolean isConversationSelected = false;

	List<Contact> onlineContactsList, groupsList, contactslist;
	private static final int CHOICE_FRIENDS = 2016;
	private static final int ONLINE_FRIENDS_CMDID = 1001;

	private List<Integer> ONLINE_FRIENDS_ID;
	private static int CHOICE_ONLINE_FRIENDS_ID = 2004;

	private static final int CHOICE_CONTACTS = 2018;
	private List<Integer> CONTACTS_LIST;
	private static int CHOICE_CONTACTS_ID = 4000;

	private static final int CHOICE_GROUPS = 2017;
	private static final int ONLINE_GROUPS_CMDID = 1002;
	private List<Integer> ONLINE_GROUP_ID;
	private static int CHOICE_GROUPS_ID = 5001;

	// variables responsible for VR Command after friend is Clicked
	private static final int VR_SEND_MESSAGE_ID = 1003;

	private static final int VR_BACK_TO_MENU_ID = 1004;

	// variables resposible for sending messages
	private String conCONTACT_JID = "";
	private Account conACOUNT_NAME = null;
	private String conSENDER_NAME = "";
	private int conCONVERSATION_MODE;

	Handler mHandler, childHandler;
	ContactsActivity contactActivity;

	private Account finalaccount;

	/*
	 * 
	 * SoftButton ids Main : - 106, 107 ,108, 109 For Group/ friend/ recieved
	 * message - 111, 112 for Creating Group : - 130
	 */

	// private Recorder mRecorder;

	public static AppLinkService getInstance() {
		return instance;
	}

	public ConversationFragment getCurrentActivity() {
		return currentUIActivity;
	}

	public SyncProxyALM getProxy() {
		return proxy;
	}

	public void setCurrentActivity(ConversationFragment currentActivity) {
		this.currentUIActivity = currentActivity;
	}

	public void onCreate() {
		super.onCreate();
		instance = this;
	}

	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null) {
			mBtAdapter = BluetoothAdapter.getDefaultAdapter();
			if (mBtAdapter != null) {
				if (mBtAdapter.isEnabled()) {
					startProxy();
				}
			}
		}
		if (ConversationFragment.getInstance() != null) {
			setCurrentActivity(ConversationFragment.getInstance());
		}
		// mRecorder = new Recorder(this, mRecordingHandler);
		manageA = new ManageAccountActivity();
		return START_STICKY;
	}

	public void startProxy() {
		if (proxy == null) {
			try {
				proxy = new SyncProxyALM(this, "Sync Chat", true, "584421907");
			} catch (SyncException e) {
				e.printStackTrace();
				// error creating proxy, returned proxy = null
				if (proxy == null) {
					stopSelf();
				}
			}
		}
	}

	public void onDestroy() {
		disposeSyncProxy();
		clearlockscreen();
		instance = null;
		super.onDestroy();
	}

	public void disposeSyncProxy() {
		if (proxy != null) {
			try {
				proxy.dispose();
			} catch (SyncException e) {
				e.printStackTrace();
			}
			proxy = null;
			clearlockscreen();
		}
	}

	@Override
	public void onProxyClosed(String info, Exception e) {
		clearlockscreen();

		if ((((SyncException) e).getSyncExceptionCause() != SyncExceptionCause.SYNC_PROXY_CYCLED)) {
			if (((SyncException) e).getSyncExceptionCause() != SyncExceptionCause.BLUETOOTH_DISABLED) {
				Log.v(TAG, "reset proxy in onproxy closed");
				reset();
			}
		}
	}

	public void reset() {
		if (proxy != null) {
			try {
				proxy.resetProxy();
			} catch (SyncException e1) {
				e1.printStackTrace();
				// something goes wrong, & the proxy returns as null, stop the
				// service.
				// do not want a running service with a null proxy
				if (proxy == null) {
					stopSelf();
				}
			}
		} else {
			startProxy();
		}
	}

	@Override
	public void onOnHMIStatus(OnHMIStatus notification) {

		switch (notification.getSystemContext()) {
		case SYSCTXT_MAIN:
			break;
		case SYSCTXT_VRSESSION:
			break;
		case SYSCTXT_MENU:
			break;
		default:
			return;
		}

		switch (notification.getAudioStreamingState()) {
		case AUDIBLE:
			// play audio if applicable
			break;
		case NOT_AUDIBLE:
			// pause/stop/mute audio if applicable
			break;
		default:
			return;
		}

		switch (notification.getHmiLevel()) {
		case HMI_FULL:
			if (driverdistrationNotif == false) {
				showLockScreen();
			}
			if (notification.getFirstRun()) {
				// setup app on SYNC
				// send welcome message if applicable
				try {
					proxy.show("Welcome to ", "Sync Chat",
							TextAlignment.CENTERED, autoIncCorrId++);
					proxy.speak("Welcome to sync Chat", autoIncCorrId++);

				} catch (SyncException e) {
					DebugTool.logError("Failed to send Show", e);
				}
				// send addcommands
				// subscribe to buttons
				subButtons();
				showSoftButtonsOnScreen();
				initializeVoiceCommand();
				refreshOnlineFriendsAndGroup();

				createVRForContactList();
				try {
					proxy.show("Sync Chat", "Application",
							TextAlignment.CENTERED, autoIncCorrId++);
				} catch (Exception e) {

				}

				if (ConversationFragment.getInstance() != null) {
					setCurrentActivity(ConversationFragment.getInstance());
				}
				manageA.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (manageA.isMyAccountIsOnline()) {
							Intent i = new Intent(AppLinkService.this,
									LockScreenActivity.class);
							startActivity(i);
						} else {
							if (ManageAccountActivity.getInstance() != null)
								ManageAccountActivity.getInstance().finish();
							Intent i = new Intent(AppLinkService.this,
									ManageAccountActivity.class);

							i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							i.putExtra("ServiceIntent", "start");
							AppLinkService.this.startActivity(i);
						}
					}
				});
				isSyncConnectedToChat = true;
			} else {

			}

			if (ConversationFragment.getInstance() != null
					&& manageA.isMyAccountIsOnline()) {
				Intent i = new Intent(AppLinkService.this,
						LockScreenActivity.class);
				startActivity(i);
			}
			break;
		case HMI_LIMITED:
			if (driverdistrationNotif == false) {
				showLockScreen();
			}
			break;
		case HMI_BACKGROUND:
			if (driverdistrationNotif == false) {
				showLockScreen();
			}
			break;
		case HMI_NONE:
			Log.i("hello", "HMI_NONE");
			driverdistrationNotif = false;
			clearlockscreen();
			isSyncConnectedToChat = false;
			break;
		default:
			return;
		}
	}

	public void showLockScreen() {
		lockscreenUP = true;
	}

	private void clearlockscreen() {
		if (LockScreenActivity.getInstance() != null) {
			LockScreenActivity.getInstance().exit();
		}
		lockscreenUP = false;
	}

	public boolean getLockScreenStatus() {
		return lockscreenUP;
	}

	public void subButtons() {
		try {
			proxy.subscribeButton(ButtonName.OK, autoIncCorrId++);
			proxy.subscribeButton(ButtonName.SEEKLEFT, autoIncCorrId++);
			proxy.subscribeButton(ButtonName.SEEKRIGHT, autoIncCorrId++);
			proxy.subscribeButton(ButtonName.TUNEUP, autoIncCorrId++);
			proxy.subscribeButton(ButtonName.TUNEDOWN, autoIncCorrId++);
			proxy.subscribeButton(ButtonName.PRESET_1, autoIncCorrId++);
			proxy.subscribeButton(ButtonName.PRESET_2, autoIncCorrId++);
			proxy.subscribeButton(ButtonName.PRESET_3, autoIncCorrId++);
			proxy.subscribeButton(ButtonName.PRESET_4, autoIncCorrId++);
			proxy.subscribeButton(ButtonName.PRESET_5, autoIncCorrId++);
			proxy.subscribeButton(ButtonName.PRESET_6, autoIncCorrId++);
			proxy.subscribeButton(ButtonName.PRESET_7, autoIncCorrId++);
			proxy.subscribeButton(ButtonName.PRESET_8, autoIncCorrId++);
			proxy.subscribeButton(ButtonName.PRESET_9, autoIncCorrId++);
			proxy.subscribeButton(ButtonName.PRESET_0, autoIncCorrId++);
		} catch (SyncException e) {
		}
	}

	@Override
	public void onOnDriverDistraction(OnDriverDistraction notification) {
		driverdistrationNotif = true;

		if (notification.getState() == DriverDistractionState.DD_OFF) {
			Log.i(TAG, "clear lock, DD_OFF");
			clearlockscreen();
		} else {
			Log.i(TAG, "show lockscreen, DD_ON");
			showLockScreen();
		}
	}

	@Override
	public void onError(String info, Exception e) {

	}

	@Override
	public void onGenericResponse(GenericResponse response) {

	}

	@Override
	public void onOnCommand(OnCommand notification) {
		switch (notification.getCmdID()) {
		case ONLINE_FRIENDS_CMDID: // for Choice set
			performInteraction();

			VRCommandSelected = vrFriend;
			refreshOnlineFriendsAndGroup();
			break;

		case ONLINE_GROUPS_CMDID:
			performInteractionGroup();

			VRCommandSelected = vrGroup;
			refreshOnlineFriendsAndGroup();
			break;

		case VR_SEND_MESSAGE_ID:
			sendRecorderMessage();

			break;

		// case VR_SEND_MESSAGE_ID:
		// sendRecorderMessage();
		//
		// break;
		default:
			break;

		}
	}

	@Override
	public void onAddCommandResponse(AddCommandResponse response) {

	}

	@Override
	public void onAddSubMenuResponse(AddSubMenuResponse response) {

	}

	@Override
	public void onCreateInteractionChoiceSetResponse(
			CreateInteractionChoiceSetResponse response) {

	}

	@Override
	public void onAlertResponse(AlertResponse response) {

	}

	@Override
	public void onDeleteCommandResponse(DeleteCommandResponse response) {

	}

	@Override
	public void onDeleteInteractionChoiceSetResponse(
			DeleteInteractionChoiceSetResponse response) {

	}

	@Override
	public void onDeleteSubMenuResponse(DeleteSubMenuResponse response) {

	}

	@Override
	public void onPerformInteractionResponse(PerformInteractionResponse response) {
		if (VRCommandSelected == vrFriend) {

			for (int i = 0; i < onlineContactsList.size(); i++) {
				int ContactID = ONLINE_FRIENDS_ID.get(i);

				if (response.getChoiceID() == ContactID) {
					Log.d("service",
							"In PerformInteractionResponse and clicked item is "
									+ i);

					Account con = onlineContactsList.get(i).getAccount();
					String name = onlineContactsList.get(i).getDisplayName();
					String contactJid = onlineContactsList.get(i).getJid();

					conACOUNT_NAME = con;
					conCONTACT_JID = contactJid;
					conSENDER_NAME = name;
					conCONVERSATION_MODE = Conversation.MODE_SINGLE;

					showSoftButtonsForFriends();
					initializeVoiceCommandForFriend();
					try {
						proxy.show("Friend", name, TextAlignment.CENTERED,
								autoIncCorrId++);
					} catch (SyncException e) {

						e.printStackTrace();
					}

					return;
				}
			}
		} else if (VRCommandSelected == vrGroup) {

			if (groupsList.size() > 0) {
				for (int i = 0; i < groupsList.size(); i++) {
					int ContactID = ONLINE_GROUP_ID.get(i);

					if (response.getChoiceID() == ContactID) {
						Account con = groupsList.get(i).getAccount();
						String name = groupsList.get(i).getDisplayName();
						String contactJid = groupsList.get(i).getJid();
						conACOUNT_NAME = con;
						conCONTACT_JID = contactJid;
						conSENDER_NAME = name;
						conCONVERSATION_MODE = Conversation.MODE_MULTI;

						showSoftButtonsForGroups();
						try {
							proxy.show("Group", name, TextAlignment.CENTERED,
									autoIncCorrId++);
						} catch (SyncException e) {

							e.printStackTrace();
						}
					}
				}
			}
		} else if (VRCommandSelected == vrContact) {

			if (contactActivity == null) {
				ConversationActivity.getInstance().runOnUiThread(
						new Runnable() {

							@Override
							public void run() {

								contactActivity = new ContactsActivity();
							}
						});
			}

			for (int i = 0; i < contactslist.size(); i++) {
				int ContactID = CONTACTS_LIST.get(i);

				if (response.getChoiceID() == ContactID) {
					final Account con = contactslist.get(i).getAccount();
					final Contact tempContact = contactslist.get(i);
					ConversationActivity.getInstance().runOnUiThread(
							new Runnable() {
								@Override
								public void run() {
									contactActivity.inviteToGroupFromService(
											con, tempContact, conCONTACT_JID);
								}

							});
				}
			}

		}

	}

	@Override
	public void onResetGlobalPropertiesResponse(
			ResetGlobalPropertiesResponse response) {

	}

	@Override
	public void onSetGlobalPropertiesResponse(
			SetGlobalPropertiesResponse response) {
	}

	@Override
	public void onSetMediaClockTimerResponse(SetMediaClockTimerResponse response) {

	}

	@Override
	public void onShowResponse(ShowResponse response) {

	}

	@Override
	public void onSpeakResponse(SpeakResponse response) {

	}

	@Override
	public void onOnButtonEvent(OnButtonEvent notification) {

		if (notification.getButtonName() == ButtonName.PRESET_2) {

			try {
				proxy.speak(" 2 is clicked", autoIncCorrId++);
			} catch (SyncException e) {

				e.printStackTrace();
			}
		}
		if (notification.getButtonName() == ButtonName.PRESET_8) {

		}

	}

	@Override
	public void onOnButtonPress(OnButtonPress notification) {

		switch (notification.getCustomButtonName()) {
		case 106:
			performInteraction();

			// isFriendIsClicked = true;
			VRCommandSelected = vrFriend;
			refreshOnlineFriendsAndGroup();
			break;
		case 107:
			performInteractionGroup();
			VRCommandSelected = vrGroup;
			// isFriendIsClicked = false;
			refreshOnlineFriendsAndGroup();
			break;

		case 109:

			ConversationActivity.getInstance().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					contactActivity = new ContactsActivity();
					if (finalaccount != null && contactActivity != null)
						contactActivity
								.startConferenceFromService(finalaccount);

				}

			});
			try {
				softButtonAfterCreatingGroup();
				proxy.show("Group Created", contactActivity.getMucName(),
						TextAlignment.CENTERED, autoIncCorrId++);
			} catch (Exception e) {

			}

			break;
		case 111:
			sendRecorderMessage();
			break;

		case 112:
			try {

				showSoftButtonsOnScreen();
				proxy.show("Sync Chat", "Application", TextAlignment.CENTERED,
						autoIncCorrId++);
			} catch (Exception e) {

			}

			break;

		case 121:
			sendMessage(conSENDER_NAME, conACOUNT_NAME, conCONTACT_JID,
					Conversation.MODE_MULTI);

			break;

		case 131:
			performInteractionContact();
			VRCommandSelected = vrContact;
			break;
		default:
			break;
		}

	}

	@Override
	public void onSubscribeButtonResponse(SubscribeButtonResponse response) {

	}

	@Override
	public void onUnsubscribeButtonResponse(UnsubscribeButtonResponse response) {

	}

	@Override
	public void onOnPermissionsChange(OnPermissionsChange notification) {

	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onSubscribeVehicleDataResponse(
			SubscribeVehicleDataResponse response) {

	}

	@Override
	public void onUnsubscribeVehicleDataResponse(
			UnsubscribeVehicleDataResponse response) {

	}

	@Override
	public void onGetVehicleDataResponse(GetVehicleDataResponse response) {

	}

	@Override
	public void onReadDIDResponse(ReadDIDResponse response) {

	}

	@Override
	public void onGetDTCsResponse(GetDTCsResponse response) {

	}

	@Override
	public void onOnVehicleData(OnVehicleData notification) {

	}

	@Override
	public void onPerformAudioPassThruResponse(
			PerformAudioPassThruResponse response) {
		final Result result = response.getResultCode();
		ConversationActivity.getInstance().runOnUiThread(new Runnable() {
			@Override
			public void run() {

				AudioRecorderClass.getInstance().performAudioPassThruResponse(
						result,
						getConversation(conSENDER_NAME, conACOUNT_NAME,
								conCONTACT_JID, conCONVERSATION_MODE));
			}
		});

	}

	@Override
	public void onEndAudioPassThruResponse(EndAudioPassThruResponse response) {

		final ConversationActivity mainActivity = ConversationActivity
				.getInstance();
		final Result result = response.getResultCode();
		mainActivity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				AudioRecorderClass.getInstance().endAudioPassThruResponse(
						result,
						getConversation(conSENDER_NAME, conACOUNT_NAME,
								conCONTACT_JID, conCONVERSATION_MODE));
			}
		});
	}

	@Override
	public void onOnAudioPassThru(OnAudioPassThru notification) {

		// Log.i("OnAudioPassThruNotif", "-" + notification.toString());
		final byte[] aptData = notification.getAPTData();
		ConversationActivity.getInstance().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				AudioRecorderClass.getInstance().audioPassThru(aptData);
				// RecordingAudio.getInstance().audioPassThru(aptData);
			}

		});

	}

	@Override
	public void onPutFileResponse(PutFileResponse response) {

	}

	@Override
	public void onDeleteFileResponse(DeleteFileResponse response) {

	}

	@Override
	public void onListFilesResponse(ListFilesResponse response) {

	}

	@Override
	public void onSetAppIconResponse(SetAppIconResponse response) {

	}

	@Override
	public void onScrollableMessageResponse(ScrollableMessageResponse response) {

	}

	@Override
	public void onChangeRegistrationResponse(ChangeRegistrationResponse response) {

	}

	@Override
	public void onSetDisplayLayoutResponse(SetDisplayLayoutResponse response) {

	}

	@Override
	public void onOnLanguageChange(OnLanguageChange notification) {

	}

	@Override
	public void onSliderResponse(SliderResponse response) {

	}

	@Override
	public void onDialNumberResponse(DialNumberResponse response) {

	}

	public void SpeakOutNow(String message, String contact, String contactJid,
			int mode) {

		try {
			List<Account> accountList = new ArrayList<Account>();
			accountList
					.addAll(ConversationActivity.getInstance().xmppConnectionService
							.getAccounts());

			conACOUNT_NAME = accountList.get(0);

			proxy.speak(" " + message, autoIncCorrId++);
			if (!isConversationSelected) {

				showSoftButtonWhenNewMessageArrives();
				conCONTACT_JID = contactJid;
				conSENDER_NAME = contact;
				conCONVERSATION_MODE = mode;
				try {
					if (mode == Conversation.MODE_SINGLE) {
						proxy.show("Friend", contact, TextAlignment.CENTERED,
								autoIncCorrId++);
					} else {
						proxy.show("Group", contact, TextAlignment.CENTERED,
								autoIncCorrId++);
					}

				} catch (SyncException e) {

					e.printStackTrace();
				}
			}

		} catch (SyncException e) {

			e.printStackTrace();
		}
	}

	public boolean isMyServiceRunning(Class<?> serviceClass) {
		ActivityManager manager = (ActivityManager) this
				.getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			if (serviceClass.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	/*
	 * Initializes the VR commands.
	 * 
	 * @return : null
	 */
	private void initializeVoiceCommand() {
		try {
			proxy.addCommand(
					/* 1002 */ONLINE_FRIENDS_CMDID,
					"Online friends",
					new Vector<String>(Arrays.asList(new String[] { "Online",
							"Friend", "Online Friends" })), autoIncCorrId);

			proxy.addCommand(
					/* 1002 */ONLINE_GROUPS_CMDID,
					"Groups",
					new Vector<String>(Arrays.asList(new String[] { "Groups",
							"Group", "Groups" })), autoIncCorrId);
		} catch (SyncException e) {
			// Log.e(TAG, "Error adding AddCommands", e);
		}
	}

	/*
	 * Initializes the VR commands For Messages.
	 * 
	 * @return : null
	 */
	private void initializeVoiceCommandForFriend() {
		try {
			proxy.addCommand(
					/* 1002 */VR_SEND_MESSAGE_ID,
					"send",
					new Vector<String>(Arrays.asList(new String[] { "send",
							"sent", "reply" })), autoIncCorrId);

			proxy.addCommand(
			/* 1002 */VR_BACK_TO_MENU_ID, "Groups",
					new Vector<String>(Arrays.asList(new String[] { "BACK" })),
					autoIncCorrId);
		} catch (SyncException e) {
			// Log.e(TAG, "Error adding AddCommands", e);
		}
	}

	// Then call PerformInteraction，make sure RPC CreateChoiceSet has been
	// successfully processed:
	public void performInteraction() {
		PerformInteraction msg = new PerformInteraction();
		msg.setCorrelationID(autoIncCorrId++);
		Vector<Integer> interactionChoiceSetIDs = new Vector<Integer>();
		interactionChoiceSetIDs.add(CHOICE_FRIENDS);
		Vector<TTSChunk> initChunks = TTSChunkFactory
				.createSimpleTTSChunks("Select your friend");
		Vector<TTSChunk> helpChunks = TTSChunkFactory
				.createSimpleTTSChunks("Please say the name of your Friend ");
		Vector<TTSChunk> timeoutChunks = TTSChunkFactory
				.createSimpleTTSChunks("you miss the chance to pick");
		msg.setInitialPrompt(initChunks);
		msg.setInitialText("Friend List : Please say the name of your friend");
		msg.setInteractionChoiceSetIDList(interactionChoiceSetIDs);
		msg.setInteractionMode(InteractionMode.BOTH);
		msg.setTimeout(10000);
		msg.setHelpPrompt(helpChunks);
		msg.setTimeoutPrompt(timeoutChunks);
		try {
			proxy.sendRPCRequest(msg);
		} catch (SyncException e) {
			Log.e(TAG, "Error sending message");
		}

	}

	// For Group Perform Interaction
	public void performInteractionGroup() {
		PerformInteraction msg = new PerformInteraction();
		msg.setCorrelationID(autoIncCorrId++);
		Vector<Integer> interactionChoiceSetIDs = new Vector<Integer>();
		interactionChoiceSetIDs.add(CHOICE_GROUPS);
		Vector<TTSChunk> initChunks = TTSChunkFactory
				.createSimpleTTSChunks("Select A Group");
		Vector<TTSChunk> helpChunks = TTSChunkFactory
				.createSimpleTTSChunks("Please say the Group Name ");
		Vector<TTSChunk> timeoutChunks = TTSChunkFactory
				.createSimpleTTSChunks("you miss the chance to pick");
		msg.setInitialPrompt(initChunks);
		msg.setInitialText("Groups : Select a Group");
		msg.setInteractionChoiceSetIDList(interactionChoiceSetIDs);
		msg.setInteractionMode(InteractionMode.BOTH);
		msg.setTimeout(10000);
		msg.setHelpPrompt(helpChunks);
		msg.setTimeoutPrompt(timeoutChunks);
		try {
			proxy.sendRPCRequest(msg);
		} catch (SyncException e) {
			Log.e(TAG, "Error sending message");
		}

	}

	public void refreshOnlineFriendsAndGroup() {
		Log.i("ApplinkService", "In PerformInterac");

		Vector<Choice> commands = new Vector<Choice>();
		Vector<Choice> commands2 = new Vector<Choice>();
		List<Contact> tempgroupList, tempcontactList, tempRosterContacts;
		tempgroupList = new ArrayList<Contact>();
		tempcontactList = new ArrayList<Contact>();
		tempRosterContacts = new ArrayList<Contact>();
		onlineContactsList = new ArrayList<Contact>();
		groupsList = new ArrayList<Contact>();
		List<Account> accountList = new ArrayList<Account>();
		accountList
				.addAll(ConversationActivity.getInstance().xmppConnectionService
						.getAccounts());

		finalaccount = accountList.get(0);
		tempRosterContacts = accountList.get(0).getRoster().getContacts();

		for (int i = 0; i < tempRosterContacts.size(); i++) {
			if (tempRosterContacts.get(i).couldBeMuc()) {
				tempgroupList.add(tempRosterContacts.get(i));
			} else {
				Presences pre = tempRosterContacts.get(i).getPresences();
				Log.i("applinkService",
						"Presence" + pre.getMostAvailableStatus());
				if (pre.getMostAvailableStatus() == Presences.ONLINE) {

					tempcontactList.add(tempRosterContacts.get(i));
				}
			}
			Log.i("Hemant", "ContactList :" + tempcontactList.size()
					+ " groupList : " + tempgroupList.size());
		}

		onlineContactsList.addAll(tempcontactList);
		groupsList.addAll(tempgroupList);

		if (onlineContactsList.size() != 0) {
			Log.i("service", "Contact List" + onlineContactsList.size());
		}

		ONLINE_FRIENDS_ID = new ArrayList<Integer>();
		for (int i = 0; i < onlineContactsList.size(); i++) {
			ONLINE_FRIENDS_ID.add(CHOICE_ONLINE_FRIENDS_ID + i);
			Contact name = onlineContactsList.get(i);
			String contactName = name.getDisplayName();
			Choice one = new Choice();
			one.setChoiceID(CHOICE_ONLINE_FRIENDS_ID + i);
			one.setMenuName(contactName);
			one.setVrCommands(new Vector<String>(Arrays
					.asList(new String[] { contactName })));
			one.setImage(null);
			commands.add(one);

		}

		// For Groups

		ONLINE_GROUP_ID = new ArrayList<Integer>();
		for (int i = 0; i < groupsList.size(); i++) {
			ONLINE_GROUP_ID.add(CHOICE_GROUPS_ID + i);
			Contact name = groupsList.get(i);
			String contactName = name.getDisplayName();
			Choice TWO = new Choice();
			TWO.setChoiceID(CHOICE_GROUPS_ID + i);
			TWO.setMenuName(contactName);
			TWO.setVrCommands(new Vector<String>(Arrays
					.asList(new String[] { contactName })));
			TWO.setImage(null);
			commands2.add(TWO);

		}

		if (!commands.isEmpty()) {
			Log.e(TAG, "send choice set to SYNC");
			CreateInteractionChoiceSet msg2 = new CreateInteractionChoiceSet();
			msg2.setCorrelationID(autoIncCorrId++);
			int choiceSetID = CHOICE_FRIENDS;
			msg2.setInteractionChoiceSetID(choiceSetID);
			msg2.setChoiceSet(commands);
			try {
				proxy.sendRPCRequest(msg2);
			} catch (SyncException e) {
				Log.e(TAG, "Error sending message: ");
			}
		} else {

		}

		if (!commands2.isEmpty()) {
			Log.e(TAG, "send choice set to SYNC");
			CreateInteractionChoiceSet msg3 = new CreateInteractionChoiceSet();
			msg3.setCorrelationID(autoIncCorrId++);
			int choiceSetID = CHOICE_GROUPS;
			msg3.setInteractionChoiceSetID(choiceSetID);
			msg3.setChoiceSet(commands2);
			try {
				proxy.sendRPCRequest(msg3);
			} catch (SyncException e) {
				Log.e(TAG, "Error sending message: ");
			}
		} else {

		}
	}

	private void performInteractionContact() {

		PerformInteraction msg = new PerformInteraction();
		msg.setCorrelationID(autoIncCorrId++);
		Vector<Integer> interactionChoiceSetIDs = new Vector<Integer>();
		interactionChoiceSetIDs.add(CHOICE_CONTACTS);
		Vector<TTSChunk> initChunks = TTSChunkFactory
				.createSimpleTTSChunks("Select a friend to add");
		Vector<TTSChunk> helpChunks = TTSChunkFactory
				.createSimpleTTSChunks("Please say the name of your Friend ");
		Vector<TTSChunk> timeoutChunks = TTSChunkFactory
				.createSimpleTTSChunks("you miss the chance to pick");
		msg.setInitialPrompt(initChunks);
		msg.setInitialText("Friend List : Please say the name of your friend");
		msg.setInteractionChoiceSetIDList(interactionChoiceSetIDs);
		msg.setInteractionMode(InteractionMode.BOTH);
		msg.setTimeout(10000);
		msg.setHelpPrompt(helpChunks);
		msg.setTimeoutPrompt(timeoutChunks);
		try {
			proxy.sendRPCRequest(msg);
		} catch (SyncException e) {
			Log.e(TAG, "Error sending message");
		}

	}

	/**
	 * Loads all the Contact list and stores on arraylist for VR Command
	 * */
	private void createVRForContactList() {
		Vector<Choice> commands = new Vector<Choice>();
		List<Contact> tempcontactList, tempRosterContacts;
		tempcontactList = new ArrayList<Contact>();
		tempRosterContacts = new ArrayList<Contact>();
		contactslist = new ArrayList<Contact>();
		// groupsList = new ArrayList<Contact>();
		List<Account> accountList = new ArrayList<Account>();
		accountList
				.addAll(ConversationActivity.getInstance().xmppConnectionService
						.getAccounts());

		tempRosterContacts = accountList.get(0).getRoster().getContacts();
		Log.i("ApplinkService", "Contacts" + tempRosterContacts.size());
		for (int i = 0; i < tempRosterContacts.size(); i++) {
			if (tempRosterContacts.get(i).couldBeMuc()) {
			} else {
				tempcontactList.add(tempRosterContacts.get(i));
			}
		}

		contactslist.addAll(tempcontactList);// groupsList.addAll(tempgroupList);

		if (contactslist.size() != 0) {
			Log.i("service", "Contact List" + onlineContactsList.size());
		}

		CONTACTS_LIST = new ArrayList<Integer>();
		for (int i = 0; i < contactslist.size(); i++) {
			CONTACTS_LIST.add(CHOICE_CONTACTS_ID + i);
			Contact name = contactslist.get(i);
			String contactName = name.getDisplayName();
			Choice one = new Choice();
			one.setChoiceID(CHOICE_CONTACTS_ID + i);
			one.setMenuName(contactName);
			one.setVrCommands(new Vector<String>(Arrays
					.asList(new String[] { contactName })));
			one.setImage(null);
			commands.add(one);

		}

		if (!commands.isEmpty()) {
			Log.e(TAG, "send choice set to SYNC");
			CreateInteractionChoiceSet msg2 = new CreateInteractionChoiceSet();
			msg2.setCorrelationID(autoIncCorrId++);
			int choiceSetID = CHOICE_CONTACTS;
			msg2.setInteractionChoiceSetID(choiceSetID);
			msg2.setChoiceSet(commands);
			try {
				proxy.sendRPCRequest(msg2);
			} catch (SyncException e) {
				Log.e(TAG, "Error sending message: ");
			}
		} else {

		}

	}

	public void SetAlert(String name, int time, String msg) {
		Alert alert = new Alert();
		alert.setAlertText1(name);
		alert.setDuration(time);
		alert.setCorrelationID(autoIncCorrId++);
		Vector<TTSChunk> ttsChunks = new Vector<TTSChunk>();
		ttsChunks
				.add(TTSChunkFactory.createChunk(SpeechCapabilities.TEXT, msg));
		alert.setTtsChunks(ttsChunks);
		try {
			proxy.sendRPCRequest(alert);

		} catch (SyncException e) {

		}

	}

	private void showSoftButtonsOnScreen() {
		isConversationSelected = false;
		// Add Soft button name
		ArrayList<String> SoftButtonName = new ArrayList<String>();
		SoftButtonName.add("Friends");
		SoftButtonName.add("Groups");
		SoftButtonName.add("Create Group");

		ArrayList<Integer> SoftButtonId = new ArrayList<Integer>();
		SoftButtonId.add(106);
		SoftButtonId.add(107);
		SoftButtonId.add(109);

		Vector<SoftButton> vsoftButton = new Vector<SoftButton>();
		SoftButton softButton;
		try {
			for (int i = 0; i < SoftButtonName.size(); i++) {
				softButton = new SoftButton();
				softButton.setText(SoftButtonName.get(i));
				softButton.setSoftButtonID(SoftButtonId.get(i));
				softButton.setType(SoftButtonType.SBT_TEXT);
				softButton.setSystemAction(SystemAction.DEFAULT_ACTION);
				vsoftButton.add(softButton);

			}
			// Send Show RPC:
			proxy.show("", "", "", "", null, vsoftButton, null, null,
					autoIncCorrId++);
		} catch (SyncException e) {

		}

	}

	/**
	 * Will declares the Soft buttons when group is selected.
	 * 
	 * 
	 * */
	private void softButtonAfterCreatingGroup() {

		isConversationSelected = false;
		// Add Soft button name
		ArrayList<String> SoftButtonName = new ArrayList<String>();
		// SoftButtonName.add("Add Friend");
		SoftButtonName.add("Back");
		// SoftButtonName.add("Vehicle");

		// Add Soft buttonID
		ArrayList<Integer> SoftButtonId = new ArrayList<Integer>();
		// SoftButtonId.add(131);
		SoftButtonId.add(112);

		Vector<SoftButton> vsoftButton = new Vector<SoftButton>();
		SoftButton softButton;
		try {
			for (int i = 0; i < SoftButtonName.size(); i++) {
				softButton = new SoftButton();
				softButton.setText(SoftButtonName.get(i));
				softButton.setSoftButtonID(SoftButtonId.get(i));
				softButton.setType(SoftButtonType.SBT_TEXT);
				softButton.setSystemAction(SystemAction.DEFAULT_ACTION);
				vsoftButton.add(softButton);

			}
			// Send Show RPC:
			proxy.show("", "", "", "", null, vsoftButton, null, null,
					autoIncCorrId++);
		} catch (SyncException e) {

		}

	}

	/**
	 * Will be called when friends is clicked, Has options like Send and Back
	 * friends softbutton ids = 111-120
	 * */
	private void showSoftButtonsForFriends() {

		isConversationSelected = true;
		// Add Soft button name
		ArrayList<String> SoftButtonName = new ArrayList<String>();
		SoftButtonName.add("Send");
		SoftButtonName.add("Back");

		// SoftButtonName.add("Vehicle");

		// Add Soft buttonID
		ArrayList<Integer> SoftButtonId = new ArrayList<Integer>();
		SoftButtonId.add(111);
		SoftButtonId.add(112);

		Vector<SoftButton> vsoftButton = new Vector<SoftButton>();
		SoftButton softButton;
		try {
			for (int i = 0; i < SoftButtonName.size(); i++) {
				softButton = new SoftButton();
				softButton.setText(SoftButtonName.get(i));
				softButton.setSoftButtonID(SoftButtonId.get(i));
				softButton.setType(SoftButtonType.SBT_TEXT);
				softButton.setSystemAction(SystemAction.DEFAULT_ACTION);
				vsoftButton.add(softButton);

			}
			// Send Show RPC:
			proxy.show("", "", "", "", null, vsoftButton, null, null,
					autoIncCorrId++);
		} catch (SyncException e) {

		}

	}

	/**
	 * Will be called when Groups is clicked, Has options like Send add friend
	 * and Back Groups softbutton ids = 121-130
	 * */
	private void showSoftButtonWhenNewMessageArrives() {
		ArrayList<String> SoftButtonName = new ArrayList<String>();
		SoftButtonName.add("Reply");
		SoftButtonName.add("Back");
		ArrayList<Integer> SoftButtonId = new ArrayList<Integer>();
		SoftButtonId.add(111);
		SoftButtonId.add(112);

		Vector<SoftButton> vsoftButton = new Vector<SoftButton>();
		SoftButton softButton;
		try {
			for (int i = 0; i < SoftButtonName.size(); i++) {
				softButton = new SoftButton();
				softButton.setText(SoftButtonName.get(i));
				softButton.setSoftButtonID(SoftButtonId.get(i));
				softButton.setType(SoftButtonType.SBT_TEXT);
				softButton.setSystemAction(SystemAction.DEFAULT_ACTION);
				vsoftButton.add(softButton);

			}
			// Send Show RPC:
			proxy.show("", "", "", "", null, vsoftButton, null, null,
					autoIncCorrId++);
		} catch (SyncException e) {

		}

	}

	/**
	 * Will be called when Groups is clicked, Has options like Send add friend
	 * and Back Groups softbutton ids = 121-130
	 * */
	private void showSoftButtonsForGroups() {

		isConversationSelected = true;
		// Add Soft button name
		ArrayList<String> SoftButtonName = new ArrayList<String>();
		SoftButtonName.add("Send");
		SoftButtonName.add("Back");
		SoftButtonName.add("Add Friend");

		// Add Soft buttonID
		ArrayList<Integer> SoftButtonId = new ArrayList<Integer>();
		SoftButtonId.add(121);
		SoftButtonId.add(112);
		SoftButtonId.add(131);

		Vector<SoftButton> vsoftButton = new Vector<SoftButton>();
		SoftButton softButton;
		try {
			for (int i = 0; i < SoftButtonName.size(); i++) {
				softButton = new SoftButton();
				softButton.setText(SoftButtonName.get(i));
				softButton.setSoftButtonID(SoftButtonId.get(i));
				softButton.setType(SoftButtonType.SBT_TEXT);
				softButton.setSystemAction(SystemAction.DEFAULT_ACTION);
				vsoftButton.add(softButton);

			}
			// Send Show RPC:
			proxy.show("", "", "", "", null, vsoftButton, null, null,
					autoIncCorrId++);
		} catch (SyncException e) {

		}

	}

	/**
	 * Sends Message to particular user
	 * 
	 * @param name
	 *            : Name of the Reciever
	 * @param account
	 *            : Account name
	 * @param contactJid
	 *            : Jid of the reciever
	 * */
	private void sendMessage(String name, Account con, String contactJid,
			int mode) {

		Conversation conver = new Conversation(name, con, contactJid, mode);
		ManageAccountActivity.getInstance().xmppConnectionService
				.sendMessage(new Message(conver, getResources().getString(
						R.string.message_from_tdk), Message.ENCRYPTION_NONE));
		try {
			proxy.alert("Message Sent", "", false, 2000, autoIncCorrId++);
		} catch (Exception e) {

		}

		try {
			proxy.alert("Message Sent", false, autoIncCorrId++);
		} catch (Exception e) {

		}

	}

	/**
	 * Returns conversation
	 * 
	 * */
	private Conversation getConversation(String name, Account con, String jid,
			int mode) {
		DatabaseBackend databaseBackend;
		databaseBackend = DatabaseBackend.getInstance(getApplicationContext());
		Conversation conversation = databaseBackend.findConversation(con, jid);
		if (conversation != null) {
			conversation.setStatus(Conversation.STATUS_AVAILABLE);
			conversation.setAccount(con);
			conversation.setMode(Conversation.MODE_SINGLE);

			conversation.setMessages(databaseBackend.getMessages(conversation,
					50));
			databaseBackend.updateConversation(conversation);
		} else {
			String conversationName;
			Contact contact = con.getRoster().getContact(jid);
			if (contact != null) {
				conversationName = contact.getDisplayName();
			} else {
				conversationName = jid.split("@")[0];
			}
			conversation = new Conversation(conversationName, con, jid,
					Conversation.MODE_SINGLE);
			databaseBackend.createConversation(conversation);
		}

		return conversation;

	}

	/**
	 * @author Hemant
	 * 
	 *         method which opens a recording tab and send that recording to
	 *         specified Contact
	 * 
	 **/
	private void sendRecorderMessage() {
		if (conACOUNT_NAME != null) {
			Vector<TTSChunk> initChunks3 = TTSChunkFactory
					.createSimpleTTSChunks("Speak To Record!");
			try {
				PerformAudioPassThru msg = new PerformAudioPassThru();
				msg.setInitialPrompt(initChunks3);
				msg.setAudioPassThruDisplayText1("Sync Chat ");
				msg.setAudioPassThruDisplayText2("Recording..");
				// msg.setSamplingRate(samplingRate)
				msg.setSamplingRate(SamplingRate._8KHZ);
				msg.setMaxDuration(Integer.parseInt("10000"));
				msg.setBitsPerSample(BitsPerSample._8_BIT);
				msg.setAudioType(AudioType.PCM);
				msg.setCorrelationID(autoIncCorrId++);
				msg.setMuteAudio(false);
				AudioRecorderClass.getInstance().resetClass();
				AudioRecorderClass.getInstance().latestPerformAudioPassThruMsg = msg;
				AudioRecorderClass.getInstance().mySampleRate = 8000;
				AudioRecorderClass.getInstance().myBitsPerSample = 8;
				Log.i("PerformAudioPClass", msg.toString());
				proxy.sendRPCRequest(msg);
			} catch (SyncException e) {
				e.printStackTrace();
			}

		} else {

			Vector<TTSChunk> initChunks2 = TTSChunkFactory
					.createSimpleTTSChunks("Speak To Record!");
			try {
				PerformAudioPassThru msg = new PerformAudioPassThru();
				msg.setInitialPrompt(initChunks2);
				msg.setAudioPassThruDisplayText1("Sync Chat ");
				msg.setAudioPassThruDisplayText2("Recording..");
				// msg.setSamplingRate(samplingRate)
				msg.setSamplingRate(SamplingRate._8KHZ);
				msg.setMaxDuration(Integer.parseInt("10000"));
				msg.setBitsPerSample(BitsPerSample._8_BIT);
				msg.setAudioType(AudioType.PCM);
				msg.setCorrelationID(autoIncCorrId++);
				msg.setMuteAudio(false);
				AudioRecorderClass.getInstance().resetClass();
				AudioRecorderClass.getInstance().latestPerformAudioPassThruMsg = msg;
				AudioRecorderClass.getInstance().mySampleRate = 8000;
				AudioRecorderClass.getInstance().myBitsPerSample = 8;
				Log.i("PerformAudioPClass", msg.toString());
				proxy.sendRPCRequest(msg);
			} catch (SyncException e) {
				e.printStackTrace();
			}
			// sendRecordedMessage();
		}
	}

	public void showAlert(String message) {
		try {
			proxy.alert(message, true, autoIncCorrId++);
		} catch (SyncException e) {
			e.printStackTrace();
		}
	}
}

// class LooperThread extends Thread {}
