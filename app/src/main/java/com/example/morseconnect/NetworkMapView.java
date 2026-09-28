package com.example.morseconnect;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class NetworkMapView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int correct = 0;

    public NetworkMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public void setCorrect(int value) {
        correct = Math.max(0, Math.min(20, value));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();

        float[][] nodes = {
                {w * 0.17f, h * 0.24f},
                {w * 0.83f, h * 0.24f},
                {w * 0.50f, h * 0.50f},
                {w * 0.17f, h * 0.78f},
                {w * 0.83f, h * 0.78f}
        };

        int[][] links = {
                {0, 2}, {1, 2}, {2, 3}, {2, 4}, {0, 1}, {3, 4}
        };

        int activeNodes = correct == 20 ? 5 : correct / 4;

        // Connections
        for (int i = 0; i < links.length; i++) {
            int a = links[i][0];
            int b = links[i][1];

            boolean active = a < activeNodes && b < activeNodes;

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(active ? 3.2f : 1.7f);
            paint.setColor(active ? 0xFF35F4F1 : 0xFF3B4380);
            paint.setStrokeCap(Paint.Cap.ROUND);

            if (active) {
                paint.setShadowLayer(9f, 0f, 0f, 0xAA20EFFF);
            } else {
                paint.clearShadowLayer();
            }

            canvas.drawLine(
                    nodes[a][0], nodes[a][1],
                    nodes[b][0], nodes[b][1],
                    paint
            );

            // Small signal dashes along inactive links
            if (!active) {
                paint.clearShadowLayer();
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(0xFF7377B2);

                float mx = (nodes[a][0] + nodes[b][0]) / 2f;
                float my = (nodes[a][1] + nodes[b][1]) / 2f;
                canvas.drawCircle(mx, my, 2.4f, paint);
            }
        }

        // Stations
        for (int i = 0; i < nodes.length; i++) {
            float x = nodes[i][0];
            float y = nodes[i][1];
            boolean active = i < activeNodes;

            paint.clearShadowLayer();
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(active ? 0xFF164D72 : 0xFF29245F);
            canvas.drawCircle(x, y, 31f, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(active ? 2.8f : 1.8f);
            paint.setColor(active ? 0xFF32F3F0 : 0xFF59639B);

            if (active) {
                paint.setShadowLayer(14f, 0f, 0f, 0xFF00EFFF);
            }

            canvas.drawCircle(x, y, 31f, paint);
            paint.clearShadowLayer();

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(active ? 0xFF71FFFF : 0xFF68709D);

            if (active) {
                canvas.drawCircle(x, y, 8f, paint);
                paint.setShadowLayer(12f, 0f, 0f, 0xFF00FFFF);
                canvas.drawCircle(x, y, 4f, paint);
                paint.clearShadowLayer();
            } else {
                canvas.drawCircle(x, y, 5f, paint);
            }
        }
    }
}