package hsb.ess.chat.xmpp.stanzas.streammgmt;

import hsb.ess.chat.xmpp.stanzas.AbstractStanza;

public class EnablePacket extends AbstractStanza {

	public EnablePacket(int smVersion) {
		super("enable");
		this.setAttribute("xmlns","urn:xmpp:sm:"+smVersion);
		this.setAttribute("resume", "true");
	}

}
