package com.example.administrator.demomms;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private EditText edtPhoneNumber;
    private EditText edtMessage;
    private Button btnSend;
    private ImageButton ibMedia;
    private ImageView imgDetail;
    private String phoneNumber = "15555215554";
    private String contentMessage;
    private String url;
    private final int REQUEST_CODE_LIBRARY = 1;
    private final int REQUEST_CODE_CAMERA = 2;
    private final int PERMISSIONS_REQUEST = 7;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        iniUI();
    }

    protected void iniUI() {
        edtPhoneNumber = (EditText) findViewById(R.id.edit_message);
        edtMessage = (EditText) findViewById(R.id.edit_message);
        btnSend = (Button) findViewById(R.id.btn_send);
        ibMedia = (ImageButton) findViewById(R.id.ib_add_media);
        imgDetail = (ImageView) findViewById(R.id.img_detail);
        ibMedia.setOnClickListener(this);
        btnSend.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_send:
//                contentMessage = edtMessage.getText().toString();
//                url = "file:///sdcard/Cristiano_Ronaldo_2017.jpg";
//                Intent sendIntent = new Intent(Intent.ACTION_SEND);
//                sendIntent.setClassName("com.android.mms", "com.android.mms.ui.ComposeMessageActivity");
//                sendIntent.putExtra("address", phoneNumber);
//                sendIntent.putExtra("sms_body", contentMessage);
//                sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(url));
//                sendIntent.setType(url);
//
//                sendIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//
//                startActivity(sendIntent);
//                sendMMS();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST);
                    } else {
                        showCaptureImageDialog(REQUEST_CODE_LIBRARY, REQUEST_CODE_CAMERA);
                    }
                } else {
                    showCaptureImageDialog(REQUEST_CODE_LIBRARY, REQUEST_CODE_CAMERA);
                }
                break;
        }
    }

    private void showCaptureImageDialog(final int request_code_library, final int request_code_camera) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select type");
        builder.setNeutralButton("Gallery",
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(MainActivity.this, ImagePickerActivity.class);
                        intent.putExtra(ImagePickerActivity.EXTRA_TYPE_PICKER,
                                ImagePickerActivity.GALLERY_PICKER);
                        intent.putExtra(ImagePickerActivity.MODE, ImagePickerActivity.Mode.CROP);
                        startActivityForResult(intent, 1);
                    }
                });

        builder.setNegativeButton("Camera",
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        startActivityForResult(cameraIntent, request_code_camera);
                    }
                });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST) {
            if ((grantResults.length > 0)
                    && (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    && (grantResults[1] == PackageManager.PERMISSION_GRANTED)) {
                showCaptureImageDialog(REQUEST_CODE_LIBRARY, REQUEST_CODE_CAMERA);
            } else {
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {

            ImagePickerActivity.ImageReceiver receiver = new ImagePickerActivity.ImageReceiver(data);
            String mImagePath = receiver.getCroppedPath();
            Uri uri = Uri.fromFile(new File(mImagePath));
            Log.d("AKSHD", mImagePath + "");
            Glide.with(this).load(mImagePath).centerCrop().into(imgDetail);
            Log.d("AnhNQ ", " 11 mImagePath : " + mImagePath);
        } else if (requestCode == 2 && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                if (data.getData() == null) {
                    Bitmap photo = (Bitmap) data.getExtras().get("data");
                    if (photo != null) {
                        final File file = savebitmap(photo);
                        String mImagePath = file.getPath();
                        Glide.clear(imgDetail);
                        Glide.with(this).load(mImagePath).diskCacheStrategy(DiskCacheStrategy.NONE)
                                .centerCrop().skipMemoryCache(true).into(imgDetail);
                        imgDetail.invalidate();
                    }
                }
            }
        }
    }

    public static File savebitmap(Bitmap bmp) {
        String extStorageDirectory = Environment.getExternalStorageDirectory().toString();
        OutputStream outStream = null;
        // String temp = null;
        File file = new File(extStorageDirectory, "temp.png");
        if (file.exists()) {
            file.delete();
            file = new File(extStorageDirectory, "temp.png");
        }

        try {
            outStream = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, outStream);
            outStream.flush();
            outStream.close();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return file;
    }

    private void sendMMS() {
        try {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.putExtra("sms_body", "Hi how are you");
            String path = "/sdcard/ronaldo.png";
            Uri uri = Uri.fromFile(new File(path));
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.setType("image/png");
            startActivity(Intent.createChooser(intent, "Send"));
        } catch (Exception e) {
            Log.d("CHECK_LOG", "sendMMS: " + e.getMessage());
        }
    }

}
