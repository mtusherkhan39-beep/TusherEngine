package com.azizul.tushereditor.editor;

import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.VelocityTracker;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import androidx.annotation.NonNull;

public class EditorGestures {
    private final TusherEngine engine;
    private final ScaleGestureDetector scaleDetector;
    private final GestureDetector gestureDetector;
    private VelocityTracker velocityTracker;
    private int activeHandle = 0;
    
    private float touchOffsetY = 0;
    private int lastIndex = -1;
    
    private final Handler scrollHandler = new Handler(Looper.getMainLooper());
    private Runnable autoScrollRunnable;
    private float lastRawX, lastRawY;
    
    private final TextSelectionWindow selectionWindow;

    public EditorGestures(TusherEngine engine) {
        this.engine = engine;
        this.selectionWindow = new TextSelectionWindow(engine);
        this.scaleDetector = new ScaleGestureDetector(engine.getContext(), new ScaleListener());
        this.gestureDetector = new GestureDetector(engine.getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(@NonNull MotionEvent e) {
                // ১. পপআপ খোলা থাকলে প্রথমে শুধু পপআপ হাইড হবে
                if (selectionWindow.isShowing()) {
                    selectionWindow.hide();
                    return true;
                }

                // ২. পপআপ না থাকলে এবং সিলেকশন থাকলে, এখন সিলেকশন কাটবে
                if (engine.textSelection.isSelectionActive()) {
                    engine.textSelection.clearSelection();
                    engine.invalidate();
                    return true;
                }

                // ৩. সিলেকশন না থাকলে নতুন কার্সার বসবে
                engine.requestFocus();
                InputMethodManager imm = (InputMethodManager) engine.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(engine, InputMethodManager.SHOW_IMPLICIT);
                engine.setCursorFromTouch(e.getX(), e.getY());
                return true;
            }

            @Override
            public void onLongPress(@NonNull MotionEvent e) {
                if (engine.textSelection.isSelectionActive()) {
                    // সিলেকশন থাকলে লং প্রেস করলে আপনার আঙুলের পজিশনে পপআপ দেখাবে
                    selectionWindow.show(e.getX(), e.getY());
                    engine.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                } else {
                    // সিলেকশন না থাকলে নতুন সিলেকশন শুরু হবে, কিন্তু কোনো মেনু আসবে না
                    engine.textSelection.onLongPress(e.getX(), e.getY());
                    activeHandle = TextSelaction.HANDLE_END; 
                    engine.showMagnifier = true;
                    calculateInitialOffset(e.getY());
                    engine.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                    startAutoScroll();
                }
            }
        });
    }

    private void calculateInitialOffset(float touchY) {
        float lineHeight = engine.defaultTextSize * 1.4f;
        Paint.FontMetrics fm = engine.textPaint.getFontMetrics();
        float baselineOffset = (lineHeight - (fm.descent - fm.ascent)) / 2 - fm.ascent;
        
        int targetIndex = (activeHandle == TextSelaction.HANDLE_START) ? engine.textSelection.selectionStart : engine.textSelection.selectionEnd;
        float[] coords = engine.textSelection.getCursorCoordsCentered(targetIndex, lineHeight, baselineOffset);
        
        float handleRadius = 25f * engine.scaleFactor * 1.5f;
        float handleBaseY = (coords[1] - engine.scrollY) + (lineHeight - baselineOffset) * engine.scaleFactor;
        float handleCenterY = handleBaseY + handleRadius;
        
        touchOffsetY = handleCenterY - touchY;
    }

    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);
        if (velocityTracker == null) velocityTracker = VelocityTracker.obtain();
        velocityTracker.addMovement(event);

        int action = event.getActionMasked();
        lastRawX = event.getX();
        lastRawY = event.getY();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                activeHandle = getClosestHandle(event.getX(), event.getY());
                
                if (!engine.getScroller().isFinished()) engine.getScroller().abortAnimation();
                engine.mActivePointerId = event.getPointerId(0);
                engine.lastTouchX = event.getX();
                engine.lastTouchY = event.getY();
                
                if (activeHandle != 0) {
                    selectionWindow.hide();
                    engine.showMagnifier = true;
                    calculateInitialOffset(event.getY());
                    startAutoScroll();
                }
                break;

            case MotionEvent.ACTION_MOVE:
                int pIndex = event.findPointerIndex(engine.mActivePointerId);
                if (pIndex == -1) break;
                float x = event.getX(pIndex);
                float y = event.getY(pIndex);

                if (activeHandle != 0) {
                    handleMove(x, y);
                } else if (!scaleDetector.isInProgress()) {
                    engine.scrollX -= (x - engine.lastTouchX);
                    engine.scrollY -= (y - engine.lastTouchY);
                    engine.clampScroll();
                    engine.triggerScrollbar();
                }
                engine.lastTouchX = x;
                engine.lastTouchY = y;
                engine.invalidate();
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                stopAutoScroll();
                
                // ACTION_UP এ স্বয়ংক্রিয় মেনু দেখানো বন্ধ করা হয়েছে
                
                activeHandle = 0;
                engine.showMagnifier = false;
                
                if (action == MotionEvent.ACTION_UP && !scaleDetector.isInProgress()) {
                    velocityTracker.computeCurrentVelocity(1000);
                    engine.getScroller().fling((int) engine.scrollX, (int) engine.scrollY, 
                        (int) -velocityTracker.getXVelocity(), (int) -velocityTracker.getYVelocity(), 
                        0, (int) engine.maxScrollX, 0, (int) engine.maxScrollY);
                    engine.postInvalidateOnAnimation();
                }
                if (velocityTracker != null) { velocityTracker.recycle(); velocityTracker = null; }
                engine.invalidate();
                break;
        }
        return true;
    }

    private float[] getHandleScreenCoords(int handleType) {
        float lineHeight = engine.defaultTextSize * 1.4f;
        Paint.FontMetrics fm = engine.textPaint.getFontMetrics();
        float baselineOffset = (lineHeight - (fm.descent - fm.ascent)) / 2 - fm.ascent;
        int targetIndex = (handleType == TextSelaction.HANDLE_START) ? engine.textSelection.selectionStart : engine.textSelection.selectionEnd;
        float[] coords = engine.textSelection.getCursorCoordsCentered(targetIndex, lineHeight, baselineOffset);
        float hX = engine.lineNumberBarWidth * engine.scaleFactor + coords[0] - engine.scrollX;
        float hY = coords[1] - engine.scrollY;
        return new float[]{hX, hY};
    }

    private void startAutoScroll() {
        if (autoScrollRunnable != null) return;
        autoScrollRunnable = new Runnable() {
            @Override
            public void run() {
                if (activeHandle == 0) return;
                float edgeSize = 120f; 
                float maxScrollStep = 20f; 
                boolean scrolled = false;
                if (lastRawY < edgeSize) {
                    float speedFactor = (edgeSize - lastRawY) / edgeSize;
                    engine.scrollY -= maxScrollStep * speedFactor;
                    scrolled = true;
                } else if (lastRawY > engine.getHeight() - edgeSize) {
                    float speedFactor = (lastRawY - (engine.getHeight() - edgeSize)) / edgeSize;
                    engine.scrollY += maxScrollStep * speedFactor;
                    scrolled = true;
                }
                if (lastRawX < (engine.lineNumberBarWidth * engine.scaleFactor) + edgeSize) {
                    float speedFactor = (edgeSize - (lastRawX - engine.lineNumberBarWidth * engine.scaleFactor)) / edgeSize;
                    engine.scrollX -= maxScrollStep * speedFactor;
                    scrolled = true;
                } else if (lastRawX > engine.getWidth() - edgeSize) {
                    float speedFactor = (lastRawX - (engine.getWidth() - edgeSize)) / edgeSize;
                    engine.scrollX += maxScrollStep * speedFactor;
                    scrolled = true;
                }
                if (scrolled) { engine.clampScroll(); handleMove(lastRawX, lastRawY); engine.invalidate(); }
                scrollHandler.postDelayed(this, 16);
            }
        };
        scrollHandler.post(autoScrollRunnable);
    }

    private void stopAutoScroll() {
        scrollHandler.removeCallbacks(autoScrollRunnable);
        autoScrollRunnable = null;
    }

    private int getClosestHandle(float x, float y) {
        if (!engine.textSelection.isSelectionActive()) return 0;
        float lineHeight = engine.defaultTextSize * 1.4f;
        float lineNumWidth = engine.lineNumberBarWidth * engine.scaleFactor;
        float density = engine.getContext().getResources().getDisplayMetrics().density;
        float touchRadius = 60f * density;
        Paint.FontMetrics fm = engine.textPaint.getFontMetrics();
        float baselineOffset = (lineHeight - (fm.descent - fm.ascent)) / 2 - fm.ascent;
        
        float handleSize = 25f * engine.scaleFactor;
        float radius = handleSize * 1.5f;

        float[] sCoords = engine.textSelection.getCursorCoordsCentered(engine.textSelection.selectionStart, lineHeight, baselineOffset);
        float sX = lineNumWidth + sCoords[0] - engine.scrollX;
        float sY = sCoords[1] - engine.scrollY + (lineHeight - baselineOffset) * engine.scaleFactor + (radius * 1.5f);
        double distStart = Math.hypot(x - sX, y - sY);

        float[] eCoords = engine.textSelection.getCursorCoordsCentered(engine.textSelection.selectionEnd, lineHeight, baselineOffset);
        float eX = lineNumWidth + eCoords[0] - engine.scrollX;
        float eY = eCoords[1] - engine.scrollY + (lineHeight - baselineOffset) * engine.scaleFactor + (radius * 1.5f);
        double distEnd = Math.hypot(x - eX, y - eY);

        if (distStart < distEnd && distStart < touchRadius) return TextSelaction.HANDLE_START;
        if (distEnd < touchRadius) return TextSelaction.HANDLE_END;
        return 0;
    }

    private void handleMove(float x, float y) {
        float targetHandleCenterY = y + touchOffsetY;
        float lineHeight = engine.defaultTextSize * 1.4f * engine.scaleFactor;
        float handleRadius = 25f * engine.scaleFactor * 1.5f;
        float adjustedY = targetHandleCenterY - handleRadius - (lineHeight / 2f);
        
        int currentIndex = engine.textSelection.getIndexFromOffset(x, adjustedY);
        if (currentIndex == -1) return;

        if (activeHandle == TextSelaction.HANDLE_START && currentIndex > engine.textSelection.selectionEnd) {
            engine.textSelection.selectionStart = engine.textSelection.selectionEnd;
            activeHandle = TextSelaction.HANDLE_END;
        } else if (activeHandle == TextSelaction.HANDLE_END && currentIndex < engine.textSelection.selectionStart) {
            engine.textSelection.selectionEnd = engine.textSelection.selectionStart;
            activeHandle = TextSelaction.HANDLE_START;
        }

        engine.textSelection.updateHandle(activeHandle, x, adjustedY);
        
        int updatedIndex = (activeHandle == TextSelaction.HANDLE_START) ? engine.textSelection.selectionStart : engine.textSelection.selectionEnd;
        if (updatedIndex != lastIndex) {
            engine.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            lastIndex = updatedIndex;
        }
        updateMagnifierPos(x, adjustedY);
    }

    private void updateMagnifierPos(float x, float y) {
        float lineHeight = engine.defaultTextSize * 1.4f;
        Paint.FontMetrics fm = engine.textPaint.getFontMetrics();
        float baselineOffset = (lineHeight - (fm.descent - fm.ascent)) / 2 - fm.ascent;
        int targetIndex = (activeHandle == TextSelaction.HANDLE_START) ? engine.textSelection.selectionStart : engine.textSelection.selectionEnd;
        float[] coords = engine.textSelection.getCursorCoordsCentered(targetIndex, lineHeight, baselineOffset);
        engine.magnifierX = engine.lineNumberBarWidth * engine.scaleFactor + coords[0] - engine.scrollX;
        engine.magnifierY = coords[1] - engine.scrollY;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        private float lastFocusX, lastFocusY;
        @Override public boolean onScaleBegin(@NonNull ScaleGestureDetector d) { lastFocusX = d.getFocusX(); lastFocusY = d.getFocusY(); return true; }
        @Override public boolean onScale(@NonNull ScaleGestureDetector d) {
            float oldScale = engine.scaleFactor;
            engine.scaleFactor = Math.max(0.5f, Math.min(5.0f, engine.scaleFactor * d.getScaleFactor()));
            if (engine.scaleFactor != oldScale) {
                float ratio = engine.scaleFactor / oldScale;
                engine.scrollX = (engine.scrollX + d.getFocusX() - engine.lineNumberBarWidth * oldScale) * ratio - (d.getFocusX() - engine.lineNumberBarWidth * engine.scaleFactor);
                engine.scrollY = (engine.scrollY + d.getFocusY()) * ratio - d.getFocusY();
                engine.dimensionsNeedsUpdate = true;
            }
            engine.scrollX -= (d.getFocusX() - lastFocusX);
            engine.scrollY -= (d.getFocusY() - lastFocusY);
            lastFocusX = d.getFocusX(); lastFocusY = d.getFocusY();
            engine.clampScroll(); engine.invalidate(); return true;
        }
    }
}
