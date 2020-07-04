package com.longhan.textrecognition;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.memetix.mst.language.Language;
import com.memetix.mst.translate.Translate;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

public class MainActivity extends AppCompatActivity {

    ImageView mPreviewIv;
    EditText mResultEt, mTransEt;
    Button mTransBtn;

    private static final int CAMERA_REQUEST_CODE = 200;
    private static final int STORAGE_REQUEST_CODE = 400;
    private static final int IMAGE_PICK_GALLERY_CODE = 1000;
    private static final int IMAGE_PICK_CAMERA_CODE = 1001;

    String cameraPermission[];
    String storagePermission[];

    Context context = this;

    Uri image_uri;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initActionBar();
        initPermission();
        initUI();
        initEvent();
    }

    //actionbar mennu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //Inflate menu
        getMenuInflater().inflate(R.menu.menu_main,menu);
        return true;
    }

    //handle actionbar item click
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.addImage)
        {
            showImageImportDialog();
        }
        if (id == R.id.settings)
        {
            Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show();
        }
        return super.onOptionsItemSelected(item);
    }

    //handle image result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //got image from camera
        if (resultCode == RESULT_OK)
        {
            if (requestCode == IMAGE_PICK_GALLERY_CODE)
            {
                //got image form gallery now crop it
                CropImage.activity(data.getData())
                        .setGuidelines(CropImageView.Guidelines.ON) //enable image guidelines
                        .start(this);
            }
            if (requestCode == IMAGE_PICK_CAMERA_CODE)
            {
                //got image form gallery now crop it
                CropImage.activity(image_uri)
                        .setGuidelines(CropImageView.Guidelines.ON) //enable image guidelines
                        .start(this);
            }
        }
        //get cropped image
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE)
        {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode  == RESULT_OK)
            {
                Uri resultUri = result.getUri(); // get image uri
                //set image to image view
                mPreviewIv.setImageURI(resultUri);

                //get drawable bitmap for text recognition
                BitmapDrawable bitmapDrawable = (BitmapDrawable)mPreviewIv.getDrawable();
                Bitmap bitmap = bitmapDrawable.getBitmap();

                TextRecognizer recognizer = new TextRecognizer.Builder(getApplicationContext()).build();
                if (!recognizer.isOperational())
                {
                    Toast.makeText(this, "error: "+ recognizer.isOperational(), Toast.LENGTH_SHORT).show();

                }
                else
                {
                    Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                    SparseArray<TextBlock> items = recognizer.detect(frame);
                    StringBuilder sb =  new StringBuilder();
                    //get text from sb until there is no text
                    for (int i =0; i<items.size();i++)
                    {
                        TextBlock myItem = items.valueAt(i);
                        sb.append(myItem.getValue());
                        sb.append("\n");
                    }
                    mResultEt.setText(sb.toString());
                }
            }
            else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE)
            {
                Exception error = result.getError();
                Toast.makeText(this, ""+error, Toast.LENGTH_SHORT).show();
            }
        }
    }

    //handle permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case CAMERA_REQUEST_CODE:
                if (grantResults.length >0)
                {
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean writeStorageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted && writeStorageAccepted)
                    {
                        pickCamera();
                    }
                    else
                    {
                        Toast.makeText(this, "permission denied", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case STORAGE_REQUEST_CODE:
                if (grantResults.length >0)
                {
                    boolean writeStorageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (writeStorageAccepted)
                    {
                        pickGallery();
                    }
                    else
                    {
                        Toast.makeText(this, "permission denied", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
        }

    }

    private void showImageImportDialog() {
        //items to display in dialog
        String[] items = {"Camera","Gallery"};
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        //set title
        dialog.setTitle("Select Image");
        dialog.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0)
                {
                    //camera option clicked
                    if (!checkCameraPermission()){
                        //camera permission not allowed, request it
                        requestCameraPermission();
                    }else
                    {
                        //permission allowed, take picture
                        pickCamera();
                    }
                }
                if (which == 1)
                {
                    //gallery option clicked
                    if (!checkStoragePermission()){
                        //storage permission not allowed, request it
                        requestStoragePermission();
                    }else
                    {
                        //permission allowed, take picture
                        pickGallery();
                    }
                }
            }
        });
        dialog.create().show();
    }

    private void pickGallery() {
        //intent to pick image from gallery
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_GALLERY_CODE);
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this,storagePermission,STORAGE_REQUEST_CODE);
    }

    private boolean checkStoragePermission() {
        Boolean result = ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return result;
    }

    private void pickCamera() {
        //intent to take image from camera, it will also to save to storage to get  high quality image
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE,"New Pic"); //title of the picture
        values.put(MediaStore.Images.Media.DESCRIPTION,"Image to Text"); //description of the image
        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT,image_uri);
        startActivityForResult(cameraIntent, IMAGE_PICK_CAMERA_CODE);

    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this,cameraPermission,CAMERA_REQUEST_CODE);
    }

    private boolean checkCameraPermission() {
        //check camera permission and result
        Boolean result = ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);
        Boolean result1 = ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return result && result1;
    }

    private void initActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setSubtitle("Click + button to insert image");
    }

    private void initPermission() {
        //Camera permission
        cameraPermission = new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE};
        //storage permission
        storagePermission = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
    }

    private void initEvent() {
        mTransBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Yandex translator
                String textTobeTranslate = String.valueOf(mTransEt.getText());
                Log.d( "onClick: ", textTobeTranslate);
                String languagePair = "en-vi";
                Translate(textTobeTranslate,languagePair);

            }
        });
    }

    void Translate(String textToBeTranslated,String languagePair){
        try {
            TranslatorBackgroundTask translatorBackgroundTask = new TranslatorBackgroundTask(context);
            //String result = ""; // Returns the translated text as a String
            String result = translatorBackgroundTask.execute(textToBeTranslated, languagePair).get();
            //String[] output = result.split("\""); //output[9]
            mResultEt.setText(result); // set result in Android Monitor
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

    }
    private String translate(String text) throws Exception {
        // Set the Client ID / Client Secret once per JVM. It is set statically and applies to all services
        Translate.setClientId("longhan"); //Change this
        Translate.setClientSecret("bEYMwOFEljfyGs35IpIkyYcoZnPrt+Q8N49LpY64G8k="); //change


        String translatedText = "";

        translatedText = Translate.execute(text, Language.VIETNAMESE);

        return translatedText;
    }

    private void initUI() {
        mResultEt = findViewById(R.id.resultEt);
        mTransEt = findViewById(R.id.transEt);
        mPreviewIv = findViewById(R.id.imageIv);
        mTransBtn = findViewById(R.id.transBtn);
    }

}
