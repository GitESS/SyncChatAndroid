package hsb.ess.chat.xmpp.jingle.stanzas;

import hsb.ess.chat.xml.Element;

public class Reason extends Element {
	private Reason(String name) {
		super(name);
	}
	
	public Reason() {
		super("reason");
	}
}
