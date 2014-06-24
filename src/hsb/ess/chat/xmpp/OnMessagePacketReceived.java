package hsb.ess.chat.xmpp;

import hsb.ess.chat.entities.Account;
import hsb.ess.chat.xmpp.stanzas.MessagePacket;

public interface OnMessagePacketReceived extends PacketReceived {
	public void onMessagePacketReceived(Account account, MessagePacket packet);
}
