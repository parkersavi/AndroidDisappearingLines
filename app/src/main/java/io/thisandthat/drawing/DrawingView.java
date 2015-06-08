package io.thisandthat.drawing;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;


import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Parth Savani
 * @since 5/28/15
 */
public class DrawingView extends View {

    public static BlockingDeque<LinePath> linePaths = new LinkedBlockingDeque<LinePath>();
    static Timer timer = new Timer();
    static int ALPHA_STEP = 5;

    private static final int PIXEL_SIZE = 8;

    private Paint mPaint;
    private int mLastX;
    private int mLastY;
    private Canvas mBuffer;
    private Bitmap mBitmap;
    private Paint mBitmapPaint;
    private int mCurrentColor = 0xFFFF0000;
    private Path mPath;
    private Segment mCurrentSegment;
    private Path mChildPath = new Path();

    public DrawingView(Context context) {
        super(context);
        //this timer will start fading out the lines if any are present in the queue
        scheduleTimer();
        mPath = new Path();
        setBackgroundColor(Color.parseColor("#303030"));
        mPaint = paintFromColor(mCurrentColor);

        mBitmapPaint = new Paint(Paint.DITHER_FLAG);
    }


    public void setColor(int color) {
        mCurrentColor = color;
        mPaint.setColor(color);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);

        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mBuffer = new Canvas(mBitmap);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        for(LinePath p : linePaths) {
            canvas.drawPath(p.getPath(), p.getPaint());
        }
        invalidate();
    }

    private Paint paintFromColor(int color) {
        Paint p = new Paint();
        p.setAntiAlias(true);
        p.setDither(true);
        p.setColor(color);
        p.setStrokeWidth(20f);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setShadowLayer(20, 0, 0, color);
        return p;
    }

    private void drawSegment(Segment segment, Paint paint) {
        mChildPath.reset();
        List<Point> points = segment.getPoints();
        Point current = points.get(0);
        mChildPath.moveTo(current.x * PIXEL_SIZE, current.y * PIXEL_SIZE);
        Point next = null;
        for (int i = 1; i < points.size(); ++i) {
            next = points.get(i);
            mChildPath.quadTo(current.x * PIXEL_SIZE, current.y * PIXEL_SIZE, ((next.x + current.x) * PIXEL_SIZE) / 2, ((next.y + current.y) * PIXEL_SIZE) / 2);
            current = next;
        }
        if (next != null) {
            mChildPath.lineTo(next.x * PIXEL_SIZE, next.y * PIXEL_SIZE);
        }
        mBuffer.drawPath(mChildPath, paint);
    }

    private void onTouchStart(float x, float y) {
        mPath.reset();
        mPath.moveTo(x, y);
        mCurrentSegment = new Segment(mCurrentColor);
        mLastX = (int) x / PIXEL_SIZE;
        mLastY = (int) y / PIXEL_SIZE;
        mCurrentSegment.addPoint(mLastX, mLastY);
    }

    private void onTouchMove(float x, float y) {

        int x1 = (int) x / PIXEL_SIZE;
        int y1 = (int) y / PIXEL_SIZE;

        float dx = Math.abs(x1 - mLastX);
        float dy = Math.abs(y1 - mLastY);
        if (dx >= 1 || dy >= 1) {
            mPath.quadTo(mLastX * PIXEL_SIZE, mLastY * PIXEL_SIZE, ((x1 + mLastX) * PIXEL_SIZE) / 2, ((y1 + mLastY) * PIXEL_SIZE) / 2);
            mLastX = x1;
            mLastY = y1;
            mCurrentSegment.addPoint(mLastX, mLastY);
        }
    }

    private void onTouchEnd() {
        mPath.lineTo(mLastX * PIXEL_SIZE, mLastY * PIXEL_SIZE);
        mBuffer.drawPath(mPath, mPaint);
       //we don't need this because we create new objects every time
       // mPath.reset();

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                //create line Path object to be added to a queue
                LinePath linePath = new LinePath();
                mPath = new android.graphics.Path();
                linePath.setPath(mPath);
                linePath.setPaint(paintFromColor(mCurrentColor));
                //let's start drawing
                onTouchStart(x, y);

                //add that to the queue
                linePaths.add(linePath);


                onTouchStart(x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                onTouchMove(x, y);
                break;
            case MotionEvent.ACTION_UP:
                onTouchEnd();
                for (LinePath p : linePaths) {
                   p.setCanAnimate(true);
                }
                break;
        }
        return true;
    }

    /*
      This class stores properties of a path and the path object itself
     */
    class LinePath {
        private Path path;
        private Paint paint;
        private AtomicBoolean canAnimate = new AtomicBoolean(false);

        private int alpha;

        public int getAlpha() {
            return alpha;
        }

        public void setAlpha(int alpha) {
            this.alpha = alpha;
        }

        public Path getPath() {
            return path;
        }

        public void setPath(Path path) {
            this.path = path;
        }

        public Paint getPaint() {
            return paint;
        }

        public void setPaint(Paint paint) {
            this.paint = paint;
        }

        public boolean canAnimate() {
            return canAnimate.get();
        }

        public void setCanAnimate(boolean canAnimate) {
            this.canAnimate.set(canAnimate);
        }

    }

    /*
        This is the method where everything happens
        We take the Path objects from the queue and from the object
        we get the paint object and reduce each line's alpha value every
        time and once the alpha values is less than zero, we remove that
        object from the queue.
        Note: the queue is iterated here and in the onDraw method
        onDraw method iterates through the objects and paints them on the screen
        The path objects removed from the queue below won't be drawn
     */
    public static void scheduleTimer() {
        timer.scheduleAtFixedRate(new TimerTask() {
            private Handler updateUI = new Handler() {
                @Override
                public void dispatchMessage(Message msg) {
                    //iterate through all the paths
                    for (LinePath path : linePaths) {
                        //if we can animate that path
                        if (path.canAnimate()) {
                            //get the alpha from the paint object of that Path
                            int currentAlpha = path.getPaint().getAlpha();
                            //let's reduce the alpha
                            currentAlpha -= ALPHA_STEP;

                            //set the new aplha back in the LinePath object
                            path.setAlpha(currentAlpha);
                            path.getPaint().setAlpha(currentAlpha);

                            //remove the linePath from the queue if the alpha value is 0
                            if (currentAlpha <= 0) {

                                linePaths.remove(path);
                            }
                        }
                    }
                }
            };

            public void run() {
                try {
                    updateUI.sendEmptyMessage(0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 0, 100);
    }




}
