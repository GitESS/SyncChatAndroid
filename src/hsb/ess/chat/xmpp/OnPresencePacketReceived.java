package hsb.ess.chat.xmpp;

import hsb.ess.chat.entities.Account;
import hsb.ess.chat.xmpp.stanzas.PresencePacket;

public interface OnPresencePacketReceived extends PacketReceived {
	public void onPresencePacketReceived(Account account, PresencePacket packet);
}
