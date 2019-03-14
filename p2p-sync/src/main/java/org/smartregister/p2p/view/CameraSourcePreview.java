package org.smartregister.p2p.view;

import android.Manifest;
import android.content.Context;
import android.support.annotation.RequiresPermission;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;

import com.google.android.gms.vision.CameraSource;

import java.io.IOException;

import timber.log.Timber;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 14/03/2019
 */

public class CameraSourcePreview extends ViewGroup {
    private static final String TAG = CameraSourcePreview.class.getSimpleName();

    private SurfaceView surfaceView;
    private boolean startRequested;
    private boolean surfaceAvailable;
    private CameraSource cameraSource;

    public CameraSourcePreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        startRequested = false;
        surfaceAvailable = false;

        surfaceView = new SurfaceView(context);
        surfaceView.getHolder().addCallback(new SurfaceCallback());
        addView(surfaceView);
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    public void start(CameraSource cameraSource) throws IOException, SecurityException {
        if (cameraSource == null) {
            stop();
        }

        this.cameraSource = cameraSource;

        if (this.cameraSource != null) {
            startRequested = true;
            startIfReady();
        }
    }

    public void stop() {
        if (cameraSource != null) {
            cameraSource.stop();
        }
    }

    public void release() {
        if (cameraSource != null) {
            cameraSource.release();
            cameraSource = null;
        }
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    private void startIfReady() throws IOException, SecurityException {
        if (startRequested && surfaceAvailable) {
            cameraSource.start(surfaceView.getHolder());
            startRequested = false;
        }
    }

    private class SurfaceCallback implements SurfaceHolder.Callback {
        @Override
        public void surfaceCreated(SurfaceHolder surface) {
            surfaceAvailable = true;
            try {
                startIfReady();
            } catch (SecurityException se) {
                Timber.e(se, "Do not have permission to start the camera");
            } catch (IOException e) {
                Timber.e(e, "Could not start camera source.");
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surface) {
            surfaceAvailable = false;
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            //Todo
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {

        for (int i = 0; i < getChildCount(); ++i) {
            getChildAt(i).layout(0, 0, right, bottom);
        }

        try {
            startIfReady();
        } catch (SecurityException se) {
            Timber.e(se, "Do not have permission to start the camera");
        } catch (IOException e) {
            Timber.e(e, "Could not start camera source.");
        }
    }
}