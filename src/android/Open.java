package com.disusered;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Base64;
import android.webkit.MimeTypeMap;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * This class starts an activity for an intent to view files
 */
public class Open extends CordovaPlugin {

    public static final String ACTION_OPEN = "open";
    public static final String ACTION_OPEN_BASE64 = "openBase64";

    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        if (action.equals(ACTION_OPEN)) {

            final String path = args.getString(0);
            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    chooseIntent(path, null, callbackContext);
                }
            });

            return true;

        } else if (action.equals(ACTION_OPEN_BASE64)) {

            final String byteString = args.getString(0);
            final String mimeType = args.getString(1);
            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    saveAndChooseIntent(byteString, mimeType, callbackContext);
                }
            });

            return true;
        }
        return false;
    }

    /**
     * Returns the MIME type of the file.
     *
     * @param path
     * @return
     */
    private static String getMimeType(String path) {
        String mimeType = null;

        String extension = MimeTypeMap.getFileExtensionFromUrl(path);
        if (extension != null) {
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            mimeType = mime.getMimeTypeFromExtension(extension);
        }

        System.out.println("Mime type: " + mimeType);

        return mimeType;
    }

    /**
     * Creates an intent for the data of mime type
     *
     * @param path
     * @param callbackContext
     */
    private void chooseIntent(String path, String mime, CallbackContext callbackContext) {
        if (path != null && path.length() > 0) {
            try {
                Uri uri = Uri.parse(path);
                if (mime == null) mime = getMimeType(path);
                Intent fileIntent = new Intent(Intent.ACTION_VIEW);

                if (Build.VERSION.SDK_INT > 15) {
                    fileIntent.setDataAndTypeAndNormalize(uri, mime); // API Level 16 -> Android 4.1
                } else {
                    fileIntent.setDataAndType(uri, mime);
                }

                cordova.getActivity().startActivity(fileIntent);

                callbackContext.success();
            } catch (ActivityNotFoundException e) {
                e.printStackTrace();
                callbackContext.error(1);
            }
        } else {
            callbackContext.error(2);
        }
    }

    /**
     * Decodes the base64 encoded string as a temporary file and then calls chooser intent on that file.
     * MIME type must be supplied when using this function.
     *
     * @param byteString      (string) - Base64 encoded string
     * @param mimeType        (string) - mime type e.g. 'application/pdf'
     * @param callbackContext
     */
    private void saveAndChooseIntent(String byteString, String mimeType, CallbackContext callbackContext) {

        if (isExternalStorageWritable()) {
            try {
                byte[] byteArray = Base64.decode(byteString, 0);
                File filePath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/_tempfile");
                FileOutputStream os = new FileOutputStream(filePath, true);
                os.write(byteArray);
                os.flush();
                os.close();
                chooseIntent(filePath.getAbsolutePath(), mimeType, callbackContext);
            } catch (IOException | IllegalArgumentException e) {
                e.printStackTrace();
                callbackContext.error(e.getMessage());
            }
        } else {
            callbackContext.error(2);
        }
    }

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }
}
