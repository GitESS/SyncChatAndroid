package hsb.ess.chat.xmpp;

import hsb.ess.chat.entities.Account;

public interface OnTLSExceptionReceived {
	public void onTLSExceptionReceived(String fingerprint, Account account);
}
