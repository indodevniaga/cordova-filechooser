package com.megster.cordova;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.ContentResolver;

public class FileChooser extends CordovaPlugin {

    private static final String TAG = "FileChooser";
    private static final String ACTION_OPEN = "open";
    private static final int PICK_FILE_REQUEST = 1;

    public static final String MIME = "mime";
    private Context context;
    private ContentResolver contentResolver;
    CallbackContext callback;

    @Override
    public boolean execute(String action, JSONArray inputs, CallbackContext callbackContext) throws JSONException {

        if (action.equals(ACTION_OPEN)) {
            JSONObject filters = inputs.optJSONObject(0);
            chooseFile(filters, callbackContext);
            return true;
        }

        return false;
    }

    public void chooseFile(JSONObject filter, CallbackContext callbackContext) {
        String uri_filter = filter.has(MIME) ? filter.optString(MIME) : "*/*";

        // type and title should be configurable

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType(uri_filter);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.setFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

        Intent chooser = Intent.createChooser(intent, "Select File");
        cordova.startActivityForResult(this, chooser, PICK_FILE_REQUEST);

        PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
        pluginResult.setKeepCallback(true);
        callback = callbackContext;
        callbackContext.sendPluginResult(pluginResult);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == PICK_FILE_REQUEST && callback != null) {

            if (resultCode == Activity.RESULT_OK) {

                Uri uri = data.getData();
                if (uri != null) {
                    File file = new File(String.valueOf(uri));
                    Log.w(TAG, uri.toString());
                    //  context.getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    String path =  makeFileCopyInCacheDirectory(uri);
                    callback.success(path);
                } else {
                    callback.error("File uri was null");
                }

            } else if (resultCode == Activity.RESULT_CANCELED) {
                // keep this string the same as in iOS document picker plugin
                // https://github.com/iampossible/Cordova-DocPicker
                callback.error("User canceled.");
            } else {

                callback.error(resultCode);
            }
        }
    }

  public String makeFileCopyInCacheDirectory(Uri contentUri) {
    Cursor returnCursor = null;
    try {
      String[] filePathColumn = {
        MediaStore.Files.FileColumns._ID,
        MediaStore.Files.FileColumns.TITLE,
        MediaStore.Files.FileColumns.DATA,
        MediaStore.Files.FileColumns.SIZE,
        MediaStore.Files.FileColumns.DATE_ADDED,
        MediaStore.Files.FileColumns.DISPLAY_NAME,
        MediaStore.MediaColumns.DATA,
        MediaStore.MediaColumns.MIME_TYPE,
        MediaStore.MediaColumns.DISPLAY_NAME
      };
      ContentResolver contentResolver = this.cordova.getActivity().getContentResolver();
      returnCursor = contentResolver.query(contentUri, filePathColumn, null, null, null);
      if (returnCursor != null) {
        returnCursor.moveToFirst();
        int nameIndex = returnCursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME);
        String name = returnCursor.getString(nameIndex);
        File file = new File(this.cordova.getActivity().getCacheDir(), name);
        InputStream inputStream = contentResolver.openInputStream(contentUri);
        FileOutputStream outputStream = new FileOutputStream(file);
        int read = 0;
        int maxBufferSize = 1 * 1024 * 1024;
        int bytesAvailable = inputStream.available();

        //int bufferSize = 1024;
        int bufferSize = Math.min(bytesAvailable, maxBufferSize);
        byte[] buffers = intToByteArray(bufferSize);
        for (int length; (length = inputStream.read(buffers)) != -1; ) {
          outputStream.write(buffers, 0, length);
        }
        inputStream.close();
        outputStream.close();
        return file.getAbsolutePath();
      }
      return null;
    }
    catch(Exception e) {
      return null;
    }
  }

  public static final byte[] intToByteArray(int value) {
    return new byte[] {
      (byte)(value >>> 24),
      (byte)(value >>> 16),
      (byte)(value >>> 8),
      (byte)value};
  }
}
