package hsb.ess.chat.xmpp.stanzas.streammgmt;

import hsb.ess.chat.xmpp.stanzas.AbstractStanza;

public class RequestPacket extends AbstractStanza {

	public RequestPacket(int smVersion) {
		super("r");
		this.setAttribute("xmlns","urn:xmpp:sm:"+smVersion);
	}

}
