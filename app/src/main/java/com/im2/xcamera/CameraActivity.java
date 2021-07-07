package com.im2.xcamera;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.media.Image;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.util.concurrent.ListenableFuture;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.DisplayOrientedMeteringPointFactory;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.FocusMeteringResult;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.Preview;
import androidx.camera.core.SurfaceOrientedMeteringPointFactory;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.ScaleGestureDetector;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class CameraActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ListenableFuture<FocusMeteringResult> focusMeteringResultListenableFuture;
    private Camera camera;
    private ScaleGestureDetector mScaleGestureDetector;
    private float mScaleFactor = 1.0f;
    private ImageView mImageView;

    SurfaceHolder holder;
    SurfaceView surfaceView;
    Canvas canvas;
    Paint paint;
    int cameraHeight, cameraWidth, xOffset, yOffset, boxWidth, boxHeight;
    Activity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = this;
        setContentView(R.layout.activity_camera);
        previewView = findViewById(R.id.previewView);

        //Create the bounding box
        surfaceView = findViewById(R.id.overlay);
        surfaceView.setZOrderOnTop(true);
        holder = surfaceView.getHolder();
        holder.setFormat(PixelFormat.TRANSPARENT);
        holder.addCallback(this);

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        mScaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindImageAnalysis(cameraProvider);
                    previewView.getPreviewStreamState().observe((LifecycleOwner) activity, value -> {
                        Log.d("CameraX", String.valueOf(value));
                        if(value.toString().equals("STREAMING")){
                            Log.d("CameraX", String.valueOf(value));
                            //adjustCameraFocus();
                            //adjustCameraFocus();
                            //adjustCameraFocus1();
                            adjustCameraFocus2();
                        }
                    });
                    //getTouchCoord();
                    System.out.println("Finished");
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    private void bindImageAnalysis(@NonNull ProcessCameraProvider cameraProvider) {
        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder().setTargetResolution(new Size(1920, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), new ImageAnalysis.Analyzer() {
            @SuppressLint("UnsafeExperimentalUsageError")
            @Override
            public void analyze(@NonNull ImageProxy image) {

                image.close();
            }
        });

        OrientationEventListener orientationEventListener = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int orientation) {
                //updateTransform(orientation);

            }
        };
        orientationEventListener.enable();
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        camera =  cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector,
                imageAnalysis, preview);
        //camera = cameraProvider.bindToLifecycle(this, cameraSelector);
    }

    private void getTouchCoord() {
        previewView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                float x = event.getX();
                float y = event.getY();
                Log.d("XCAMERA", "X : "+event.getX() +" Y : "+ event.getY());
                return true;
            }
        });
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector){
            mScaleFactor *= scaleGestureDetector.getScaleFactor();
            mScaleFactor = Math.max(0.1f,
                    Math.min(mScaleFactor, 10.0f));
            //float zoomR = Objects.requireNonNull(camera.getCameraInfo().getZoomState().getValue()).getZoomRatio();
            //camera.getCameraControl().setZoomRatio(zoomR * mScaleFactor);
            return true;
        }
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        mScaleGestureDetector.onTouchEvent(motionEvent);
        return true;
    }

    private void adjustCameraFocus1() {
        float x = previewView.getWidth()/2;
        float y = (previewView.getHeight() - previewView.getHeight()/4);
        MeteringPoint meteringPoint1 = new SurfaceOrientedMeteringPointFactory(previewView.getWidth(), previewView.getHeight()).createPoint(x, y);
        FocusMeteringAction action = new FocusMeteringAction.Builder(meteringPoint1, FocusMeteringAction.FLAG_AF).setAutoCancelDuration(2, TimeUnit.SECONDS)
                .build();
        focusMeteringResultListenableFuture = camera.getCameraControl().startFocusAndMetering(action);

        focusMeteringResultListenableFuture.addListener(() -> {
            try {
                FocusMeteringResult result = focusMeteringResultListenableFuture.get();
                Log.d("isFocusSuccessful", ""+result.isFocusSuccessful());
                // process the result
            } catch (Exception e) {
            }
        } , ContextCompat.getMainExecutor(this));
    }

    private void adjustCameraFocus() {
        Log.d("adjustCameraFocus ", "adjustCameraFocus");
        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis() + 100;

        float x = previewView.getWidth()/2;
        float y = (previewView.getHeight() - previewView.getHeight()/4);

        Log.d("---", "X "+ x + "Y "+y);
        int metaState = 0;
        MotionEvent event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP, x, y, metaState);
        MeteringPointFactory meteringPointFactory = new DisplayOrientedMeteringPointFactory(this.getDisplay(), camera.getCameraInfo(), previewView.getWidth(), previewView.getHeight());
        MeteringPoint meteringPoint1 = meteringPointFactory.createPoint(event.getX(), event.getY());
        FocusMeteringAction action = new FocusMeteringAction.Builder(meteringPoint1, FocusMeteringAction.FLAG_AF).setAutoCancelDuration(2, TimeUnit.SECONDS)
                .build();
        focusMeteringResultListenableFuture = camera.getCameraControl().startFocusAndMetering(action);

        focusMeteringResultListenableFuture.addListener(() -> {
            try {
                FocusMeteringResult result = focusMeteringResultListenableFuture.get();
                Log.d("isFocusSuccessful", ""+result.isFocusSuccessful());
                // process the result
            } catch (Exception e) {
            }
        } , ContextCompat.getMainExecutor(this));

        float zoomR = Objects.requireNonNull(camera.getCameraInfo().getZoomState().getValue()).getZoomRatio();
        camera.getCameraControl().setZoomRatio(zoomR * 1.2f);
        //animateFocusRing(event.getX(), event.getY());
    }

    private void adjustCameraFocus2() {
        Log.d("CameraX", "adjustCameraFocus2");
        previewView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    MeteringPointFactory meteringPointFactory = previewView.getMeteringPointFactory();
                    MeteringPoint meteringPoint = meteringPointFactory.createPoint(event.getX(), event.getY());
                    FocusMeteringAction action = new FocusMeteringAction.Builder(meteringPoint).build();
                    focusMeteringResultListenableFuture = camera.getCameraControl().startFocusAndMetering(action);

                    focusMeteringResultListenableFuture.addListener(() -> {
                        try {
                            FocusMeteringResult result = focusMeteringResultListenableFuture.get();
                            Log.d("isFocusSuccessful", ""+result.isFocusSuccessful());
                            // process the result
                        } catch (Exception e) {
                            Log.d("CameraX", e.getMessage());
                        }
                    } , ContextCompat.getMainExecutor(activity));
                    return true;
                }
                if(event.getAction() == MotionEvent.ACTION_UP){
                    MeteringPointFactory meteringPointFactory = previewView.getMeteringPointFactory();
                    MeteringPoint meteringPoint = meteringPointFactory.createPoint(event.getX(), event.getY());
                    FocusMeteringAction action = new FocusMeteringAction.Builder(meteringPoint).build();
                    focusMeteringResultListenableFuture = camera.getCameraControl().startFocusAndMetering(action);

                    focusMeteringResultListenableFuture.addListener(() -> {
                        try {
                            FocusMeteringResult result = focusMeteringResultListenableFuture.get();
                            Log.d("isFocusSuccessful", ""+result.isFocusSuccessful());
                            // process the result
                        } catch (Exception e) {
                            Log.d("CameraX", e.getMessage());
                        }
                    } , ContextCompat.getMainExecutor(activity));
                    return true;
                }
                return false;
            }
        });

    }

    /**
     *
     * For drawing the rectangular box
     */
    private void DrawFocusRect(int color) {
        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        int height = previewView.getHeight();
        int width = previewView.getWidth();

        //cameraHeight = height;
        //cameraWidth = width;

        int left, right, top, bottom, diameter;

        diameter = width;
        if (height < width) {
            diameter = height;
        }

        int offset = (int) (0.05 * diameter);
        diameter -= offset;

        canvas = holder.lockCanvas();
        canvas.drawColor(0, PorterDuff.Mode.CLEAR);
        //border's properties
        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(color);
        paint.setStrokeWidth(5);

        left = width / 2 - diameter / 3;
        top = height / 2 - diameter / 3;
        right = width / 2 + diameter / 3;
        bottom = height / 2 + diameter / 3;

        xOffset = left;
        yOffset = top;
        boxHeight = bottom - top;
        boxWidth = right - left;

        left = 5; //x
        top = (height - height/4); //y
        right = left + width - 5;
        bottom = (height);

        //Changing the value of x in diameter/x will change the size of the box ; inversely proportionate to x
        canvas.drawRect(left, top, right, bottom, paint);
        holder.unlockCanvasAndPost(canvas);
    }

    /**
     * Callback functions for the surface Holder
     */

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        //Drawing rectangle
        DrawFocusRect(Color.parseColor("#b3dabb"));
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}