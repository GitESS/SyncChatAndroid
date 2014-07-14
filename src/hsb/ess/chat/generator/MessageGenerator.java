package hsb.ess.chat.generator;

import hsb.ess.chat.entities.Account;
import hsb.ess.chat.entities.Conversation;
import hsb.ess.chat.entities.Message;
import hsb.ess.chat.xml.Element;
import hsb.ess.chat.xmpp.stanzas.MessagePacket;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import net.java.otr4j.OtrException;
import net.java.otr4j.session.Session;

public class MessageGenerator {
	private MessagePacket preparePacket(Message message, boolean addDelay) {
		Conversation conversation = message.getConversation();
		Account account = conversation.getAccount();
		MessagePacket packet = new MessagePacket();
		if (conversation.getMode() == Conversation.MODE_SINGLE) {
			packet.setTo(message.getCounterpart());
			packet.setType(MessagePacket.TYPE_CHAT);
			packet.addChild("markable", "urn:xmpp:chat-markers:0");
		} else {
			packet.setTo(message.getCounterpart().split("/")[0]);
			packet.setType(MessagePacket.TYPE_GROUPCHAT);
		}
		packet.setFrom(account.getFullJid());
		packet.setId(message.getUuid());
		if (addDelay) {
			addDelay(packet, message.getTimeSent());
		}
		return packet;
	}

	private void addDelay(MessagePacket packet, long timestamp) {
		final SimpleDateFormat mDateFormat = new SimpleDateFormat(
				"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
		mDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		Element delay = packet.addChild("delay", "urn:xmpp:delay");
		Date date = new Date(timestamp);
		delay.setAttribute("stamp", mDateFormat.format(date));
	}

	public MessagePacket generateOtrChat(Message message) throws OtrException {
		return generateOtrChat(message, false);
	}

	public MessagePacket generateOtrChat(Message message, boolean addDelay) {
		Session otrSession = message.getConversation().getOtrSession();
		if (otrSession == null) {
			return null;
		}
		MessagePacket packet = preparePacket(message, addDelay);
		packet.addChild("private", "urn:xmpp:carbons:2");
		packet.addChild("no-copy", "urn:xmpp:hints");
		try {
			packet.setBody(otrSession.transformSending(message.getBody()));
			return packet;
		} catch (OtrException e) {
			return null;
		}
	}

	public MessagePacket generateChat(Message message) {
		return generateChat(message, false);
	}

	public MessagePacket generateChat(Message message, boolean addDelay) {
		MessagePacket packet = preparePacket(message, addDelay);
		packet.setBody(message.getBody());
		return packet;
	}

	public MessagePacket generatePgpChat(Message message) {
		return generatePgpChat(message, false);
	}

	public MessagePacket generatePgpChat(Message message, boolean addDelay) {
		MessagePacket packet = preparePacket(message, addDelay);
		packet.setBody("This is an XEP-0027 encryted message");
		if (message.getEncryption() == Message.ENCRYPTION_DECRYPTED) {
			packet.addChild("x", "jabber:x:encrypted").setContent(
					message.getEncryptedBody());
		} else if (message.getEncryption() == Message.ENCRYPTION_PGP) {
			packet.setBody(message.getBody());
		}
		return packet;
	}

	public MessagePacket generateNotAcceptable(MessagePacket origin) {
		MessagePacket packet = generateError(origin);
		Element error = packet.addChild("error");
		error.setAttribute("type", "modify");
		error.setAttribute("code", "406");
		error.addChild("not-acceptable");
		return packet;
	}

	private MessagePacket generateError(MessagePacket origin) {
		MessagePacket packet = new MessagePacket();
		packet.setId(origin.getId());
		packet.setTo(origin.getFrom());
		packet.setBody(origin.getBody());
		packet.setType(MessagePacket.TYPE_ERROR);
		return packet;
	}
}
