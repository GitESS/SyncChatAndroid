package hsb.ess.chat.xmpp;

import hsb.ess.chat.entities.Contact;

public interface OnContactStatusChanged {
	public void onContactStatusChanged(Contact contact, boolean online);
}
