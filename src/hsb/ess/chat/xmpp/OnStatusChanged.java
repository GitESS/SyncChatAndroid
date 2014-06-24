package hsb.ess.chat.xmpp;

import hsb.ess.chat.entities.Account;

public interface OnStatusChanged {
	public void onStatusChanged(Account account);
}
