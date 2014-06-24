package hsb.ess.chat.parser;

import hsb.ess.chat.entities.Account;
import hsb.ess.chat.entities.Contact;
import hsb.ess.chat.services.XmppConnectionService;
import hsb.ess.chat.xml.Element;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


public abstract class AbstractParser {
	
	protected XmppConnectionService mXmppConnectionService;

	protected AbstractParser(XmppConnectionService service) {
		this.mXmppConnectionService = service;
	}
	
	protected long getTimestamp(Element packet) {
		if (packet.hasChild("delay")) {
			try {
				String stamp = packet.findChild("delay").getAttribute(
						"stamp");
				stamp = stamp.replace("Z", "+0000");
				Date date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
						.parse(stamp);
				return date.getTime();
			} catch (ParseException e) {
				return System.currentTimeMillis();
			}
		} else {
			return System.currentTimeMillis();
		}
	}
	
	protected void updateLastseen(Element packet, Account account, boolean presenceOverwrite) {
		String[] fromParts = packet.getAttribute("from").split("/");
		String from = fromParts[0];
		String presence = null;
		if (fromParts.length >= 2) {
			presence = fromParts[1];
		}
		Contact contact = account.getRoster().getContact(from);
		long timestamp = getTimestamp(packet);
		if (timestamp >= contact.lastseen.time) {
			contact.lastseen.time = timestamp;
			if ((presence!=null)&&(presenceOverwrite)) {
				contact.lastseen.presence = presence;
			}
		}
	}
}
