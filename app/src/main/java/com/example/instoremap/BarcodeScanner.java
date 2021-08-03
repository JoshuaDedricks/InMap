package com.example.instoremap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
public class BarcodeScanner extends AppCompatActivity {
    TextureView cameraBox;
    private int STORAGE_PERMISSION_CODE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.barcodescanner);

        cameraBox = (TextureView) findViewById(R.id.cameraBox);

        if(ContextCompat.checkSelfPermission(BarcodeScanner.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
            Log.d("Permission Status", "All Permissions granted");
        }else if(ActivityCompat.shouldShowRequestPermissionRationale(BarcodeScanner.this, Manifest.permission.CAMERA)){
            Log.d("User should fix this", "They really should");
        }else{
            ActivityCompat.requestPermissions(BarcodeScanner.this, new String[]{Manifest.permission.CAMERA}, STORAGE_PERMISSION_CODE);
        }

        startCamera();

    }

    private void startCamera() {
        CameraX.unbindAll();
        Rational aspectRatio = new Rational(cameraBox.getWidth(), cameraBox.getHeight());
        Size screen = new Size(cameraBox.getWidth(), cameraBox.getHeight());
        PreviewConfig pConfig = new PreviewConfig.Builder().setTargetAspectRatio(aspectRatio).setTargetResolution(screen).build();
        Preview preview = new Preview(pConfig);

        preview.setOnPreviewOutputUpdateListener(
                new Preview.OnPreviewOutputUpdateListener() {
                    @Override
                    public void onUpdated(Preview.PreviewOutput output) {
                        ViewGroup parent = (ViewGroup) cameraBox.getParent();
                        parent.removeView(cameraBox);
                        parent.addView(cameraBox);

                        cameraBox.setSurfaceTexture(output.getSurfaceTexture());
                        updateTransform();
                    }
                }
        );


        ImageCaptureConfig imageCaptureConfig = new ImageCaptureConfig.Builder().setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY).
                setTargetRotation(getWindowManager().getDefaultDisplay().getRotation()).build();
        final ImageCapture imgCap = new ImageCapture(imageCaptureConfig);


        findViewById(R.id.captureCode).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                File imagefile = new File(BarcodeScanner.this.getFilesDir() + "/pc.jpeg");
                imgCap.takePicture(imagefile, new ImageCapture.OnImageSavedListener() {
                    @Override
                    public void onImageSaved(@NonNull File file) {
                        BarcodeScannerOptions options =
                                new BarcodeScannerOptions.Builder()
                                        .setBarcodeFormats(
                                                Barcode.FORMAT_QR_CODE,
                                                Barcode.FORMAT_AZTEC,
                                                Barcode.FORMAT_DATA_MATRIX,
                                                Barcode.FORMAT_UPC_E,
                                                Barcode.FORMAT_UPC_A,
                                                Barcode.FORMAT_ITF,
                                                Barcode.FORMAT_EAN_8,
                                                Barcode.FORMAT_EAN_13,
                                                Barcode.FORMAT_CODABAR,
                                                Barcode.FORMAT_CODE_93,
                                                Barcode.FORMAT_CODE_39,
                                                Barcode.FORMAT_CODE_128
                                        )
                                        .build();

                        InputImage image;

                        com.google.mlkit.vision.barcode.BarcodeScanner scanner = BarcodeScanning.getClient();
                        Log.d("path", String.valueOf(Uri.parse(file.getAbsolutePath())));
                        try {
                            image = InputImage.fromFilePath(BarcodeScanner.this, Uri.parse("/files/pc.jpeg"));
                            Task<List<Barcode>> result = scanner.process(image)
                                    .addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
                                        @Override
                                        public void onSuccess(List<Barcode> barcodes) {
                                            Log.d("Barcode", "barcode has been scanned");
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            // Task failed with an exception
                                            // ...
                                        }
                                    });
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        String msg = "Pic captured at" + file.getAbsolutePath();
                        Toast.makeText(getBaseContext(), msg, Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onError(@NonNull ImageCapture.UseCaseError useCaseError, @NonNull String message, @Nullable Throwable cause) {
                        String msg = "Pic capture failed: " + message;
                        Toast.makeText(getBaseContext(), msg, Toast.LENGTH_LONG).show();

                        if (cause != null) {
                            cause.printStackTrace();
                        }
                    }
                });
            }
        });
        CameraX.bindToLifecycle(this, preview, imgCap);
    }

    private void updateTransform() {
        Matrix mx = new Matrix();
        float w = cameraBox.getMeasuredWidth();
        float h = cameraBox.getMeasuredHeight();

        float cX = w / 2f;
        float cY = h / 2f;

        int rotationDgr;
        int rotation = (int)cameraBox.getRotation();

        switch(rotation) {
            case Surface.ROTATION_0:
                rotationDgr = 0;
                break;
            case Surface.ROTATION_90:
                rotationDgr = 90;
                break;
            case Surface.ROTATION_180:
                rotationDgr = 180;
                break;
            case Surface.ROTATION_270:
                rotationDgr = 270;
                break;
            default:
                return;

        }

        mx.postRotate((float)rotationDgr, cX, cY);
        cameraBox.setTransform(mx);
    }
}