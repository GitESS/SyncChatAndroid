package hsb.ess.chat.xmpp.stanzas.streammgmt;

import hsb.ess.chat.xmpp.stanzas.AbstractStanza;

public class AckPacket extends AbstractStanza {

	public AckPacket(int sequence, int smVersion) {
		super("a");
		this.setAttribute("xmlns","urn:xmpp:sm:"+smVersion);
		this.setAttribute("h", ""+sequence);
	}

}
