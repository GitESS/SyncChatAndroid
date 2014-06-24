package hsb.ess.chat.xmpp.jingle;

import hsb.ess.chat.entities.Account;
import hsb.ess.chat.xmpp.PacketReceived;
import hsb.ess.chat.xmpp.jingle.stanzas.JinglePacket;

public interface OnJinglePacketReceived extends PacketReceived {
	public void onJinglePacketReceived(Account account, JinglePacket packet);
}
