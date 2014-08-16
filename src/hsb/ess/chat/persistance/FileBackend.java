package hsb.ess.chat.persistance;

import hsb.ess.chat.R;
import hsb.ess.chat.entities.Conversation;
import hsb.ess.chat.entities.Message;
import hsb.ess.chat.services.XmppConnectionService;
import hsb.ess.chat.sync.LockScreenActivity;
import hsb.ess.chat.ui.ContactsActivity;
import hsb.ess.chat.ui.ConversationActivity;
import hsb.ess.chat.ui.Utils;
import hsb.ess.chat.ui.XmppActivity;
import hsb.ess.chat.xmpp.jingle.JingleFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.ford.syncV4.proxy.rpc.enums.MaintenanceModeStatus;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.MediaRecorder.AudioEncoder;
import android.net.Uri;
import android.provider.MediaStore.Audio;
import android.util.Log;
import android.util.LruCache;

public class FileBackend {

	private static int IMAGE_SIZE = 1920;

	private Context context;
	private LruCache<String, Bitmap> thumbnailCache;

	public FileBackend(Context context) {
		this.context = context;
		int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
		int cacheSize = maxMemory / 8;
		thumbnailCache = new LruCache<String, Bitmap>(cacheSize) {
			@Override
			protected int sizeOf(String key, Bitmap bitmap) {
				return bitmap.getByteCount() / 1024;
			}
		};

	}

	public LruCache<String, Bitmap> getThumbnailCache() {
		return thumbnailCache;
	}

	public JingleFile getJingleFile(Message message) {
		return getJingleFile(message, true);
	}

	public JingleFile getJingleFile(Message message, boolean decrypted) {
		Conversation conversation = message.getConversation();
//		 if (context == null) {
//		 context = LockScreenActivity.getInstance();
//		 String prefix = context.getFilesDir().getAbsolutePath();
//		 String path = prefix + "/" + conversation.getAccount().getJid()
//		 + "/" + conversation.getContactJid();
//		 String filename;
//		 if ((decrypted)
//		 || (message.getEncryption() == Message.ENCRYPTION_NONE)) {
//		 filename = message.getUuid() + ".webp";
//		 } else {
//		 if (message.getEncryption() == Message.ENCRYPTION_OTR) {
//		 filename = message.getUuid() + ".webp";
//		 } else {
//		 filename = message.getUuid() + ".webp.pgp";
//		 }
//		 }
//		 return new JingleFile(path + "/" + filename);
//		 } else {
//		XmppConnectionService xmpp = new XmppConnectionService();
//
//		context = xmpp.getBaseContext();
			 context =  ConversationActivity.getInstance();
			
		String prefix = context.getFilesDir().getAbsolutePath();
		String path = prefix + "/" + conversation.getAccount().getJid() + "/"
				+ conversation.getContactJid();
		String filename;
		if ((decrypted) || (message.getEncryption() == Message.ENCRYPTION_NONE)) {
			filename = message.getUuid() + ".webp";
		} else {
			if (message.getEncryption() == Message.ENCRYPTION_OTR) {
				filename = message.getUuid() + ".webp";
			} else {
				filename = message.getUuid() + ".webp.pgp";
			}
		}
		return new JingleFile(path + "/" + filename);
		 //}

	}

	public Bitmap resize(Bitmap originalBitmap, int size) {
		int w = originalBitmap.getWidth();
		int h = originalBitmap.getHeight();
		if (Math.max(w, h) > size) {
			int scalledW;
			int scalledH;
			if (w <= h) {
				scalledW = (int) (w / ((double) h / size));
				scalledH = size;
			} else {
				scalledW = size;
				scalledH = (int) (h / ((double) w / size));
			}
			Bitmap scalledBitmap = Bitmap.createScaledBitmap(originalBitmap,
					scalledW, scalledH, true);
			return scalledBitmap;
		} else {
			return originalBitmap;
		}
	}

