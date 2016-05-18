package com.alexanderbezverhni.cortica.sampleapp;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.BaseColumns;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.Settings;

import java.io.File;

public class Utils {

	private static final String[] PROJECTION_DATA = { MediaStore.MediaColumns.DATA };
	private static final String SELECTION_ID = BaseColumns._ID + "=?";

	private static final String API_ID_PREFIX = "some_pseudo_unique_text_";

	public static String getImageId() {
		long now = System.currentTimeMillis();
		return API_ID_PREFIX + String.valueOf(now);
	}

	public static String getDeviceId(Context context) {
		return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
	}

	/**
	 * Get a file path from a Uri. This will get the the path for Storage Access Framework Documents, as well as the _data field for the
	 * MediaStore and other file-based ContentProviders.<br> <br> Callers should check whether the path is local before assuming it represents a
	 * local file.
	 *
	 * @param context
	 * 		The context.
	 * @param uri
	 * 		The Uri to query.
	 *
	 * @author paulburke
	 */
	@SuppressLint("NewApi")
	public static String getPath(final Context context, final Uri uri) {
		final String scheme = uri.getScheme();

		// File
		if (ContentResolver.SCHEME_FILE.equals(scheme)) {
			return uri.getPath();
		}

		// MediaStore (and general)
		if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {

			// Return the remote address
			if (isOldGooglePhotosUri(uri)) {
				return uri.getLastPathSegment();
			}
			if (isGooglePhotosUri(uri)) {
				// java.lang.SecurityException: Permission Denial: reading com.google.android.apps.photos.contentprovider.MediaContentProvider
				return null;
			}

			try {
				String data = getDataColumn(context, uri, null, null);
				if (data != null) {
					return data;
				}
			} catch (Exception e) {
				// do nothing
			}
		}

		final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

		// DocumentProvider
		if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {

			// ExternalStorageProvider
			if (isExternalStorageDocument(uri)) {
				final String docId = DocumentsContract.getDocumentId(uri);
				final String[] split = docId.split(":");
				final String type = split[0];

				if ("primary".equalsIgnoreCase(type)) {
					return Environment.getExternalStorageDirectory() + File.separator + split[1];
				}

				// TODO handle non-primary volumes
			}
			// DownloadsProvider
			else if (isDownloadsDocument(uri)) {

				final String id = DocumentsContract.getDocumentId(uri);
				final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

				return getDataColumn(context, contentUri, null, null);
			}
			// MediaProvider
			else if (isMediaDocument(uri)) {
				final String docId = DocumentsContract.getDocumentId(uri);
				final String[] split = docId.split(":");
				final String type = split[0];
				final String id = split[1];

				Uri contentUri = null;
				if ("image".equals(type)) {
					contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
				} else if ("video".equals(type)) {
					contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
				} else if ("audio".equals(type)) {
					contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
				}

				final String selection = SELECTION_ID;
				final String[] selectionArgs = new String[]{ id };

				return getDataColumn(context, contentUri, selection, selectionArgs);
			}
		}

		return null;
	}

	/**
	 * Get the value of the data column for this Uri. This is useful for MediaStore Uris, and other file-based ContentProviders.
	 *
	 * @param context
	 * 		The context.
	 * @param uri
	 * 		The Uri to query.
	 * @param selection
	 * 		(Optional) Filter used in the query.
	 * @param selectionArgs
	 * 		(Optional) Selection arguments used in the query.
	 *
	 * @return The value of the _data column, which is typically a file path.
	 * @author paulburke
	 */
	public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
		Cursor cursor = null;
		final String[] projection = PROJECTION_DATA;

		try {
			cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
			if (cursor != null && cursor.moveToFirst()) {
				return cursor.getString(0);
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return null;
	}

	/**
	 * @param uri
	 * 		The Uri to check.
	 *
	 * @return Whether the Uri authority is MediaProvider.
	 * @author paulburke
	 */
	public static boolean isMediaDocument(Uri uri) {
		return "com.android.providers.media.documents".equals(uri.getAuthority());
	}

	/**
	 * @param uri
	 * 		The Uri to check.
	 *
	 * @return Whether the Uri authority is DownloadsProvider.
	 * @author paulburke
	 */
	public static boolean isDownloadsDocument(Uri uri) {
		return "com.android.providers.downloads.documents".equals(uri.getAuthority());
	}

	/**
	 * @param uri
	 * 		The Uri to check.
	 *
	 * @return Whether the Uri authority is ExternalStorageProvider.
	 * @author paulburke
	 */
	public static boolean isExternalStorageDocument(Uri uri) {
		return "com.android.externalstorage.documents".equals(uri.getAuthority());
	}

	/**
	 * Is media from Google Photos (aka Picasa)?
	 *
	 * @param uri
	 * 		The Uri to check.
	 *
	 * @return Whether the Uri authority is Google Photos.
	 */
	public static boolean isOldGooglePhotosUri(Uri uri) {
		return "com.google.android.apps.photos.content".equals(uri.getAuthority());
	}

	/**
	 * Is media from Google Photos?
	 *
	 * @param uri
	 * 		The Uri to check.
	 *
	 * @return Whether the Uri authority is Google Photos.
	 */
	public static boolean isGooglePhotosUri(Uri uri) {
		return "com.google.android.apps.photos.contentprovider".equals(uri.getAuthority());
	}
}
