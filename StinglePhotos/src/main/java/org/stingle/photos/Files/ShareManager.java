package org.stingle.photos.Files;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.collection.ArraySet;
import androidx.core.content.FileProvider;

import org.stingle.photos.AsyncTasks.DecryptFilesAsyncTask;
import org.stingle.photos.AsyncTasks.OnAsyncTaskFinish;
import org.stingle.photos.Db.Objects.StingleDbFile;
import org.stingle.photos.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ShareManager {

	public static void shareDbFiles(final Context context, List<StingleDbFile> dbFiles, int set, String albumId){


		DecryptFilesAsyncTask decFilesJob = new DecryptFilesAsyncTask(context, new OnAsyncTaskFinish() {
			@Override
			public void onFinish(ArrayList<File> files) {
				super.onFinish(files);
				shareFiles(context, files);
			}
		});
		decFilesJob.setSet(set);
		decFilesJob.setAlbumId(albumId);
		decFilesJob.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, dbFiles);
	}

	public static void shareFiles(Context context, ArrayList<File> fileToShare) {
		if(fileToShare == null){
			return;
		}
		if (fileToShare.size() == 1) {
			Intent share = new Intent(Intent.ACTION_SEND);
			share.setType(getMimeType(fileToShare.get(0).getPath()));

			share.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(context.getApplicationContext(), context.getString(R.string.content_provider), fileToShare.get(0)));
			context.startActivity(Intent.createChooser(share, "Share Image"));
		}
		else if (fileToShare.size() > 1) {
			Intent share = new Intent(Intent.ACTION_SEND_MULTIPLE);

			//String[] mimetypes = {"image/*", "video/*"};
			//share.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
			//ClipData clipData = new ClipData();

			String mimeType = null;
			boolean sameMimeType = true;
			ArrayList<Uri> uris = new ArrayList<Uri>();
			for (int i = 0; i < fileToShare.size(); i++) {
				//clipData.addItem(new ClipData.Item());
				String thisMimeType = getMimeType(fileToShare.get(0).getPath());
				if(mimeType != null && !mimeType.equals(thisMimeType)){
					sameMimeType = false;
				}
				mimeType = thisMimeType;

				uris.add(FileProvider.getUriForFile(context.getApplicationContext(), context.getString(R.string.content_provider), fileToShare.get(i)));
			}

			if(sameMimeType) {
				share.setType(mimeType);
				Log.d("mimeType", mimeType);
			}
			else{
				share.setType("*/*");
			}

			//share.setClipData(clipData);
			share.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
			context.startActivity(Intent.createChooser(share, context.getString(R.string.share)));
		}
	}

	public static void sendBackSelection(final Activity activity, final Intent originalIntent, ArrayList<StingleDbFile> selectedFiles, int set, String albumId) {
		DecryptFilesAsyncTask decFilesJob = new DecryptFilesAsyncTask(activity, new OnAsyncTaskFinish() {
			@Override
			public void onFinish(ArrayList<File> files) {
				super.onFinish(files);
				if(files != null) {
					if (originalIntent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)) {
						originalIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

						ArraySet<String> mimeTypesSet = new ArraySet<>();
						for(File file : files){
							mimeTypesSet.add(getMimeType(file.getPath()));
						}
						String[] mimeTypes = new String[mimeTypesSet.size()];
						for(int i=0; i < mimeTypesSet.size(); i ++){
							mimeTypes[i] = mimeTypesSet.valueAt(i);
						}

						ClipData.Item firstItem = new ClipData.Item(FileProvider.getUriForFile(activity, activity.getString(R.string.content_provider), files.get(0)));
						ClipData clipData = new ClipData("files", mimeTypes, firstItem);
						if(files.size() > 1) {
							for (int i = 1; i < files.size(); i++) {
								clipData.addItem(new ClipData.Item(FileProvider.getUriForFile(activity, activity.getString(R.string.content_provider), files.get(i))));
							}
						}
						originalIntent.setClipData(clipData);
						activity.setResult(Activity.RESULT_OK, originalIntent);
						activity.finish();
						return;
					}
					else{
						Uri fileUri = FileProvider.getUriForFile(activity, activity.getString(R.string.content_provider), files.get(0));
						originalIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
						originalIntent.setDataAndType(fileUri, activity.getContentResolver().getType(fileUri));
						activity.setResult(Activity.RESULT_OK, originalIntent);
						activity.finish();
						return;
					}
				}
				originalIntent.setDataAndType(null, "");
				activity.setResult(Activity.RESULT_CANCELED, originalIntent);
				activity.finish();
			}
		});
		decFilesJob.setSet(set);
		decFilesJob.setAlbumId(albumId);
		decFilesJob.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, selectedFiles);
	}

	public static String getMimeType(String url) {
		String type = null;
		String extension = MimeTypeMap.getFileExtensionFromUrl(url);
		if (extension != null) {
			type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
		}
		return type;
	}

	public static void scanFile(final Context context, File file) {
	    try {
	    	if( file.isFile() ) {
		        MediaScannerConnection.scanFile(context, new String[] { file.getAbsolutePath() }, null, null);
	    	}
	    } catch (Exception e) {
	        e.printStackTrace();
	    }

	}
}
