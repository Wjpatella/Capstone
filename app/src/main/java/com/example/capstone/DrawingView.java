package com.example.capstone;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DrawingView extends View {
    private Path currentPath = new Path();
    private Paint drawPaint = new Paint();
    private List<Point> currentStroke = new ArrayList<>();
    private List<Path> paths = new ArrayList<>();
    private List<Paint> paints = new ArrayList<>();
    private OnDrawListener listener;
    private DatabaseReference strokesRef; // Firebase reference

    // Firebase Realtime Database root path for strokes
    public static final String STROKES_PATH = "drawings";

    public interface OnDrawListener {
        void onDrawStroke(List<Point> stroke);
    }

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        drawPaint.setColor(Color.BLACK);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeWidth(5);
        drawPaint.setAntiAlias(true);

        // Initialize Firebase
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        strokesRef = database.getReference(STROKES_PATH); // Points to /drawings in Realtime Database
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Draw all previous paths
        for (int i = 0; i < paths.size(); i++) {
            canvas.drawPath(paths.get(i), paints.get(i));
        }

        // Draw the current path being drawn
        canvas.drawPath(currentPath, drawPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        Point point = new Point((int) x, (int) y);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                currentPath.moveTo(x, y);
                currentStroke.add(point);
                return true;
            case MotionEvent.ACTION_MOVE:
                currentPath.lineTo(x, y);
                currentStroke.add(point);
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                // Add the current path and paint to the lists
                paths.add(new Path(currentPath));
                paints.add(new Paint(drawPaint));

                if (listener != null) listener.onDrawStroke(currentStroke);

                // Send stroke to Firebase
                sendStrokeToFirebase(currentStroke, drawPaint.getColor(), drawPaint.getStrokeWidth());

                // Clear the current path and stroke
                currentPath.reset();
                currentStroke.clear();
                return true;
        }
        return false;
    }

    // Listener to notify other components
    public void setOnDrawListener(OnDrawListener listener) {
        this.listener = listener;
    }

    // Add stroke (e.g., received from Firebase)
    public void addStroke(List<Point> stroke, int color, float strokeWidth) {
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(strokeWidth);
        paint.setAntiAlias(true);

        Path newPath = new Path();
        for (int i = 0; i < stroke.size(); i++) {
            Point point = stroke.get(i);
            if (i == 0) {
                newPath.moveTo(point.x, point.y);
            } else {
                newPath.lineTo(point.x, point.y);
            }
        }

        paths.add(newPath);
        paints.add(paint);
        invalidate();
    }

    // Send a stroke to Firebase Realtime Database
    private void sendStrokeToFirebase(List<Point> stroke, int color, float strokeWidth) {
        String key = strokesRef.push().getKey(); // Unique key for each stroke

        Map<String, Object> strokeData = new HashMap<>();
        strokeData.put("points", stroke);
        strokeData.put("color", color);
        strokeData.put("strokeWidth", strokeWidth);

        strokesRef.child(key).setValue(strokeData);
    }

    // Set paint color
    public void setPaintColor(int color) {
        drawPaint.setColor(color);
    }

    // Set stroke width
    public void setStrokeWidth(float width) {
        drawPaint.setStrokeWidth(width);
    }

    // Clear all drawings
    public void clearDrawing() {
        paths.clear();
        paints.clear();
        invalidate();
    }
}