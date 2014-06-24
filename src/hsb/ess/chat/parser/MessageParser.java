package hsb.ess.chat.parser;

import hsb.ess.chat.entities.Account;
import hsb.ess.chat.entities.Conversation;
import hsb.ess.chat.entities.Message;
import hsb.ess.chat.services.XmppConnectionService;
import hsb.ess.chat.xml.Element;
import hsb.ess.chat.xmpp.stanzas.MessagePacket;
import net.java.otr4j.session.Session;
import net.java.otr4j.session.SessionStatus;

public class MessageParser extends AbstractParser {

	public MessageParser(XmppConnectionService service) {
		super(service);
	}

	public Message parseChat(MessagePacket packet, Account account) {
		String[] fromParts = packet.getFrom().split("/");
		Conversation conversation = mXmppConnectionService
				.findOrCreateConversation(account, fromParts[0], false);
		conversation.setLatestMarkableMessageId(getMarkableMessageId(packet));
		updateLastseen(packet, account,true);
		String pgpBody = getPgpBody(packet);
		if (pgpBody != null) {
			return new Message(conversation, packet.getFrom(), pgpBody,
					Message.ENCRYPTION_PGP, Message.STATUS_RECIEVED);
		} else {
			return new Message(conversation, packet.getFrom(),
					packet.getBody(), Message.ENCRYPTION_NONE,
					Message.STATUS_RECIEVED);
		}
	}

	public Message parseOtrChat(MessagePacket packet, Account account) {
		boolean properlyAddressed = (packet.getTo().split("/").length == 2)
				|| (account.countPresences() == 1);
		String[] fromParts = packet.getFrom().split("/");
		Conversation conversation = mXmppConnectionService
				.findOrCreateConversation(account, fromParts[0], false);
		updateLastseen(packet, account,true);
		String body = packet.getBody();
		if (!conversation.hasValidOtrSession()) {
			if (properlyAddressed) {
				conversation.startOtrSession(
						mXmppConnectionService.getApplicationContext(),
						fromParts[1], false);
			} else {
				return null;
			}
		} else {
			String foreignPresence = conversation.getOtrSession()
					.getSessionID().getUserID();
			if (!foreignPresence.equals(fromParts[1])) {
				conversation.resetOtrSession();
				if (properlyAddressed) {
					conversation.startOtrSession(
							mXmppConnectionService.getApplicationContext(),
							fromParts[1], false);
				} else {
					return null;
				}
			}
		}
		try {
			Session otrSession = conversation.getOtrSession();
			SessionStatus before = otrSession.getSessionStatus();
			body = otrSession.transformReceiving(body);
			SessionStatus after = otrSession.getSessionStatus();
			if ((before != after) && (after == SessionStatus.ENCRYPTED)) {
				mXmppConnectionService.onOtrSessionEstablished(conversation);
			} else if ((before != after) && (after == SessionStatus.FINISHED)) {
				conversation.resetOtrSession();
			}
			// isEmpty is a work around for some weird clients which send emtpty
			// strings over otr
			if ((body == null) || (body.isEmpty())) {
				return null;
			}
			conversation.setLatestMarkableMessageId(getMarkableMessageId(packet));
			Message finishedMessage = new Message(conversation, packet.getFrom(), body,
					Message.ENCRYPTION_OTR, Message.STATUS_RECIEVED);
			finishedMessage.setTime(getTimestamp(packet));
			return finishedMessage;
		} catch (Exception e) {
			conversation.resetOtrSession();
			return null;
		}
	}

