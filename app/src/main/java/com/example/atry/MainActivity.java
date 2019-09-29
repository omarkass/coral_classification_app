package com.example.atry;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.tensorflow.lite.Interpreter;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.webkit.PermissionRequest;
import android.widget.Button;
import android.widget.ImageView;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;

import android.Manifest;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {
    protected Interpreter tflite;
    private int DIM_IMG_SIZE_X = 224;
    private int DIM_IMG_SIZE_Y = 224;
    private int DIM_PIXEL_SIZE = 3;
    private ByteBuffer imgData = null;
    private int[] intValues;
    private float[][] labelProbArray ;
    private Button btn;
    private Button cl;
    private ImageView imageview;
    private TextView result ;
    private static final String IMAGE_DIRECTORY = "/demonuts";
    private int GALLERY = 1 ;
    private Bitmap bmp ;
    private Bitmap img;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Resources res = this.getResources();
        result = (TextView) findViewById(R.id.res) ;
        btn = (Button) findViewById(R.id.btn);
        cl = (Button) findViewById(R.id.cl);
        tflite = new Interpreter(loadModelFile());
        imageview = (ImageView) findViewById(R.id.iv);
        final Drawable inici = imageview.getDrawable() ;
        btn.setOnClickListener(
                new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                                Toast.makeText(getApplicationContext(),"Permission is not granted to acess gallery ",Toast.LENGTH_LONG).show();
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            1);
                }
                else choosePhotoFromGallary();
            }
        });
        final String labels [] = {"Bgrade","bleeding apple","green","multicolour","red","superman","ufo","warpaint"} ;

        cl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (imageview.getDrawable() != inici) {
                    convertBitmapToByteBuffer(img);
                    tflite.run(imgData, labelProbArray);
                    float max = labelProbArray[0][0] ;
                    int index = 0 ;
                    for (int i = 0 ; i < 8 ; i++) {
                        if (labelProbArray[0][i] > max ){
                            max = labelProbArray[0][i] ;
                            index = i ;
                        }
                    }
                    String st = "Result : " + labels[index] ;
                    result.setText(st);
                }
            else Toast.makeText(getApplicationContext(),"you have to choose image first",Toast.LENGTH_SHORT).show();
            }
        });

        intValues = new int[DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y];
        labelProbArray = new float [1][8] ;
        imgData =
                ByteBuffer.allocateDirect(
                        4 * DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE);
        imgData.order(ByteOrder.nativeOrder());

    int shape [] = new int [2] ;
        shape[0] = 784 ;
        shape[1] = 1 ;

           for (int x = 0 ; x < 8 ; x += 1) {
            Log.i("Pixel Value", "Top Right pixel: " + (labelProbArray[0][x]) );
        }
    }


    public void choosePhotoFromGallary() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        startActivityForResult(galleryIntent, GALLERY);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == this.RESULT_CANCELED) {
            return;
        }
        if (requestCode == GALLERY) {
            if (data != null) {
                Uri contentURI = data.getData();
                try {
                    bmp = MediaStore.Images.Media.getBitmap(this.getContentResolver(), contentURI);
                    img = Bitmap.createScaledBitmap(bmp, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y, true);
                    Toast.makeText(MainActivity.this, "Image Saved!", Toast.LENGTH_SHORT).show();
                    imageview.setImageBitmap(bmp);

                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Failed!", Toast.LENGTH_SHORT).show();
                }
            }

        }
    }
    private MappedByteBuffer loadModelFile() {
        try {
            AssetFileDescriptor fileDescriptor = this.getAssets().openFd("coral_model.tflite");

            FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
            FileChannel fileChannel = inputStream.getChannel();
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();
            return  fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        }
        catch (Exception e) {
            Log.e("file read error",e.getMessage()) ;
        }
        return null ;
    }

    private void convertBitmapToByteBuffer(Bitmap bitmap) {
        if (imgData == null) {
            return;
        }
        imgData.rewind();
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        int pixel = 0;
        for (int i = 0; i < DIM_IMG_SIZE_X; ++i) {
            for (int j = 0; j < DIM_IMG_SIZE_Y; ++j) {
                final int val = intValues[pixel++];
                    imgData.putFloat((((val >> 16) & 0xFF)/255f));
                    imgData.putFloat((((val >> 8) & 0xFF))/255f);
                    imgData.putFloat((((val) & 0xFF))/255f);
                }

            }
        }
}



