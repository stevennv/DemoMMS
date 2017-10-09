package com.example.administrator.demomms;

/**
 * Created by Administrator on 09/10/2017.
 */

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.ArrayAdapter;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;


public class ImagePickerActivity extends FragmentActivity {
    private static final String TAG = "ImagePickerActivity";

    public static final int REQUEST_CODE_GALLERY = 0x1;
    public static final int REQUEST_CODE_TAKE_PICTURE = 0x2;
    public static final int REQUEST_CODE_CROP_IMAGE = 0x3;

    public static final int GALLERY_PICKER = 0x1;
    public static final int CAMERA_PICKER = 0x2;

    public static final String EXTRA_TYPE_PICKER = "EXTRA_TYPE_PICKER";
    public static final String CROPPED_IMAGE_PATH = "cropped_image_path";
    public static final String ORIGINAL_IMAGE_PATH = "original_image_path";
    public static final String ORIGINAL_IMAGE_URI = "original_image_uri";
    public static final String MODE = "mode";

    private static final String KEY_URI = "KEY_URI_IMAGE";
    private static final String KEY_ORIGINAL_PATH = "KEY_ORIGINAL_PATH";
    private static final String KEY_CROPPED_PATH = "KEY_CROPPED_PATH";

    private Uri mImageCaptureUri;
    private String mOriginalImagePath;
    private String mCroppedImagePath;

    private Mode mMode;

    public enum Mode {
        CROP, ORIGINAL
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        // ditermine type picker
        Intent intent = getIntent();
        if (intent != null && bundle == null) {
            int type = intent.getIntExtra(EXTRA_TYPE_PICKER, -1);
            if (type == GALLERY_PICKER) {
                openGallery();
            } else if (type == CAMERA_PICKER) {
                takePicture();
            }
        }
        if (intent != null) {
            mMode = (Mode) intent.getSerializableExtra(MODE);
        }
        // if (bundle != null) {
        // mMode = (Mode) bundle.getSerializable(MODE);
        // mImageCaptureUri = bundle.getParcelable(KEY_URI);
        // mCroppedImagePath = bundle.getString(KEY_CROPPED_PATH);
        // mOriginalImagePath = bundle.getString(KEY_ORIGINAL_PATH);
        // }
    }