	public Bitmap rotate(Bitmap bitmap, int degree) {
		int w = bitmap.getWidth();
		int h = bitmap.getHeight();
		Matrix mtx = new Matrix();
		mtx.postRotate(degree);
		return Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true);
	}

	public JingleFile copyImageToPrivateStorage(Message message, Uri image)
			throws ImageCopyException {
		return this.copyImageToPrivateStorage(message, image, 0);
	}

	public JingleFile copyAudioToPrivateStorage(Message message, Uri image)
			throws ImageCopyException {
		return this.copyAudioToPrivateStorage(message, image, 0);
	}

	private JingleFile copyImageToPrivateStorage(Message message, Uri image,
			int sampleSize) throws ImageCopyException {
		Log.i(Utils.LOG_IMAGE, "in copy Image to private Storage");
		try {
			InputStream is;
			if (image != null) {
				is = context.getContentResolver().openInputStream(image);
			} else {
				is = new FileInputStream(getIncomingFile());
				image = getIncomingUri();
			}
			JingleFile file = getJingleFile(message);

			file.getParentFile().mkdirs();
			file.createNewFile();

			Bitmap originalBitmap;
			BitmapFactory.Options options = new BitmapFactory.Options();
			int inSampleSize = (int) Math.pow(2, sampleSize);
			Log.d("xmppService", "reading bitmap with sample size "
					+ inSampleSize);
			options.inSampleSize = inSampleSize;
			originalBitmap = BitmapFactory.decodeStream(is, null, options);
			is.close();
			if (originalBitmap == null) {
				throw new ImageCopyException(R.string.error_not_an_image_file);
			}
			if (image == null) {
				getIncomingFile().delete();
			}
			Bitmap scalledBitmap = resize(originalBitmap, IMAGE_SIZE);
			originalBitmap = null;
			ExifInterface exif = new ExifInterface(image.toString());
			if (exif.getAttribute(ExifInterface.TAG_ORIENTATION)
					.equalsIgnoreCase("6")) {
				scalledBitmap = rotate(scalledBitmap, 90);
			} else if (exif.getAttribute(ExifInterface.TAG_ORIENTATION)
					.equalsIgnoreCase("8")) {
				scalledBitmap = rotate(scalledBitmap, 270);
			} else if (exif.getAttribute(ExifInterface.TAG_ORIENTATION)
					.equalsIgnoreCase("3")) {
				scalledBitmap = rotate(scalledBitmap, 180);
			}

			OutputStream os = new FileOutputStream(file);
			Log.i("File transfer", "file size" + file.getSize());

			Log.i("File transfer", "Bitmap size" + scalledBitmap.getByteCount());
			boolean success = scalledBitmap.compress(
					Bitmap.CompressFormat.WEBP, 75, os);

			// if (!success) {
			// throw new ImageCopyException(R.string.error_compressing_image);
			// }

			Log.i("File transfer", "file size" + file.getSize());
			os.flush();
			Log.i("File transfer", "file size" + file.getSize());
			os.close();
			Log.i("File transfer", "file size" + file.getSize());
			long size = file.getSize();
			int width = scalledBitmap.getWidth();
			int height = scalledBitmap.getHeight();
			message.setBody("" + size + "," + width + "," + height);
			return file;
		} catch (FileNotFoundException e) {
			throw new ImageCopyException(R.string.error_file_not_found);
		} catch (IOException e) {
			throw new ImageCopyException(R.string.error_io_exception);
		} catch (SecurityException e) {
			throw new ImageCopyException(
					R.string.error_security_exception_during_image_copy);
		} catch (OutOfMemoryError e) {
			++sampleSize;
			if (sampleSize <= 3) {
				return copyImageToPrivateStorage(message, image, sampleSize);
			} else {
				throw new ImageCopyException(R.string.error_out_of_memory);
			}
		}
	}

	// For Audio Sharing

	private JingleFile copyAudioToPrivateStorage(Message message, Uri image,
			int sampleSize) {
		Log.i("file transfer", "File transafer in copyaudiotoprivatestorage"
				+ image);
		try {
			// InputStream is;
			// if (image != null) {
			// is = context.getContentResolver().openInputStream(image);
			// } else {
			// is = new FileInputStream(getIncomingFile());
			// image = getIncomingUri();
			// }
			JingleFile file = getJingleFile(message);
			file.getParentFile().mkdirs();
			file.createNewFile();
			// Bitmap originalBitmap;
			// BitmapFactory.Options options = new BitmapFactory.Options();
			// int inSampleSize = (int) Math.pow(2, sampleSize);
			// Log.d("xmppService", "reading bitmap with sample size "
			// + inSampleSize);
			// options.inSampleSize = inSampleSize;
			// originalBitmap = BitmapFactory.decodeStream(is, null, options);
			// is.close();
			// if (originalBitmap == null) {
			// throw new ImageCopyException(R.string.error_not_an_image_file);
			// }
			// if (image == null) {
			// getIncomingFile().delete();
			// }
			// Bitmap scalledBitmap = resize(originalBitmap, IMAGE_SIZE);
			// originalBitmap = null;
			// ExifInterface exif = new ExifInterface(image.toString());
			// if (exif.getAttribute(ExifInterface.TAG_ORIENTATION)
			// .equalsIgnoreCase("6")) {
			// scalledBitmap = rotate(scalledBitmap, 90);
			// } else if (exif.getAttribute(ExifInterface.TAG_ORIENTATION)
			// .equalsIgnoreCase("8")) {
			// scalledBitmap = rotate(scalledBitmap, 270);
			// } else if (exif.getAttribute(ExifInterface.TAG_ORIENTATION)
			// .equalsIgnoreCase("3")) {
			// scalledBitmap = rotate(scalledBitmap, 180);
			// }
			OutputStream os = new FileOutputStream(file);

			// boolean success = scalledBitmap.compress(
			// Bitmap.CompressFormat.WEBP, 75, os);
			// if (!success) {
			// throw new ImageCopyException(R.string.error_compressing_image);
			// }
			File fileUri = new File(image.getPath());
			byte[] buf = new byte[8192];

			InputStream is1 = new FileInputStream(fileUri);

			int c = 0;

			while ((c = is1.read(buf, 0, buf.length)) > 0) {
				os.write(buf, 0, c);
				os.flush();
			}

			os.flush();
			os.close();
			long size = file.getSize();
			Log.i("File Transfer", "File size" + size);
			// int width = scalledBitmap.getWidth();
			// int height = scalledBitmap.getHeight();
			message.setBody("" + size + "," + 20 + "," + 20);
			return file;

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}

	public Bitmap getImageFromMessage(Message message) {
		return BitmapFactory.decodeFile(getJingleFile(message)
				.getAbsolutePath());
	}

	public Bitmap getThumbnail(Message message, int size, boolean cacheOnly)
			throws FileNotFoundException {
		Bitmap thumbnail = thumbnailCache.get(message.getUuid());
		if ((thumbnail == null) && (!cacheOnly)) {
			Bitmap fullsize = BitmapFactory.decodeFile(getJingleFile(message)
					.getAbsolutePath());

			if (fullsize == null) {
				throw new FileNotFoundException();
			}
			thumbnail = resize(fullsize, size);
			this.thumbnailCache.put(message.getUuid(), thumbnail);
		}
		return thumbnail;
	}

	public Bitmap getAudio(Message message, int size, boolean cacheOnly)
			throws FileNotFoundException {
		Bitmap thumbnail = thumbnailCache.get(message.getUuid());
		if ((thumbnail == null) && (!cacheOnly)) {

			String pathSong = "/sdcard/Maid with the Flaxen Hair.mp3";
			File f = new File(pathSong);
			File pathRecieved = new File(getJingleFile(message)
					.getAbsolutePath());
			Log.i("file transfer",
					"path recieved" + pathRecieved.getAbsolutePath());

			// // .getAbsolutePath()
			// OutputStream oos = new FileOutputStream("abc.mp3");
			//
			// byte[] buf = new byte[8192];
			//
			// InputStream is = new FileInputStream(f);
			//
			// int c = 0;
			//
			// while ((c = is.read(buf, 0, buf.length)) > 0) {
			// oos.write(buf, 0, c);
			// oos.flush();
			// }
//			Bitmap fullsize = BitmapFactory.decodeFile(getJingleFile(message)
//					.getAbsolutePath());

//			if (fullsize == null) {
//				throw new FileNotFoundException();
		//	}
		//	thumbnail = resize(fullsize, size);
			//this.thumbnailCache.put(message.getUuid(), thumbnail);
		}
		return thumbnail;
	}

	public void removeFiles(Conversation conversation) {
		String prefix = context.getFilesDir().getAbsolutePath();
		String path = prefix + "/" + conversation.getAccount().getJid() + "/"
				+ conversation.getContactJid();
		File file = new File(path);
		try {
			this.deleteFile(file);
		} catch (IOException e) {
			Log.d("xmppService",
					"error deleting file: " + file.getAbsolutePath());
		}
	}

	private void deleteFile(File f) throws IOException {
		if (f.isDirectory()) {
			for (File c : f.listFiles())
				deleteFile(c);
		}
		f.delete();
	}

	public File getIncomingFile() {
		return new File(context.getFilesDir().getAbsolutePath() + "/incoming");
	}

	public Uri getIncomingUri() {
		return Uri.parse(context.getFilesDir().getAbsolutePath() + "/incoming");
	}

	public class ImageCopyException extends Exception {
		private static final long serialVersionUID = -1010013599132881427L;
		private int resId;

		public ImageCopyException(int resId) {
			this.resId = resId;
		}

		public int getResId() {
			return resId;
		}
	}
}