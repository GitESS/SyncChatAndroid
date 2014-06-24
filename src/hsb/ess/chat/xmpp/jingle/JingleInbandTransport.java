package hsb.ess.chat.xmpp.jingle;

import hsb.ess.chat.entities.Account;
import hsb.ess.chat.utils.CryptoHelper;
import hsb.ess.chat.xml.Element;
import hsb.ess.chat.xmpp.OnIqPacketReceived;
import hsb.ess.chat.xmpp.stanzas.IqPacket;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.util.Base64;

public class JingleInbandTransport extends JingleTransport {

	private Account account;
	private String counterpart;
	private int blockSize;
	private int bufferSize;
	private int seq = 0;
	private String sessionId;

	private boolean established = false;

	private JingleFile file;
	
	private FileInputStream fileInputStream = null;
	private FileOutputStream fileOutputStream;
	private long remainingSize;
	private MessageDigest digest;

	private OnFileTransmitted onFileTransmitted;
	
	private OnIqPacketReceived onAckReceived = new OnIqPacketReceived() {
		@Override
		public void onIqPacketReceived(Account account, IqPacket packet) {
			if (packet.getType() == IqPacket.TYPE_RESULT) {
				sendNextBlock();
			}
		}
	};

	public JingleInbandTransport(Account account, String counterpart,
			String sid, int blocksize) {
		this.account = account;
		this.counterpart = counterpart;
		this.blockSize = blocksize;
		this.bufferSize = blocksize / 4;
		this.sessionId = sid;
	}

	public void connect(final OnTransportConnected callback) {
		IqPacket iq = new IqPacket(IqPacket.TYPE_SET);
		iq.setTo(this.counterpart);
		Element open = iq.addChild("open", "http://jabber.org/protocol/ibb");
		open.setAttribute("sid", this.sessionId);
		open.setAttribute("stanza", "iq");
		open.setAttribute("block-size", "" + this.blockSize);

		this.account.getXmppConnection().sendIqPacket(iq,
				new OnIqPacketReceived() {

					@Override
					public void onIqPacketReceived(Account account,
							IqPacket packet) {
						if (packet.getType() == IqPacket.TYPE_ERROR) {
							callback.failed();
						} else {
							callback.established();
						}
					}
				});
	}

	@Override
	public void receive(JingleFile file, OnFileTransmitted callback) {
		this.onFileTransmitted = callback;
		this.file = file;
		try {
			this.digest = MessageDigest.getInstance("SHA-1");
			digest.reset();
			file.getParentFile().mkdirs();
			file.createNewFile();
			this.fileOutputStream = new FileOutputStream(file);
			this.remainingSize = file.getExpectedSize();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void send(JingleFile file, OnFileTransmitted callback) {
		this.onFileTransmitted = callback;
		this.file = file;
		try {
			this.digest = MessageDigest.getInstance("SHA-1");
			this.digest.reset();
			fileInputStream = new FileInputStream(file);
			this.sendNextBlock();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

	private void sendNextBlock() {
		byte[] buffer = new byte[this.bufferSize];
		try {
			int count = fileInputStream.read(buffer);
			if (count==-1) {
				file.setSha1Sum(CryptoHelper.bytesToHex(digest.digest()));
				fileInputStream.close();
				this.onFileTransmitted.onFileTransmitted(file);
			} else {
				this.digest.update(buffer);
				String base64 = Base64.encodeToString(buffer, Base64.DEFAULT);
				IqPacket iq = new IqPacket(IqPacket.TYPE_SET);
				iq.setTo(this.counterpart);
				Element data = iq
						.addChild("data", "http://jabber.org/protocol/ibb");
				data.setAttribute("seq", "" + this.seq);
				data.setAttribute("block-size", "" + this.blockSize);
				data.setAttribute("sid", this.sessionId);
				data.setContent(base64);
				this.account.getXmppConnection().sendIqPacket(iq,
						this.onAckReceived);
				this.seq++;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void receiveNextBlock(String data) {
		try {
			byte[] buffer = Base64.decode(data, Base64.DEFAULT);
			this.remainingSize -= buffer.length;

			this.fileOutputStream.write(buffer);

			this.digest.update(buffer);
			if (this.remainingSize <= 0) {
				file.setSha1Sum(CryptoHelper.bytesToHex(digest.digest()));
				fileOutputStream.flush();
				fileOutputStream.close();
				this.onFileTransmitted.onFileTransmitted(file);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void deliverPayload(IqPacket packet, Element payload) {
		if (payload.getName().equals("open")) {
			if (!established) {
				established = true;
				this.account.getXmppConnection().sendIqPacket(
						packet.generateRespone(IqPacket.TYPE_RESULT), null);
			} else {
				this.account.getXmppConnection().sendIqPacket(
						packet.generateRespone(IqPacket.TYPE_ERROR), null);
			}
		} else if (payload.getName().equals("data")) {
			this.receiveNextBlock(payload.getContent());
			this.account.getXmppConnection().sendIqPacket(
					packet.generateRespone(IqPacket.TYPE_RESULT), null);
		} else {
			//TODO some sort of exception
		}
	}
}
