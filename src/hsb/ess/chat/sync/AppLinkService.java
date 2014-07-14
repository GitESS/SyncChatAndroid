/**Ford Motor Company
 * September 2012
 * Elizabeth Halash
 */

package hsb.ess.chat.sync;

import hsb.ess.chat.entities.Account;
import hsb.ess.chat.entities.Contact;
import hsb.ess.chat.entities.Conversation;
import hsb.ess.chat.entities.Message;
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
import com.ford.syncV4.proxy.rpc.SpeakResponse;
import com.ford.syncV4.proxy.rpc.SubscribeButtonResponse;
import com.ford.syncV4.proxy.rpc.SubscribeVehicleDataResponse;
import com.ford.syncV4.proxy.rpc.TTSChunk;
import com.ford.syncV4.proxy.rpc.UnsubscribeButtonResponse;
import com.ford.syncV4.proxy.rpc.UnsubscribeVehicleDataResponse;
import com.ford.syncV4.proxy.rpc.enums.ButtonName;
import com.ford.syncV4.proxy.rpc.enums.DriverDistractionState;
import com.ford.syncV4.proxy.rpc.enums.InteractionMode;
import com.ford.syncV4.proxy.rpc.enums.SpeechCapabilities;
import com.ford.syncV4.proxy.rpc.enums.TextAlignment;
import com.ford.syncV4.util.DebugTool;

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

	private boolean isFriendIsClicked = false;
	Account account;

	List<Contact> contactsList, groupsList;
	private static final int CHOICE_APPLE = 2013;
	private static final int CHOICE_PEAR = 2014;
	private static final int CHOICE_PEACH = 2015;
	private static final int CHOICE_FRIENDS = 2016;
	private static final int ONLINE_FRIENDS_CMDID = 1001;

	private List<Integer> ONLINE_FRIENDS_ID;
	private static int CHOICE_FRIENDS_ID = 2004;

	private static final int CHOICE_GROUPS = 2017;
	private static final int ONLINE_GROUPS_CMDID = 1002;
	private List<Integer> ONLINE_GROUP_ID;
	private static int CHOICE_GROUPS_ID = 5001;

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

				initializeVoiceCommand();
				performInterac();
				if (ConversationFragment.getInstance() != null) {
					setCurrentActivity(ConversationFragment.getInstance());
				}

				Log.i("SyncService",
						"If online?" + manageA.isMyAccountIsOnline());

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

			} else {
				try {
					proxy.show("Sync Chat", "Application",
							TextAlignment.CENTERED, autoIncCorrId++);
				} catch (SyncException e) {
					DebugTool.logError("Failed to send Show", e);
				}
			}

			Log.i("hemant",
					"Conversation Screen is "
							+ ConversationFragment.getInstance()
							+ " Is my account is online? "
							+ manageA.isMyAccountIsOnline());
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
			break;
		default:
			return;
		}
	}

	public void showLockScreen() {
		// only throw up lockscreen if main activity is currently on top
		// else, wait until onResume() to throw lockscreen so it doesn't
		// pop-up while a user is using another app on the phone
		// if (currentUIActivity != null) {
		// if (currentUIActivity.isActivityonTop() == true) {
		// if (LockScreenActivity.getInstance() == null) {
		// Intent i = new Intent(this, LockScreenActivity.class);
		// i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		// i.addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION);
		// startActivity(i);
		// }
		// }
		// }
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
		// Log.i(TAG, "dd: " + notification.getStringState());
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
		// TODO Auto-generated method stub
	}

	@Override
	public void onGenericResponse(GenericResponse response) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onOnCommand(OnCommand notification) {

		// for (int i = 0; i < contactsList.size(); i++) {
		// int ContactID = ONLINE_FRIENDS_ID.get(i);
		//
		// if (notification.getCmdID() == ContactID) {
		// Log.d("service", "In ononCommand and clicked item is " + i);
		// performInteraction();
		// }

		switch (notification.getCmdID()) {
		case ONLINE_FRIENDS_CMDID: // for Choice set
			performInteraction();
			isFriendIsClicked = true;
			break;

		case ONLINE_GROUPS_CMDID:
			performInteractionGroup();
			isFriendIsClicked = false;
			break;

		default:
			break;

		}
		Log.i("ApplinkServer", "Is Friend Clicked"+isFriendIsClicked);
		// }

	}

	@Override
	public void onAddCommandResponse(AddCommandResponse response) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onAddSubMenuResponse(AddSubMenuResponse response) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onCreateInteractionChoiceSetResponse(
			CreateInteractionChoiceSetResponse response) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onAlertResponse(AlertResponse response) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onDeleteCommandResponse(DeleteCommandResponse response) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onDeleteInteractionChoiceSetResponse(
			DeleteInteractionChoiceSetResponse response) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onDeleteSubMenuResponse(DeleteSubMenuResponse response) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onPerformInteractionResponse(PerformInteractionResponse response) {

		Log.i("ApplinkService", "Response" + response);
		Log.i("ApplinkService", "Response id" + response.getChoiceID());
		Log.i("ApplinkServer", "Is Friend Clicked Response "+isFriendIsClicked);
		if (isFriendIsClicked) {
			for (int i = 0; i < contactsList.size(); i++) {
				int ContactID = ONLINE_FRIENDS_ID.get(i);

				if (response.getChoiceID() == ContactID) {
					Log.d("service",
							"In PerformInteractionResponse and clicked item is "
									+ i);

					Account con = contactsList.get(i).getAccount();
					String name = contactsList.get(i).getDisplayName();
					String contactJid = contactsList.get(i).getJid();

					Conversation conver = new Conversation(name, con,
							contactJid, Conversation.MODE_SINGLE);

					// ConversationFragment.getInstance().sendMessage(
					// new Message(conver, "Send From Sync",
					// Message.ENCRYPTION_NONE));

					ManageAccountActivity.getInstance().xmppConnectionService
							.sendMessage(new Message(conver, "Send From Sync",
									Message.ENCRYPTION_NONE));
					// performInteraction();
					return;
				}
			}
		} else if (!isFriendIsClicked) {
			for (int i = 0; i < groupsList.size(); i++) {
				int ContactID = ONLINE_GROUP_ID.get(i);

				if (response.getChoiceID() == ContactID) {
					Log.d("service", "In ononCommand and clicked item is " + i);

					Account con = groupsList.get(i).getAccount();
					String name = groupsList.get(i).getDisplayName();
					String contactJid = groupsList.get(i).getJid();

					Conversation conver = new Conversation(name, con,
							contactJid, Conversation.MODE_MULTI);
					ManageAccountActivity.getInstance().xmppConnectionService
							.sendMessage(new Message(conver, "Send From Sync",
									Message.ENCRYPTION_NONE));
				}
			}
		}
		// switch (response.getChoiceID()) {
		// case CHOICE_FRUIT:
		// Log.e(TAG, "onPerformInteractionResponse fruit clicked ");
		// // sendPickedChoice("fruit");
		// break;
		// case CHOICE_APPLE:
		// Log.e(TAG, "onPerformInteractionResponse Apple clicked ");
		// // sendPickedChoice("apple");
		// break;
		// case CHOICE_PEAR:
		// Log.e(TAG, "onPerformInteractionResponse pear clicked ");
		// // sendPickedChoice("pear");
		// break;
		// case CHOICE_PEACH:
		// Log.e(TAG, "onPerformInteractionResponse  peach clicked ");
		// // Log.e(TAG, "onPerformInteractionResponse fruit clicked ");
		//
		// // sendPickedChoice("peach");
		// break;
		// }

		
		SetAlert("Message Sent", 3000,
				"Message Sent");
	}

	@Override
	public void onResetGlobalPropertiesResponse(
			ResetGlobalPropertiesResponse response) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onSetGlobalPropertiesResponse(
			SetGlobalPropertiesResponse response) {
	}

	@Override
	public void onSetMediaClockTimerResponse(SetMediaClockTimerResponse response) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onShowResponse(ShowResponse response) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onSpeakResponse(SpeakResponse response) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onOnButtonEvent(OnButtonEvent notification) {

		if (notification.getButtonName() == ButtonName.PRESET_2) {

			try {
				proxy.speak(" 2 is clicked", autoIncCorrId++);
			} catch (SyncException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (notification.getButtonName() == ButtonName.PRESET_8) {

			// ConversationFragment.getInstance().letActivityFireACommand();

		}
		// TODO Auto-generated method stub
	}

	@Override
	public void onOnButtonPress(OnButtonPress notification) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onSubscribeButtonResponse(SubscribeButtonResponse response) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onUnsubscribeButtonResponse(UnsubscribeButtonResponse response) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onOnPermissionsChange(OnPermissionsChange notification) {
		// TODO Auto-generated method stub
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onSubscribeVehicleDataResponse(
			SubscribeVehicleDataResponse response) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onUnsubscribeVehicleDataResponse(
			UnsubscribeVehicleDataResponse response) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onGetVehicleDataResponse(GetVehicleDataResponse response) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onReadDIDResponse(ReadDIDResponse response) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onGetDTCsResponse(GetDTCsResponse response) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onOnVehicleData(OnVehicleData notification) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPerformAudioPassThruResponse(
			PerformAudioPassThruResponse response) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onEndAudioPassThruResponse(EndAudioPassThruResponse response) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onOnAudioPassThru(OnAudioPassThru notification) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPutFileResponse(PutFileResponse response) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onDeleteFileResponse(DeleteFileResponse response) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onListFilesResponse(ListFilesResponse response) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSetAppIconResponse(SetAppIconResponse response) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onScrollableMessageResponse(ScrollableMessageResponse response) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onChangeRegistrationResponse(ChangeRegistrationResponse response) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSetDisplayLayoutResponse(SetDisplayLayoutResponse response) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onOnLanguageChange(OnLanguageChange notification) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSliderResponse(SliderResponse response) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onDialNumberResponse(DialNumberResponse response) {
		// TODO Auto-generated method stub

	}

	public void SpeakOutNow(String message) {

		try {
			proxy.speak(" " + message, autoIncCorrId++);
		} catch (SyncException e) {
			// TODO Auto-generated catch block
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

	public void performInterac() {
		Log.i("ApplinkService", "In PerformInterac");

		Vector<Choice> commands = new Vector<Choice>();
		Vector<Choice> commands2 = new Vector<Choice>();

		List<Contact> tempgroupList, tempcontactList, tempRosterContacts;
		tempgroupList = new ArrayList<Contact>();
		tempcontactList = new ArrayList<Contact>();

		tempRosterContacts = new ArrayList<Contact>();
		contactsList = new ArrayList<Contact>();
		groupsList = new ArrayList<Contact>();
		List<Account> accountList = new ArrayList<Account>();
		accountList
				.addAll(ConversationActivity.getInstance().xmppConnectionService
						.getAccounts());
		tempRosterContacts = accountList.get(0).getRoster().getContacts();

		for (int i = 0; i < tempRosterContacts.size(); i++) {
			if (tempRosterContacts.get(i).couldBeMuc()) {
				tempgroupList.add(tempRosterContacts.get(i));
			} else {
				tempcontactList.add(tempRosterContacts.get(i));
			}
			Log.i("Hemant", "ContactList :" + tempcontactList.size()
					+ " groupList : " + tempgroupList.size());
		}

		contactsList.addAll(tempcontactList);
		groupsList.addAll(tempgroupList);

		if (contactsList.size() != 0) {
			Log.i("service", "Contact List" + contactsList.size());
		}

		ONLINE_FRIENDS_ID = new ArrayList<Integer>();
		for (int i = 0; i < contactsList.size(); i++) {
			ONLINE_FRIENDS_ID.add(CHOICE_FRIENDS_ID + i);
			Contact name = contactsList.get(i);
			String contactName = name.getDisplayName();
			Choice one = new Choice();
			one.setChoiceID(CHOICE_FRIENDS_ID + i);
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
	public void SetAlert(String name, int time, String msg){
		Alert alert = new Alert();
		alert.setAlertText1(name);
		alert.setDuration(time);
		alert.setCorrelationID(autoIncCorrId++);
		Vector<TTSChunk> ttsChunks = new Vector<TTSChunk>();
		ttsChunks.add(TTSChunkFactory.createChunk(SpeechCapabilities.TEXT,
				msg));
		alert.setTtsChunks(ttsChunks);
	try{
			proxy.sendRPCRequest(alert);
		
	}catch(SyncException e){
		
	}
	
}
}
