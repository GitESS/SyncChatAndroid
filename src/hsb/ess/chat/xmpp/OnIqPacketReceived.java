package hsb.ess.chat.xmpp;

import hsb.ess.chat.entities.Account;
import hsb.ess.chat.xmpp.stanzas.IqPacket;

public interface OnIqPacketReceived extends PacketReceived {
	public void onIqPacketReceived(Account account, IqPacket packet);
}
