package com.im2.xcamera;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
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
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.FocusMeteringResult;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.Preview;
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
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class CameraActivity extends AppCompatActivity {

    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ListenableFuture<FocusMeteringResult> focusMeteringResultListenableFuture;
    private TextView textView;
    private Camera camera;
    private ScaleGestureDetector mScaleGestureDetector;
    private float mScaleFactor = 1.0f;
    private ImageView mImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        previewView = findViewById(R.id.previewView);
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        textView = findViewById(R.id.orientation);
        mScaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindImageAnalysis(cameraProvider);
                    //getTouchCoord();
                    adjustCameraFocus();
                    System.out.println("Finished");
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindImageAnalysis(@NonNull ProcessCameraProvider cameraProvider) {
        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder().setTargetResolution(new Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy image) {
                image.close();
            }
        });
        OrientationEventListener orientationEventListener = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int orientation) {
                textView.setText(Integer.toString(orientation));
            }
        };
        orientationEventListener.enable();
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT).build();
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

    private void animateFocusRing(float x, float y) {
        ImageView focusRing = findViewById(R.id.focusRing);

        // Move the focus ring so that its center is at the tap location (x, y)
        float width = focusRing.getWidth();
        float height = focusRing.getHeight();
        focusRing.setX(x - width / 2);
        focusRing.setY(y - height / 2);

        // Show focus ring
        focusRing.setVisibility(View.VISIBLE);
        focusRing.setAlpha(1F);

        // Animate the focus ring to disappear
        focusRing.animate()
                .setStartDelay(500)
                .setDuration(300)
                .alpha(0F)
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animator) {
                        focusRing.setVisibility(View.INVISIBLE);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }

                    // The rest of AnimatorListener's methods.
                });
    }

    private void adjustCameraFocus() {
        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis() + 100;

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;

        Log.d("---", "height "+ height + "height Trans "+height/3 + "width "+ width + "width Trans "+ width/2);

        float x = width/2;
        float y = (height - height/4);

        Log.d("---", "X "+ x + "Y "+y);
        int metaState = 0;
        MotionEvent event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP, x, y, metaState);
        MeteringPointFactory meteringPointFactory = previewView.getMeteringPointFactory();
        MeteringPoint meteringPoint = meteringPointFactory.createPoint(event.getX(), event.getY());
        FocusMeteringAction action = new FocusMeteringAction.Builder(meteringPoint).build();
        focusMeteringResultListenableFuture = camera.getCameraControl().startFocusAndMetering(action);
        animateFocusRing(x, y);
        float zoomR = Objects.requireNonNull(camera.getCameraInfo().getZoomState().getValue()).getZoomRatio();
        camera.getCameraControl().setZoomRatio(zoomR * 2.0f);
//        previewView.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                if(event.getAction() == MotionEvent.ACTION_DOWN){
//                    MeteringPointFactory meteringPointFactory = previewView.getMeteringPointFactory();
//                    MeteringPoint meteringPoint = meteringPointFactory.createPoint(event.getX(), event.getY());
//                    FocusMeteringAction action = new FocusMeteringAction.Builder(meteringPoint).build();
//                    camera.getCameraControl().startFocusAndMetering(action);
//                    return true;
//                }
//                if(event.getAction() == MotionEvent.ACTION_UP){
//                    MeteringPointFactory meteringPointFactory = previewView.getMeteringPointFactory();
//                    MeteringPoint meteringPoint = meteringPointFactory.createPoint(event.getX(), event.getY());
//                    FocusMeteringAction action = new FocusMeteringAction.Builder(meteringPoint).build();
//                    camera.getCameraControl().startFocusAndMetering(action);
//                    return true;
//                }
//                return false;
//            }
//        });
    }
}