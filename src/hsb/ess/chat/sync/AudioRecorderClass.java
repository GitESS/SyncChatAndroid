package hsb.ess.chat.sync;

import hsb.ess.chat.entities.Conversation;
import hsb.ess.chat.ui.ConversationActivity;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.ford.syncV4.exception.SyncException;
import com.ford.syncV4.proxy.rpc.PerformAudioPassThru;
import com.ford.syncV4.proxy.rpc.enums.Result;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

public class AudioRecorderClass {
	private static AudioRecorderClass mRecording = null;
	private static final String STR_FILE_PCM = "app_audio.pcm";
	private static final String STR_FILE_WAV = "app_audio.wav";
	public static final int INT_WAV = 0;
	public static final int INT_PCM = 1;
	private int iMByteCount = 0;
	public int mySampleRate = 0;
	public int myBitsPerSample = 0;
	private OutputStream mOutStreamAudio = null;
	private MediaPlayer mMediaPlayerAudio = null;
	public PerformAudioPassThru latestPerformAudioPassThruMsg = null;

	public static AudioRecorderClass getInstance() {
		if (null == mRecording) {
			mRecording = new AudioRecorderClass();
		}

		return mRecording;
	}

	public void audioPassThru(byte[] aptData) {

		if (aptData == null) {
			return;
		}

		Log.i("app", "len =" + aptData.length + " count=" + iMByteCount);
		
		iMByteCount = iMByteCount + aptData.length;
		File audioOutFile = getAudioOutputFile(INT_PCM);

		try {
			if (mOutStreamAudio == null) {
				mOutStreamAudio = new BufferedOutputStream(
						new FileOutputStream(audioOutFile, false));
			}
			mOutStreamAudio.write(aptData);
			mOutStreamAudio.flush();
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
	}

	public void performAudioPassThruResponse(Result result,
			Conversation conversation) {
		closeAudioStream();
		closeAudioMediaPlayer();

			saveAsWav(conversation);

	}

	public void endAudioPassThruResponse(Result result,
			Conversation conversation) {
		performAudioPassThruResponse(result ,conversation);
	}

	public void closeAudioStream() {
		if (mOutStreamAudio != null) {
			try {
				mOutStreamAudio.flush();
				mOutStreamAudio.close();
			} catch (IOException e) {
			}
			mOutStreamAudio = null;
		}
	}

	public void closeAudioMediaPlayer() {
		if (mMediaPlayerAudio == null) {
			return;
		}

		if (mMediaPlayerAudio.isPlaying()) {
			mMediaPlayerAudio
					.setOnCompletionListener(new OnCompletionListener() {
						@Override
						public void onCompletion(MediaPlayer mp) {
							mMediaPlayerAudio.reset();
							mMediaPlayerAudio.release();
							mMediaPlayerAudio = null;
						}
					});
		} else {
			mMediaPlayerAudio.release();
			mMediaPlayerAudio = null;
		}
	}

	private File getAudioOutputFile(int iTypePar) {

		String sFileType = "";
		Date date = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat(
				"yyyy-MM-dd HH-mm-ss");

		if (iTypePar == INT_WAV) {
			sFileType = dateFormat.format(date) + ".wav";

			// sFileType = STR_FILE_WAV;
		} else if (iTypePar == INT_PCM) {
			sFileType = STR_FILE_PCM;
		}

		// File baseDir = isWritable() ?
		// Environment.getExternalStorageDirectory()
		// : ConversationActivity.getInstance().getFilesDir();

		File folder = new File(Environment.getExternalStorageDirectory()
				+ File.separator + "SyncChat");

		if (!folder.exists()) {
			folder.mkdir();
		}
		File folderSent = new File(Environment.getExternalStorageDirectory()
				+ File.separator + "SyncChat" + File.separator + "Sent");

		if (!folderSent.exists()) {
			folderSent.mkdir();
		}

		File audioOutFile = new File(folderSent, sFileType);
		return audioOutFile;
	}

	private boolean isWritable() {
		String state = Environment.getExternalStorageState();
		return Environment.MEDIA_MOUNTED.equals(state);
	}

	private byte[] getWaveFileHeader(DataOutputStream out, long totalAudioLen,
			long totalDataLen, long longSampleRate, int channels, long byteRate) {
		byte[] header = new byte[44];
		header[0] = 'R';
		header[1] = 'I';
		header[2] = 'F';
		header[3] = 'F';
		header[4] = (byte) (totalDataLen & 0xff);
		header[5] = (byte) ((totalDataLen >> 8) & 0xff);
		header[6] = (byte) ((totalDataLen >> 16) & 0xff);
		header[7] = (byte) ((totalDataLen >> 24) & 0xff);
		header[8] = 'W';
		header[9] = 'A';
		header[10] = 'V';
		header[11] = 'E';
		header[12] = 'f';
		header[13] = 'm';
		header[14] = 't';
		header[15] = ' ';
		header[16] = 16;
		header[17] = 0;
		header[18] = 0;
		header[19] = 0;
		header[20] = 1;
		header[21] = 0;
		header[22] = (byte) channels;
		header[23] = 0;
		header[24] = (byte) (longSampleRate & 0xff);
		header[25] = (byte) ((longSampleRate >> 8) & 0xff);
		header[26] = (byte) ((longSampleRate >> 16) & 0xff);
		header[27] = (byte) ((longSampleRate >> 24) & 0xff);
		header[28] = (byte) (byteRate & 0xff);
		header[29] = (byte) ((byteRate >> 8) & 0xff);
		header[30] = (byte) ((byteRate >> 16) & 0xff);
		header[31] = (byte) ((byteRate >> 24) & 0xff);
		header[32] = (byte) (channels * myBitsPerSample / 8);
		header[33] = 0;
		header[34] = (byte) myBitsPerSample;
		header[35] = 0;
		header[36] = 'd';
		header[37] = 'a';
		header[38] = 't';
		header[39] = 'a';
		header[40] = (byte) (totalAudioLen & 0xff);
		header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
		header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
		header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
		return header;
	}

	private boolean saveAsWav(Conversation conversation) {
		
		File currentPath = getAudioOutputFile(INT_WAV);
		try {
			byte[] myData;
			DataOutputStream outFile = new DataOutputStream(
					new FileOutputStream(currentPath));
			long totalAudioLen = iMByteCount;
			long totalDataLen = totalAudioLen + 36;
			long longSampleRate = mySampleRate;
			int channels = 1;
			long byteRate = mySampleRate * channels * myBitsPerSample / 8;
			byte[] header = getWaveFileHeader(outFile, totalAudioLen,
					totalDataLen, longSampleRate, channels, byteRate);
			outFile.write(header, 0, 44);
			DataInputStream inFile = new DataInputStream(new FileInputStream(
					getAudioOutputFile(INT_PCM)));
			myData = new byte[iMByteCount];
			inFile.read(myData);
			outFile.write(myData);
			inFile.close();
			outFile.close();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
		
		Uri uri = Uri.fromFile(currentPath);
		ConversationActivity conAct;
		conAct = new ConversationActivity();
		conAct.attachAudioToConversation(conversation, uri,
				ConversationActivity.getInstance().xmppConnectionService);
		return true;
	}
	
	public void resetClass() {
		// TODO Auto-generated method stub
		mRecording = null;
		iMByteCount = 0;
		mySampleRate = 0;
		myBitsPerSample = 0;
		mOutStreamAudio = null;
		mMediaPlayerAudio = null;
		latestPerformAudioPassThruMsg = null;

	}

}
