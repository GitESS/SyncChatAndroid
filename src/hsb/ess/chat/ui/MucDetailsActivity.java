package hsb.ess.chat.ui;

import hsb.ess.chat.R;
import hsb.ess.chat.crypto.PgpEngine;
import hsb.ess.chat.entities.Conversation;
import hsb.ess.chat.entities.MucOptions;
import hsb.ess.chat.entities.MucOptions.User;
import hsb.ess.chat.utils.UIHelper;

import java.util.ArrayList;
import java.util.List;

import org.openintents.openpgp.util.OpenPgpUtils;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MucDetailsActivity extends XmppActivity {
	public static final String ACTION_VIEW_MUC = "view_muc";
	private Conversation conversation;
	private EditText mYourNick;
	private EditText mSubject;
	private TextView mRoleAffiliaton;
	private TextView mFullJid;
	private LinearLayout membersView;
	private LinearLayout mMoreDetails;
	private Button mInviteButton;
	private String uuid = null;
	private OnClickListener changeNickListener = new OnClickListener() {

		@Override
		public void onClick(View arg0) {
			MucOptions options = conversation.getMucOptions();
			String nick = mYourNick.getText().toString();
			if (!options.getNick().equals(nick)) {
				xmppConnectionService.renameInMuc(conversation, nick);
				finish();
			}
		}
	};
	
	private OnClickListener changeSubjectListener = new OnClickListener() {

		@Override
		public void onClick(View arg0) {
			String subject = mSubject.getText().toString();
			MucOptions options = conversation.getMucOptions();
			if (!subject.equals(options.getSubject())) {
				xmppConnectionService.sendConversationSubject(conversation,subject);
				finish();
			}
		}
	};
	
	private OnClickListener inviteListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			Intent intent = new Intent(getApplicationContext(),
					ContactsActivity.class);
			intent.setAction("invite");
			intent.putExtra("uuid",conversation.getUuid());
			startActivity(intent);
		}
	};
	
	private List<User> users = new ArrayList<MucOptions.User>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_muc_details);
		mYourNick = (EditText) findViewById(R.id.muc_your_nick);
		mFullJid = (TextView) findViewById(R.id.muc_jabberid);
		ImageButton editNickButton = (ImageButton) findViewById(R.id.muc_edit_nick);
		editNickButton.setOnClickListener(this.changeNickListener);
		ImageButton editSubjectButton = (ImageButton) findViewById(R.id.muc_edit_subject);
		editSubjectButton.setOnClickListener(this.changeSubjectListener);
		membersView = (LinearLayout) findViewById(R.id.muc_members);
		mMoreDetails = (LinearLayout) findViewById(R.id.muc_more_details);
		mMoreDetails.setVisibility(View.GONE);
		mSubject = (EditText) findViewById(R.id.muc_subject);
		mInviteButton = (Button) findViewById(R.id.invite);
		mInviteButton.setOnClickListener(inviteListener);
		getActionBar().setHomeButtonEnabled(true);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem) {
	    switch (menuItem.getItemId()) {
	    case android.R.id.home:
	      finish();
	    }
	    return super.onOptionsItemSelected(menuItem);
	}

	public String getReadableRole(int role) {
		switch (role) {
		case User.ROLE_MODERATOR:
			return getString(R.string.moderator);
		case User.ROLE_PARTICIPANT:
			return getString(R.string.participant);
		case User.ROLE_VISITOR:
			return getString(R.string.visitor);
		default:
			return "";
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.muc_details, menu);
		return true;
	}

	@Override
	void onBackendConnected() {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		boolean useSubject = preferences.getBoolean("use_subject_in_muc", true);
		if (getIntent().getAction().equals(ACTION_VIEW_MUC)) {
			this.uuid = getIntent().getExtras().getString("uuid");
		}
		if (uuid != null) {
			for (Conversation mConv : xmppConnectionService.getConversations()) {
				if (mConv.getUuid().equals(uuid)) {
					this.conversation = mConv;
				}
			}
			if (this.conversation != null) {
				mSubject.setText(conversation.getMucOptions().getSubject());
				setTitle(conversation.getName(useSubject));
				mFullJid.setText(conversation.getContactJid().split("/")[0]);
				mYourNick.setText(conversation.getMucOptions().getNick());
				mRoleAffiliaton = (TextView) findViewById(R.id.muc_role);
				if (conversation.getMucOptions().online()) {
					mMoreDetails.setVisibility(View.VISIBLE);
					User self = conversation.getMucOptions().getSelf();
					switch (self.getAffiliation()) {
					case User.AFFILIATION_ADMIN:
						mRoleAffiliaton.setText(getReadableRole(self.getRole())
								+ " (" + getString(R.string.admin) + ")");
						break;
					case User.AFFILIATION_OWNER:
						mRoleAffiliaton.setText(getReadableRole(self.getRole())
								+ " (" + getString(R.string.owner) + ")");
						break;
					default:
						mRoleAffiliaton
								.setText(getReadableRole(self.getRole()));
						break;
					}
				}
				this.users.clear();
				this.users.addAll(conversation.getMucOptions().getUsers());
				LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				membersView.removeAllViews();
				for(final User contact : conversation.getMucOptions().getUsers()) {
					View view = (View) inflater.inflate(R.layout.contact, null);
					TextView displayName = (TextView) view.findViewById(R.id.contact_display_name);
					TextView key = (TextView) view.findViewById(R.id.key);
					displayName.setText(contact.getName());
					TextView role = (TextView) view.findViewById(R.id.contact_jid);
					role.setText(getReadableRole(contact.getRole()));
					if (contact.getPgpKeyId()!=0) {
						key.setVisibility(View.VISIBLE);
						key.setOnClickListener(new OnClickListener() {
							
							@Override
							public void onClick(View v) {
								PgpEngine pgp = xmppConnectionService.getPgpEngine();
								if (pgp!=null) {
									PendingIntent intent = pgp.getIntentForKey(conversation.getAccount(), contact.getPgpKeyId());
									if (intent!=null) {
										try {
											startIntentSenderForResult(intent.getIntentSender(), 0, null, 0, 0, 0);
										} catch (SendIntentException e) {
											
										}
									}
								}
							}
						});
						key.setText(OpenPgpUtils.convertKeyIdToHex(contact.getPgpKeyId()));
					}
					ImageView imageView = (ImageView) view
							.findViewById(R.id.contact_photo);
					imageView.setImageBitmap(UIHelper.getContactPicture(contact.getName(), 48,this.getApplicationContext(), false));
					membersView.addView(view);
				}
			}
		} else {
			Log.d("xmppService","uuid in muc details was null");
		}
	}
}
