package com.myco.musicplayer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.audiofx.Visualizer;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

public class VisualizerView extends View {

    private byte[] bytes;
    private float[] points;
    private Rect rect = new Rect();
    private Paint paint = new Paint();
    private Visualizer visualizer;

    public VisualizerView(Context context) {
        super(context);
        init();
    }

    public VisualizerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        bytes = null;
        paint.setStrokeWidth(8f);
        paint.setAntiAlias(true);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.primary_green));
    }

    public void setAudioSessionId(int sessionId) {
        if (visualizer != null) {
            visualizer.release();
        }

        visualizer = new Visualizer(sessionId);
        visualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);

        visualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
            @Override
            public void onWaveFormDataCapture(Visualizer visualizer, byte[] bytes, int samplingRate) {
                updateVisualizer(bytes);
            }

            @Override
            public void onFftDataCapture(Visualizer visualizer, byte[] bytes, int samplingRate) {
            }
        }, Visualizer.getMaxCaptureRate() / 2, true, false);

        visualizer.setEnabled(true);
    }

    public void release() {
        if (visualizer != null) {
            visualizer.setEnabled(false);
            visualizer.release();
            visualizer = null;
        }
    }

    public void updateVisualizer(byte[] bytes) {
        this.bytes = bytes;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (bytes == null) {
            return;
        }

        if (points == null || points.length < bytes.length * 4) {
            points = new float[bytes.length * 4];
        }

        rect.set(0, 0, getWidth(), getHeight());

        for (int i = 0; i < bytes.length - 1; i++) {
            points[i * 4] = rect.width() * i / (bytes.length - 1);
            points[i * 4 + 1] = rect.height() / 2
                    + ((byte) (bytes[i] + 128)) * (rect.height() / 2) / 128;
            points[i * 4 + 2] = rect.width() * (i + 1) / (bytes.length - 1);
            points[i * 4 + 3] = rect.height() / 2
                    + ((byte) (bytes[i + 1] + 128)) * (rect.height() / 2) / 128;
        }

        canvas.drawLines(points, paint);
    }
}
