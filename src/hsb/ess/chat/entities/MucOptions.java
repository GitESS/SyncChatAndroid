package hsb.ess.chat.entities;

import hsb.ess.chat.crypto.PgpEngine;
import hsb.ess.chat.xml.Element;
import hsb.ess.chat.xmpp.stanzas.PresencePacket;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;

@SuppressLint("DefaultLocale")
public class MucOptions {
	public static final int ERROR_NICK_IN_USE = 1;
	
	public interface OnRenameListener {
		public void onRename(boolean success);
	}
	
	public class User {
		public static final int ROLE_MODERATOR = 3;
		public static final int ROLE_NONE = 0;
		public static final int ROLE_PARTICIPANT = 2;
		public static final int ROLE_VISITOR = 1;
		public static final int AFFILIATION_ADMIN = 4;
		public static final int AFFILIATION_OWNER = 3;
		public static final int AFFILIATION_MEMBER = 2;
		public static final int AFFILIATION_OUTCAST = 1;
		public static final int AFFILIATION_NONE = 0;
		
		private int role;
		private int affiliation;
		private String name;
		private long pgpKeyId = 0;
		
		public String getName() {
			return name;
		}
		public void setName(String user) {
			this.name = user;
		}
		
		public int getRole() {
			return this.role;
		}
		public void setRole(String role) {
			role = role.toLowerCase();
			if (role.equals("moderator")) {
				this.role = ROLE_MODERATOR;
			} else if (role.equals("participant")) {
				this.role = ROLE_PARTICIPANT;
			} else if (role.equals("visitor")) {
				this.role = ROLE_VISITOR;
			} else {
				this.role = ROLE_NONE;
			}
		}
		public int getAffiliation() {
			return this.affiliation;
		}
		public void setAffiliation(String affiliation) {
			if (affiliation.equalsIgnoreCase("admin")) {
				this.affiliation = AFFILIATION_ADMIN;
			} else if (affiliation.equalsIgnoreCase("owner")) {
				this.affiliation = AFFILIATION_OWNER;
			} else if (affiliation.equalsIgnoreCase("member")) {
				this.affiliation = AFFILIATION_MEMBER;
			} else if (affiliation.equalsIgnoreCase("outcast")) {
				this.affiliation = AFFILIATION_OUTCAST;
			} else {
				this.affiliation = AFFILIATION_NONE;
			}
		}
		public void setPgpKeyId(long id) {
			this.pgpKeyId = id;
		}
		
		public long getPgpKeyId() {
			return this.pgpKeyId;
		}
	}
	private Account account;
	private ArrayList<User> users = new ArrayList<User>();
	private Conversation conversation;
	private boolean isOnline = false;
	private int error = 0;
	private OnRenameListener renameListener = null;
	private boolean aboutToRename = false;
	private User self = new User();
	private String subject = null;

	public MucOptions(Account account) {
		this.account = account;
	}
	
	public void deleteUser(String name) {
		for(int i = 0; i < users.size(); ++i) {
			if (users.get(i).getName().equals(name)) {
				users.remove(i);
				return;
			}
		}
	}
	
	public void addUser(User user) {
		for(int i = 0; i < users.size(); ++i) {
			if (users.get(i).getName().equals(user.getName())) {
				users.set(i, user);
				return;
			}
		}
		users.add(user);
		}
	
	public void processPacket(PresencePacket packet, PgpEngine pgp) {
		String[] fromParts = packet.getFrom().split("/");
		if (fromParts.length>=2) {
			String name = fromParts[1];
			String type = packet.getAttribute("type");
			if (type==null) {
				User user = new User();
				Element item = packet.findChild("x","http://jabber.org/protocol/muc#user").findChild("item");
				user.setName(name);
				user.setAffiliation(item.getAttribute("affiliation"));
				user.setRole(item.getAttribute("role"));
				user.setName(name);
				if (name.equals(getNick())) {
					this.isOnline = true;
					this.error = 0;
					self = user;
				} else {
					addUser(user);
				}
				if (pgp != null) {
					Element x = packet.findChild("x",
							"jabber:x:signed");
					if (x != null) {
						Element status = packet.findChild("status");
						String msg;
						if (status != null) {
							msg = status.getContent();
						} else {
							msg = "";
						}
						user.setPgpKeyId(pgp.fetchKeyId(account,msg, x.getContent()));
					}
				}
			} else if (type.equals("unavailable")) {
				if (name.equals(getNick())) {
					Element item = packet.findChild("x","http://jabber.org/protocol/muc#user").findChild("item");
					String nick = item.getAttribute("nick");
					if (nick!=null) {
						aboutToRename = false;
						if (renameListener!=null) {
							renameListener.onRename(true);
						}
						this.setNick(nick);
					}
				}
				deleteUser(packet.getAttribute("from").split("/")[1]);
			} else if (type.equals("error")) {
				Element error = packet.findChild("error");
				if (error.hasChild("conflict")) {
					if (aboutToRename) {
						if (renameListener!=null) {
							renameListener.onRename(false);
						}
						aboutToRename = false;
					} else {
						this.error  = ERROR_NICK_IN_USE;
					}
				}
			}
		}
	}
	
	public List<User> getUsers() {
		return this.users;
	}
	
	public String getNick() {
		String[] split = conversation.getContactJid().split("/");
		if (split.length == 2) {
			return split[1];
		} else {
			if (conversation.getAccount()!=null) {
				return conversation.getAccount().getUsername();
			} else {
				return null;
			}
		}
	}
	
	public void setNick(String nick) {
		String jid = conversation.getContactJid().split("/")[0]+"/"+nick;
		conversation.setContactJid(jid);
	}
	
	public void setConversation(Conversation conversation) {
		this.conversation = conversation;
	}
	
	public boolean online() {
		return this.isOnline;
	}
	
	public int getError() {
		return this.error;
	}

	public void setOnRenameListener(OnRenameListener listener) {
		this.renameListener = listener;
	}
	
	public OnRenameListener getOnRenameListener() {
		return this.renameListener;
	}

	public void setOffline() {
		this.users.clear();
		this.error = 0;
		this.isOnline = false;
	}

	public User getSelf() {
		return self;
	}

	public void setSubject(String content) {
		this.subject = content;
	}
	
	public String getSubject() {
		return this.subject;
	}

	public void flagAboutToRename() {
		this.aboutToRename = true;
	}
	
	public long[] getPgpKeyIds() {
		List<Long> ids = new ArrayList<Long>();
		for(User user : getUsers()) {
			if(user.getPgpKeyId()!=0) {
				ids.add(user.getPgpKeyId());
			}
		}
		long[] primitivLongArray = new long[ids.size()];
		for(int i = 0; i < ids.size(); ++i) {
			primitivLongArray[i] = ids.get(i);
		}
		return primitivLongArray;
	}
	
	public boolean pgpKeysInUse() {
		for(User user : getUsers()) {
			if (user.getPgpKeyId()!=0) {
				return true;
			}
		}
		return false;
	}
	
	public boolean everybodyHasKeys() {
		for(User user : getUsers()) {
			if (user.getPgpKeyId()==0) {
				return false;
			}
		}
		return true;
	}
}