	public Message parseGroupchat(MessagePacket packet, Account account) {
		int status;
		String[] fromParts = packet.getFrom().split("/");
		Conversation conversation = mXmppConnectionService
				.findOrCreateConversation(account, fromParts[0], true);
		if (packet.hasChild("subject")) {
			conversation.getMucOptions().setSubject(
					packet.findChild("subject").getContent());
			mXmppConnectionService.updateUi(conversation, false);
			return null;
		}
		if ((fromParts.length == 1)) {
			return null;
		}
		String counterPart = fromParts[1];
		if (counterPart.equals(conversation.getMucOptions().getNick())) {
			if (mXmppConnectionService.markMessage(conversation,
					packet.getId(), Message.STATUS_SEND)) {
				return null;
			} else {
				status = Message.STATUS_SEND;
			}
		} else {
			status = Message.STATUS_RECIEVED;
		}
		String pgpBody = getPgpBody(packet);
		conversation.setLatestMarkableMessageId(getMarkableMessageId(packet));
		Message finishedMessage;
		if (pgpBody == null) {
			finishedMessage = new Message(conversation, counterPart, packet.getBody(),
					Message.ENCRYPTION_NONE, status);
		} else {
			finishedMessage=  new Message(conversation, counterPart, pgpBody,
					Message.ENCRYPTION_PGP, status);
		}
		finishedMessage.setTime(getTimestamp(packet));
		return finishedMessage;
	}

	public Message parseCarbonMessage(MessagePacket packet, Account account) {
		int status;
		String fullJid;
		Element forwarded;
		if (packet.hasChild("received")) {
			forwarded = packet.findChild("received").findChild("forwarded");
			status = Message.STATUS_RECIEVED;
		} else if (packet.hasChild("sent")) {
			forwarded = packet.findChild("sent").findChild("forwarded");
			status = Message.STATUS_SEND;
		} else {
			return null;
		}
		if (forwarded == null) {
			return null;
		}
		Element message = forwarded.findChild("message");
		if ((message == null) || (!message.hasChild("body")))
			return null; // either malformed or boring
		if (status == Message.STATUS_RECIEVED) {
			fullJid = message.getAttribute("from");
			updateLastseen(message, account,true);
		} else {
			fullJid = message.getAttribute("to");
		}
		String[] parts = fullJid.split("/");
		Conversation conversation = mXmppConnectionService
				.findOrCreateConversation(account, parts[0], false);
		conversation.setLatestMarkableMessageId(getMarkableMessageId(packet));
		String pgpBody = getPgpBody(message);
		Message finishedMessage;
		if (pgpBody != null) {
			finishedMessage = new Message(conversation, fullJid, pgpBody,Message.ENCRYPTION_PGP, status);
		} else {
			String body = message.findChild("body").getContent();
			finishedMessage=  new Message(conversation, fullJid, body,Message.ENCRYPTION_NONE, status);
		}
		finishedMessage.setTime(getTimestamp(message));
		return finishedMessage;
	}

	public void parseError(MessagePacket packet, Account account) {
		String[] fromParts = packet.getFrom().split("/");
		mXmppConnectionService.markMessage(account, fromParts[0],
				packet.getId(), Message.STATUS_SEND_FAILED);
	}
	
	public void parseNormal(MessagePacket packet, Account account) {
		if (packet.hasChild("displayed","urn:xmpp:chat-markers:0")) {
			String id = packet.findChild("displayed","urn:xmpp:chat-markers:0").getAttribute("id");
			String[] fromParts = packet.getFrom().split("/");
			updateLastseen(packet, account,true);
			mXmppConnectionService.markMessage(account,fromParts[0], id, Message.STATUS_SEND_DISPLAYED);
		} else if (packet.hasChild("received","urn:xmpp:chat-markers:0")) {
			String id = packet.findChild("received","urn:xmpp:chat-markers:0").getAttribute("id");
			String[] fromParts = packet.getFrom().split("/");
			updateLastseen(packet, account,false);
			mXmppConnectionService.markMessage(account,fromParts[0], id, Message.STATUS_SEND_RECEIVED);
		} else if (packet.hasChild("x")) {
			Element x = packet.findChild("x");
			if (x.hasChild("invite")) {
				Conversation conversation = mXmppConnectionService.findOrCreateConversation(account, packet.getFrom(),
						true);
				mXmppConnectionService.updateUi(conversation, false);
			}

		}
	}

	private String getPgpBody(Element message) {
		Element child = message.findChild("x", "jabber:x:encrypted");
		if (child == null) {
			return null;
		} else {
			return child.getContent();
		}
	}
	
	private String getMarkableMessageId(Element message) {
		if (message.hasChild("markable", "urn:xmpp:chat-markers:0")) {
			return message.getAttribute("id");
		} else {
			return null;
		}
	}

	
}