    private void takePicture() {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat(
                    "yyyyMMddHHmmssSSS", Locale.getDefault());
            Calendar calendar = Calendar.getInstance();
            String fileName = "andG_" + dateFormat.format(calendar.getTime())
                    + ".jpg";

            mImageCaptureUri = Uri.fromFile(new File(
                    getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                    fileName));
            // mImageCaptureUri = Uri.fromFile(new
            // File(Environment.getExternalStorageDirectory(),
            // "tmp_avatar_" + String.valueOf(System.currentTimeMillis()) +
            // ".jpg"));

            // mImageCaptureUri = Uri
            // .fromFile(new File(
            // Environment
            // .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            // fileName));
            // use standard intent to capture an image
            Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mImageCaptureUri);
            captureIntent.putExtra("return-data", true);
            startActivityForResult(captureIntent, REQUEST_CODE_TAKE_PICTURE);
        } catch (ActivityNotFoundException e) {
            Log.d(TAG, "cannot take picture", e);
        }
    }

    private void openGallery() {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, REQUEST_CODE_GALLERY);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_URI, (Parcelable) mImageCaptureUri);
        outState.putSerializable(MODE, mMode);
        outState.putString(KEY_CROPPED_PATH, mCroppedImagePath);
        outState.putString(KEY_ORIGINAL_PATH, mOriginalImagePath);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.e(TAG, "RESULT_OK 1");
        if (resultCode != RESULT_OK) {
            finish();
            return;
        }
        Log.e(TAG, "RESULT_OK");
        switch (requestCode) {
            case REQUEST_CODE_GALLERY:
                try {
                    mImageCaptureUri = data.getData();
                    mOriginalImagePath = getRealPathFromURI(mImageCaptureUri);
                    if (mMode == Mode.CROP) {
                        doCrop();
                    } else {
                        finishGetImage();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error while creating temp file", e);
                    finishGetImage();
                }
                break;
            case REQUEST_CODE_TAKE_PICTURE:
                mOriginalImagePath = mImageCaptureUri.getPath();
                if (mMode == Mode.CROP) {
                    doCrop();
                } else {
                    finishGetImage();
                }
                break;
            case REQUEST_CODE_CROP_IMAGE:
                Log.e(TAG, "REQUEST_CODE_CROP_IMAGE");
                finishGetImage();
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mImageCaptureUri = (Uri) savedInstanceState.getParcelable(KEY_URI);
        mMode = (Mode) savedInstanceState.getSerializable(MODE);
        mCroppedImagePath = savedInstanceState.getString(KEY_CROPPED_PATH);
        mOriginalImagePath = savedInstanceState.getString(KEY_ORIGINAL_PATH);
    }

    private void finishGetImage() {
        final Intent data = new Intent();
        if (mCroppedImagePath != null) {
            data.putExtra(CROPPED_IMAGE_PATH, mCroppedImagePath);
        }
        if (mOriginalImagePath != null) {
            data.putExtra(ORIGINAL_IMAGE_PATH, mOriginalImagePath);
        }
        if (mImageCaptureUri != null) {
            data.putExtra(ORIGINAL_IMAGE_URI, mImageCaptureUri);
        }
        Runnable runnable = new Runnable() {

            @Override
            public void run() {
                setResult(RESULT_OK, data);
                finish();
            }
        };
        Handler handler = new Handler();
        handler.postDelayed(runnable, 1000);
    }

    // TODO: xay ra loi khong lay duoc path, invalid row
    private String getRealPathFromURI(Uri contentURI) {
        Cursor cursor = getContentResolver().query(contentURI, null, null,
                null, null);
        if (cursor == null)
            return contentURI.getPath();
        else {
            cursor.moveToFirst();
            int idx = cursor
                    .getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            return cursor.getString(idx);
        }
    }

    /**
     * Gets the corresponding path to a file from the given content:// URI
     *
     * @param selectedVideoUri The content:// URI to find the file path from
     * @param contentResolver  The content resolver to use to perform the query.
     * @return the file path as a string
     */
    private String getFilePathFromContentUri(Uri selectedVideoUri,
                                             ContentResolver contentResolver) {
        String filePath;
        String[] filePathColumn = {MediaColumns.DATA};

        Cursor cursor = contentResolver.query(selectedVideoUri, filePathColumn,
                null, null, null);
        cursor.moveToFirst();

        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        filePath = cursor.getString(columnIndex);
        cursor.close();
        return filePath;
    }

    private void doCrop() {
        final ArrayList<CropOption> cropOptions = new ArrayList<CropOption>();
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setType("image/*");
        List<ResolveInfo> list = getPackageManager().queryIntentActivities(
                intent, 0);
        int size = list.size();
        if (size == 0) {
            return;
        } else {
            list = list.subList(0, 1);
            size = list.size();
            Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
                    .getDefaultDisplay();
            int width = display.getWidth();
            int height = display.getHeight();
            Log.e("width ", "width = " + width + "height " + height);
            intent.setData(mImageCaptureUri);
            intent.putExtra("outputX", width);
            intent.putExtra("outputY", width);
            intent.putExtra("aspectX", 1);
            intent.putExtra("aspectY", 1);
            UUID random = UUID.randomUUID();
            File file = new File(getExternalFilesDir(null), random + ".jpg");
            try {
                file.createNewFile();
                mCroppedImagePath = file.getPath();
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
            } catch (IOException e) {
                e.printStackTrace();
            }
            intent.putExtra("return-data", false);

            Intent i = new Intent(intent);
            ResolveInfo res = list.get(0);
            i.setComponent(new ComponentName(res.activityInfo.packageName,
                    res.activityInfo.name));
            Log.e(TAG, "doCrop");
            startActivityForResult(i, REQUEST_CODE_CROP_IMAGE);

//            if (size == 1) {
//                Intent i = new Intent(intent);
//                ResolveInfo res = list.get(0);
//                i.setComponent(new ComponentName(res.activityInfo.packageName,
//                        res.activityInfo.name));
//                Log.e(TAG, "doCrop");
//                startActivityForResult(i, REQUEST_CODE_CROP_IMAGE);
//            } else {
//                for (ResolveInfo res : list) {
//                    final CropOption co = new CropOption();
//
//                    co.title = getPackageManager().getApplicationLabel(
//                            res.activityInfo.applicationInfo);
//                    co.icon = getPackageManager().getApplicationIcon(
//                            res.activityInfo.applicationInfo);
//                    co.appIntent = new Intent(intent);
//
//                    co.appIntent
//                            .setComponent(new ComponentName(
//                                    res.activityInfo.packageName,
//                                    res.activityInfo.name));
//
//                    cropOptions.add(co);
//                }
//
//                CropOptionAdapter adapter = new CropOptionAdapter(
//                        getApplicationContext(), cropOptions);
//
//                Builder builder = new Builder(this);
//
//                builder.setTitle("Chose method crop image.");
//
//                builder.setAdapter(adapter,
//                        new DialogInterface.OnClickListener() {
//                            public void onClick(DialogInterface dialog, int item) {
//                                startActivityForResult(
//                                        cropOptions.get(item).appIntent,
//                                        REQUEST_CODE_CROP_IMAGE);
//                            }
//                        });
//
//                builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
//                    @Override
//                    public void onCancel(DialogInterface dialog) {
//
//                        if (mImageCaptureUri != null) {
//                            getContentResolver().delete(mImageCaptureUri, null,
//                                    null);
//                            mImageCaptureUri = null;
//                        }
//                    }
//                });
//                AlertDialog alert = builder.create();
//                alert.show();
//            }
        }
    }

    /**
     * Use only in activity
     *
     * @param activity
     * @param requestCode
     * @param mode
     */
    public static void chooseTypeImagePicker(final Activity activity,
                                             final int requestCode, final Mode mode) {
        final String[] items = new String[]{
                "Camera",
                "Gellary"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(activity,
                android.R.layout.select_dialog_item, items);
        Builder builder = new Builder(activity);
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(activity, ImagePickerActivity.class);
                intent.putExtra(MODE, mode);
                // camera: cause by items
                if (which == 0) {
                    intent.putExtra(ImagePickerActivity.EXTRA_TYPE_PICKER,
                            ImagePickerActivity.CAMERA_PICKER);
                } else {
                    intent.putExtra(ImagePickerActivity.EXTRA_TYPE_PICKER,
                            ImagePickerActivity.GALLERY_PICKER);
                }
                activity.startActivityForResult(intent, requestCode);
            }
        });
        builder.show();
    }

    public static void chooseImageFromCamera(Activity activity,
                                             int requestCode, Mode mode) {
        chooseImageGeneral(activity, requestCode, CAMERA_PICKER, mode);
    }

    public static void chooseImageFromGallery(Activity activity,
                                              int requestCode, Mode mode) {
        chooseImageGeneral(activity, requestCode, GALLERY_PICKER, mode);
    }

    private static void chooseImageGeneral(Activity activity, int requestCode,
                                           int pickerType, Mode mode) {
        Intent intent = new Intent(activity, ImagePickerActivity.class);
        intent.putExtra(ImagePickerActivity.EXTRA_TYPE_PICKER, pickerType);
        intent.putExtra(MODE, mode);
        activity.startActivityForResult(intent, requestCode);
    }

    public interface ImageIntentReceiver {
        public void intentReceived(Intent intent);

    }

    /**
     * Use when in a fragment
     *
     * @param activity
     * @param mode
     * @param imageChoose
     */
    public static void chooseTypeImagePicker(final Activity activity,
                                             final Mode mode, final ImageIntentReceiver imageChoose) {
        final String[] items = new String[]{
                "Camera",
                "Gellary"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(activity,
                android.R.layout.select_dialog_item, items);
        Builder builder = new Builder(activity);
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // camera: cause by items
                Intent intent = new Intent(activity, ImagePickerActivity.class);
                intent.putExtra(MODE, mode);
                if (which == 0) {
                    intent.putExtra(ImagePickerActivity.EXTRA_TYPE_PICKER,
                            ImagePickerActivity.CAMERA_PICKER);
                } else {
                    intent.putExtra(ImagePickerActivity.EXTRA_TYPE_PICKER,
                            ImagePickerActivity.GALLERY_PICKER);
                }
                imageChoose.intentReceived(intent);
            }
        });
        builder.show();
    }

    public static class ImageReceiver {
        private String originalPath;
        private String croppedPath;

        public ImageReceiver(Intent intent) {
            if (intent != null) {
                croppedPath = intent
                        .getStringExtra(ImagePickerActivity.CROPPED_IMAGE_PATH);
                originalPath = intent
                        .getStringExtra(ImagePickerActivity.ORIGINAL_IMAGE_PATH);

            }
        }

        public String getCroppedPath() {
            return croppedPath;
        }

        public String getOriginalPath() {
            return originalPath;
        }

    }

}
