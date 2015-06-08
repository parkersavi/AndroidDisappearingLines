
## Objective
To draw lines (fingerpaint) and makes them fadeout gradually giving the effect of disappearing lines just like you see them in Apple Watch

## What we will achieve:
---

![](/images/this/android/output.gif)


## How it's done
---

**Big Picture**

![](/images/this/android/disappearing_lines.png)


**Step 1:**

Each time we draw a line, we create a new object where we store the attributes of that drawing. 

Our object is called LinePath. We store the following in it

1. **Path (android.graphics.Path)** - x and y coordniates. It encapsulates compound (multiple contour) geometric paths consisting of straight line segments, quadratic curves, and cubic curves

2. **Paint (android.graphics.Paint)** -  holds the style and color information about how to draw geometries, text and bitmaps

3. **canAnimate** - boolean flag to tell us if the object can be animated or not. 

 ```
   
    class LinePath {
        private Path path;
        private Paint paint;
        private AtomicBoolean canAnimate = new AtomicBoolean(false);

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

  ```
  
  
  **Step 2:**
  
  Create a Blocking queue to store our LinePath objects.
   
  ```
  
   public static BlockingDeque<LinePath> linePaths = new LinkedBlockingDeque<LinePath>();
  
  ```
  
  
  **Step 3:**
  Iteration over the queue.
   
  Once the objects are added to the queue, we iterate over all the objects in the queue and draw each line based on the properties stored in LinePath object
    ```
    @Override
    protected void onDraw(Canvas canvas) {
        for(LinePath p : linePaths) {
            canvas.drawPath(p.getPath(), p.getPaint());
        }
        invalidate();
    }
     ```
  
  We now create a timer task that basically iterates over line path objects from the queue and from the object we get the paint object and reduce each line's alpha value slightly every time and once the alpha value is less than zero, we remove that object from the queue and thus that line would no longer be draw in the **onDraw** method
  
```
   
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
```
  
  **Note:** Don't forget to start the timertask 
  
## Output 
  
  ![](/images/this/android/output.gif)

