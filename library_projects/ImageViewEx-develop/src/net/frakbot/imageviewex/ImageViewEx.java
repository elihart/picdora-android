package net.frakbot.imageviewex;

import java.io.InputStream;
import java.lang.reflect.Method;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

/**
 * Extension of the ImageView that handles any kind of image already supported
 * by ImageView, plus animated GIF images.
 * <p/>
 * <b>WARNING:</b> due to Android limitations, the android:adjustViewBounds
 * attribute is ignored on API levels < 16 (Jelly Bean 4.1). Use our own
 * adjustViewBounds attribute to obtain the same behaviour!
 *
 * @author Sebastiano Poggi, Francesco Pontillo
 */
@SuppressWarnings({"deprecation"})
public class ImageViewEx extends ImageView {

    private static final String TAG = ImageViewEx.class.getSimpleName();

    private static boolean mCanAlwaysAnimate = true;
    private float mScale = -1;
    private boolean mAdjustViewBounds = false;

    private static final int IMAGE_SOURCE_UNKNOWN = -1;
    private static final int IMAGE_SOURCE_RESOURCE = 0;
    private static final int IMAGE_SOURCE_DRAWABLE = 1;
    private static final int IMAGE_SOURCE_BITMAP = 2;
    private static final int IMAGE_SOURCE_GIF = 2;

    @SuppressWarnings("unused")
    private int mImageSource;

    // Used by the fixed size optimizations
    private boolean mIsFixedSize = false;
    private boolean mBlockLayout = false;

    private BitmapFactory.Options mOptions;
    private int mOverriddenDensity = -1;
    private static int mOverriddenClassDensity = -1;

    private int mMaxHeight, mMaxWidth;

    private float mGifAspectRatio;
    private Movie mGif;
    private double mGifStartTime;
    private int mFrameDuration = 67;
    private final Handler mHandler = new Handler();
    private Thread mUpdater;

    private ImageAlign mImageAlign = ImageAlign.NONE;

    private final DisplayMetrics mDm;
    private final SetDrawableRunnable mSetDrawableRunnable = new SetDrawableRunnable();
    private final SetGifRunnable mSetGifRunnable = new SetGifRunnable();
    private ScaleType mScaleType;

    protected Drawable mEmptyDrawable = new ColorDrawable(0x00000000);
    protected FillDirection mFillDirection = FillDirection.NONE;
    
    // added by Eli
    private static int mScreenHeight = 1280;
    private static int mScreenWidth = 720;

    ///////////////////////////////////////////////////////////
    ///                  CONSTRUCTORS                       ///
    ///////////////////////////////////////////////////////////

    /**
     * Creates an instance for the class.
     *
     * @param context The context to instantiate the object for.
     */
    public ImageViewEx(Context context) {
        super(context);
        mDm = context.getResources().getDisplayMetrics();
    }

    /**
     * Creates an instance for the class and initializes it with a given image.
     *
     * @param context The context to initialize the instance into.
     * @param src     InputStream containing the GIF to view.
     */
    public ImageViewEx(Context context, InputStream src) {
        super(context);
        mGif = Movie.decodeStream(src);
        mDm = context.getResources().getDisplayMetrics();
    }

    /**
     * Creates an instance for the class.
     *
     * @param context The context to initialize the instance into.
     * @param attrs   The parameters to initialize the instance with.
     */
    public ImageViewEx(Context context, AttributeSet attrs) {
        super(context, attrs);
        mDm = context.getResources().getDisplayMetrics();

        TypedArray a = context.obtainStyledAttributes(attrs,
                                                      R.styleable.ImageViewEx, 0, 0);

        if (a.hasValue(R.styleable.ImageViewEx_adjustViewBounds)) {
            // Prioritize our own adjustViewBounds
            setAdjustViewBounds(a.getBoolean(R.styleable.ImageViewEx_adjustViewBounds, false));
        }
        else {
            // Fallback strategy: try to use ImageView's own adjustViewBounds
            // attribute value
            if (Build.VERSION.SDK_INT >= 16) {
                // The ImageView#getAdjustViewBounds() method only exists from
                // API Level 16+, for some reason.
                try {
                    Method m = super.getClass().getMethod("getAdjustViewBounds");
                    mAdjustViewBounds = (Boolean) m.invoke(this);
                }
                catch (Exception ignored) {
                }
            }
        }

        if (a.hasValue(R.styleable.ImageViewEx_fillDirection)) {
            setFillDirection(a.getInt(R.styleable.ImageViewEx_fillDirection, 0));
        }

        if (a.hasValue(R.styleable.ImageViewEx_emptyDrawable)) {
            setEmptyDrawable(a.getDrawable(R.styleable.ImageViewEx_emptyDrawable));
        }

        a.recycle();
    }

    /**
     * Creates an instance for the class and initializes it with a provided GIF.
     *
     * @param context The context to initialize the instance into.
     * @param src     The byte array containing the GIF to view.
     */
    public ImageViewEx(Context context, byte[] src) {
        super(context);
        mGif = Movie.decodeByteArray(src, 0, src.length);
        mDm = context.getResources().getDisplayMetrics();
    }

    /**
     * Creates an instance for the class and initializes it with a provided GIF.
     *
     * @param context Il contesto in cui viene inizializzata l'istanza.
     * @param src     The path of the GIF file to view.
     */
    public ImageViewEx(Context context, String src) {
        super(context);
        mGif = Movie.decodeFile(src);
        mDm = context.getResources().getDisplayMetrics();
    }

    ///////////////////////////////////////////////////////////
    ///                 PUBLIC SETTERS                      ///
    ///////////////////////////////////////////////////////////

    /** Initalizes the inner variable describing the kind of resource attached to the ImageViewEx. */
    public void initializeDefaultValues() {
        if (isPlaying()) stop();
        mGif = null;
        setTag(null);
        mImageSource = IMAGE_SOURCE_UNKNOWN;
    }


    /**
     * Sets the image from a byte array. The actual image-setting is
     * called on a worker thread because it can be pretty CPU-consuming.
     *
     * @param src The byte array containing the image to set into the ImageViewEx.
     */
    public void setSource(final byte[] src) {
        if (src != null) {
            final ImageViewEx thisImageView = this;
            setImageDrawable(mEmptyDrawable);
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    thisImageView.setSourceBlocking(src);
                }
            });
            t.setPriority(Thread.MIN_PRIORITY);
            t.setName("ImageSetter@" + hashCode());
            t.run();
        }
    }

    /**
     * Sets the image from a byte array in a blocking, CPU-consuming way.
     * Will handle itself referring back to the UI thread when needed.
     *
     * @param src The byte array containing the image to set into the ImageViewEx.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public void setSourceBlocking(final byte[] src) {
        if (src == null) {
            try {
                stop();
                mGif = null;
                setTag(null);
            }
            catch (Throwable ignored) {
            }
            return;
        }

        Movie gif = null;

        // If the animation is not requested
        // decoding into a Movie is pointless (read: expensive)
        if (internalCanAnimate()) {
            gif = Movie.decodeByteArray(src, 0, src.length);
        }

        // If gif is null, it's probably not a gif
        if (gif == null || !internalCanAnimate()) {

            // If not a gif and if on Android 3+, enable HW acceleration
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                setLayerType(View.LAYER_TYPE_HARDWARE, null);
            }

            // Sets the image as a regular Drawable
            setTag(null);
            
            

            final Drawable d = Converters.byteArrayToDrawable(src, mOptions, getContext(), mScreenWidth, mScreenHeight);

            // We need to run this on the UI thread
            stopLoading();
            mSetDrawableRunnable.setDrawable(d);
            mHandler.post(mSetDrawableRunnable);
            mGifAspectRatio = -1;
        }
        else {
            // Disables the HW acceleration when viewing a GIF on Android 3+
            if (Build.VERSION.SDK_INT >= 11) {
                setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            }

            // We need to run this on the UI thread
            stopLoading();
            mSetGifRunnable.setGif(gif);
            mHandler.post(mSetGifRunnable);
            mGifAspectRatio = (float) gif.width() / gif.height();
        }
    }

    /** {@inheritDoc} */
    public void setImageResource(int resId) {
        initializeDefaultValues();
        stopLoading();
        stop();
        super.setImageResource(resId);
        mImageSource = IMAGE_SOURCE_RESOURCE;
        mGif = null;
    }

    /** {@inheritDoc} */
    public void setImageDrawable(Drawable drawable) {
        blockLayoutIfPossible();
        initializeDefaultValues();
        stopLoading();
        stop();
        super.setImageDrawable(drawable);
        mBlockLayout = false;
        mGif = null;
        mImageSource = IMAGE_SOURCE_DRAWABLE;
    }

    /** {@inheritDoc} */
    public void setImageBitmap(Bitmap bm) {
        initializeDefaultValues();
        stopLoading();
        stop();
        super.setImageBitmap(bm);
        mImageSource = IMAGE_SOURCE_BITMAP;
        mGif = null;
    }

    /** {@inheritDoc} */
    @Override
    public void setScaleType(ScaleType scaleType) {
        super.setScaleType(scaleType);
    }

    /**
     * Sets the fill direction for the image. This is used
     * in conjunction with {@link #setAdjustViewBounds(boolean)}.
     * If <code>adjustViewBounds</code> is not already enabled,
     * it will be automatically enabled by setting the direction
     * to anything other than {@link FillDirection#NONE}.
     *
     * @param direction The fill direction.
     */
    public void setFillDirection(FillDirection direction) {
        if (direction != mFillDirection) {
            mFillDirection = direction;

            if (mFillDirection != FillDirection.NONE && !mAdjustViewBounds) {
                setAdjustViewBounds(true);
            }

            requestLayout();
        }
    }

    /**
     * Private helper for
     * {@link #setFillDirection(net.frakbot.imageviewex.ImageViewEx.FillDirection)}.
     *
     * @param direction The direction integer. 0 = NONE, 1 = HORIZONTAL,
     *                  2 = VERTICAL.
     */
    private void setFillDirection(int direction) {
        FillDirection fd;

        switch (direction) {
            case 1:
                fd = FillDirection.HORIZONTAL;
                break;
            case 2:
                fd = FillDirection.VERTICAL;
                break;
            default:
                fd = FillDirection.NONE;
        }

        setFillDirection(fd);
    }

    /**
     * Sets the duration, in milliseconds, of each frame during the GIF animation.
     * It is the refresh period.
     *
     * @param duration The duration, in milliseconds, of each frame.
     */
    public void setFramesDuration(int duration) {
        if (duration < 1) {
            throw new IllegalArgumentException
                ("Frame duration can't be less or equal than zero.");
        }

        mFrameDuration = duration;
    }

    /**
     * Sets the number of frames per second during the GIF animation.
     *
     * @param fps The fps amount.
     */
    public void setFPS(float fps) {
        if (fps <= 0.0) {
            throw new IllegalArgumentException
                ("FPS can't be less or equal than zero.");
        }

        mFrameDuration = Math.round(1000f / fps);
    }

    /**
     * Sets a density for every image set to any {@link ImageViewEx}.
     * If a custom density level is set for a particular instance of {@link ImageViewEx},
     * this will be ignored.
     *
     * @param classLevelDensity the density to apply to every instance of {@link ImageViewEx}.
     */
    public static void setClassLevelDensity(int classLevelDensity) {
        mOverriddenClassDensity = classLevelDensity;
    }

    /**
     * Assign an Options object to this {@link ImageViewEx}. Those options
     * are used internally by the {@link ImageViewEx} when decoding the
     * image. This may be used to prevent the default behavior that loads all
     * images as mdpi density.
     *
     * @param options The BitmapFactory.Options used to decode the images.
     */
    public void setOptions(BitmapFactory.Options options) {
        mOptions = options;
    }

    /**
     * Programmatically overrides this view's density.
     * The new density will be set on the next {@link #onMeasure(int, int)}.
     *
     * @param fixedDensity the new density the view has to use.
     */
    public void setDensity(int fixedDensity) {
        mOverriddenDensity = fixedDensity;
    }

    /**
     * Removes the class level density for {@link ImageViewEx}.
     *
     * @see ImageViewEx#setClassLevelDensity(int)
     */
    public static void removeClassLevelDensity() {
        setClassLevelDensity(-1);
    }

    /**
     * Class method.
     * Sets the mCanAlwaysAnimate value. If it is true, {@link #canAnimate()} will be
     * triggered, determining if the animation can be played in that particular instance of
     * {@link ImageViewEx}. If it is false, {@link #canAnimate()} will never be triggered
     * and GIF animations will never start.
     * {@link #mCanAlwaysAnimate} defaults to true.
     *
     * @param mCanAlwaysAnimate boolean, true to always animate for every instance of
     *                          {@link ImageViewEx}, false if you want to perform the
     *                          decision method {@link #canAnimate()} on every
     *                          {@link #setSource(byte[])} call.
     */
    public static void setCanAlwaysAnimate(boolean mCanAlwaysAnimate) {
        ImageViewEx.mCanAlwaysAnimate = mCanAlwaysAnimate;
    }
    
    /**
     * Set the target screen size so that images can be scaled down.
     * Added by eli
     * @param width
     * @param height
     */
    public static void setScreenSize(int width, int height){
    	mScreenWidth = width;
    	mScreenHeight = height;
    }

    /**
     * Sets a value indicating wether the image is considered as having a fixed size.
     * This will enable an optimization when assigning images to the ImageViewEx, but
     * has to be used sparingly or it may cause artifacts if the image isn't really
     * fixed in size.
     * <p/>
     * An example of usage for this optimization is in ListViews, where items images
     * are supposed to be fixed size, and this enables buttery smoothness.
     * <p/>
     * See: https://plus.google.com/u/0/113058165720861374515/posts/iTk4PjgeAWX
     */
    public void setIsFixedSize(boolean fixedSize) {
        mIsFixedSize = fixedSize;
    }

    /**
     * Sets a new ImageAlign value and redraws the View.
     * If the ImageViewEx has a ScaleType set too, this
     * will override it!
     *
     * @param align The new ImageAlign value.
     *
     * @deprecated Use setScaleType(ScaleType.FIT_START)
     *             and setScaleType(ScaleType.FIT_END) instead.
     */
    public void setImageAlign(ImageAlign align) {
        if (align != mImageAlign) {
            mImageAlign = align;
            invalidate();
        }
    }

    /**
     * Sets the drawable used as "empty". Note that this
     * is not automatically assigned by {@link ImageViewEx}
     * but is used by descendants such as {@link ImageViewNext}.
     *
     * @param d The "empty" drawable
     */
    public void setEmptyDrawable(Drawable d) {
        mEmptyDrawable = d;
    }

    @Override
    public void setAdjustViewBounds(boolean adjustViewBounds) {
        if (mFillDirection != FillDirection.NONE) {
            // Just in case, shouldn't be ever necessary
            if (!mAdjustViewBounds) {
                mAdjustViewBounds = true;
                super.setAdjustViewBounds(true);
            }

            return;
        }

        mAdjustViewBounds = adjustViewBounds;
        super.setAdjustViewBounds(adjustViewBounds);
    }

    ///////////////////////////////////////////////////////////
    ///                 PUBLIC GETTERS                      ///
    ///////////////////////////////////////////////////////////

    /** Disables density ovverriding. */
    public void dontOverrideDensity() {
        mOverriddenDensity = -1;
    }

    /**
     * Returns a boolean indicating if an animation is currently playing.
     *
     * @return true if animating, false otherwise.
     */
    public boolean isPlaying() {
        return mUpdater != null && mUpdater.isAlive();
    }

    /**
     * Returns a boolean indicating if the instance was initialized and if
     * it is ready for playing the animation.
     *
     * @return true if the instance is ready for playing, false otherwise.
     */
    public boolean canPlay() {
        return mGif != null;
    }

    /**
     * Class method.
     * Returns the mCanAlwaysAnimate value. If it is true, {@link #canAnimate()} will be
     * triggered, determining if the animation can be played in that particular instance of
     * {@link ImageViewEx}. If it is false, {@link #canAnimate()} will never be triggered
     * and GIF animations will never start.
     * {@link #mCanAlwaysAnimate} defaults to true.
     *
     * @return boolean, true to see if this instance can be animated by calling
     *         {@link #canAnimate()}, if false, animations will never be triggered and
     *         {@link #canAnimate()} will never be evaluated for this instance.
     */
    public static boolean canAlwaysAnimate() {
        return mCanAlwaysAnimate;
    }

    /**
     * This method should be overridden with your custom implementation. By default,
     * it always returns {@code <code>true</code>}.
     * <p/>
     * <p>This method decides whether animations can be started for this instance of
     * {@link ImageViewEx}. Still, if {@link #canAlwaysAnimate()} equals
     * {@code <code>false</code>} this method will never be called for all of the
     * instances of {@link ImageViewEx}.
     *
     * @return {@code <code>true</code>} if it can animate the current instance of
     *         {@link ImageViewEx}, false otherwise.
     * @see {@link #setCanAlwaysAnimate(boolean)} to set the predefined class behavior
     *      in regards to animations.
     */
    public boolean canAnimate() {
        return true;
    }

    /**
     * Gets the frame duration, in milliseconds, of each frame during the GIF animation.
     * It is the refresh period.
     *
     * @return The duration, in milliseconds, of each frame.
     */
    public int getFramesDuration() {
        return mFrameDuration;
    }

    /**
     * Gets the number of frames per second during the GIF animation.
     *
     * @return The fps amount.
     */
    public float getFPS() {
        return 1000.0f / mFrameDuration;
    }

    /**
     * Gets the current scale value.
     *
     * @return Returns the scale value for this ImageViewEx.
     */
    public float getScale() {
        float targetDensity = getContext().getResources().getDisplayMetrics().densityDpi;
        float displayThisDensity = getDensity();
        mScale = targetDensity / displayThisDensity;
        if (mScale < 0.1f) mScale = 0.1f;
        if (mScale > 5.0f) mScale = 5.0f;
        return mScale;
    }

    /**
     * Gets the fill direction for this ImageViewEx.
     *
     * @return Returns the fill direction.
     */
    public FillDirection getFillDirection() {
        return mFillDirection;
    }

    /**
     * Gets the drawable used as "empty" state.
     *
     * @return Returns the drawable used ad "empty".
     */
    public Drawable getEmptyDrawable() {
        return mEmptyDrawable;
    }

    /**
     * Checks whether the class level density has been set.
     *
     * @return true if it has been set, false otherwise.
     * @see ImageViewEx#setClassLevelDensity(int)
     */
    public static boolean isClassLevelDensitySet() {
        return mOverriddenClassDensity != -1;
    }

    /**
     * Gets the class level density has been set.
     *
     * @return int, the class level density
     * @see ImageViewEx#setClassLevelDensity(int)
     */
    public static int getClassLevelDensity() {
        return mOverriddenClassDensity;
    }

    /**
     * Gets the set density of the view, given by the screen density or by value
     * overridden with {@link #setDensity(int)}.
     * If the density was not overridden and it can't be retrieved by the context,
     * it simply returns the DENSITY_HIGH constant.
     *
     * @return int representing the current set density of the view.
     */
    public int getDensity() {
        int density;

        // If a custom instance density was set, set the image to this density
        if (mOverriddenDensity > 0) {
            density = mOverriddenDensity;
        }
        else if (isClassLevelDensitySet()) {
            // If a class level density has been set, set every image to that density
            density = getClassLevelDensity();
        }
        else {
            // If the instance density was not overridden, get the one from the display
            DisplayMetrics metrics = new DisplayMetrics();

            if (!(getContext() instanceof Activity)) {
                density = DisplayMetrics.DENSITY_HIGH;
            }
            else {
                Activity activity = (Activity) getContext();
                activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
                density = metrics.densityDpi;
            }
        }

        return density;
    }

    /**
     * Sets a value indicating wether the image is considered as having a fixed size.
     * See {@link #setIsFixedSize(boolean)} for further details.
     */
    public boolean getIsFixedSize() {
        return mIsFixedSize;
    }
    
    /**
     * If a gif is loaded get the aspect ratio of the gif. Otherwise return -1
     */
    public float getGifAspectRatio(){
    	return mGifAspectRatio;
    }

    /**
     * Returns the current ImageAlign setting.
     *
     * @return Returns the current ImageAlign setting.
     * @deprecated Use setScaleType(ScaleType.FIT_START)
     *             and setScaleType(ScaleType.FIT_END) instead.
     */
    public ImageAlign getImageAlign() {
        return mImageAlign;
    }

    ///////////////////////////////////////////////////////////
    ///                   PUBLIC METHODS                    ///
    ///////////////////////////////////////////////////////////

    /**
     * Starts playing the GIF, if it hasn't started yet.
     * FPS defaults to 15..
     */
    public void play() {
        // Do something if the animation hasn't started yet
        if (mUpdater == null || !mUpdater.isAlive()) {
            // Check id the animation is ready
            if (!canPlay()) {
                throw new IllegalStateException
                    ("Animation can't start before a GIF is loaded.");
            }

            // Initialize the thread and start it
            mUpdater = new Thread() {

                @Override
                public void run() {

                    // Infinite loop: invalidates the View.
                    // Stopped when the thread is stopped or interrupted.
                    while (mUpdater != null && !mUpdater.isInterrupted()) {

                        mHandler.post(new Runnable() {
                            public void run() {
                                invalidate();
                            }
                        });

                        // The thread sleeps until the next frame
                        try {
                            Thread.sleep(mFrameDuration);
                        }
                        catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            };

            mUpdater.start();
        }
    }

    /** Pause playing the GIF, if it has started. */
    public void pause() {
        // If the animation has started
        if (mUpdater != null && mUpdater.isAlive()) {
            mUpdater.suspend();
        }
    }

    /** Stops playing the GIF, if it has started. */
    public void stop() {
        // If the animation has started
        if (mUpdater != null && mUpdater.isAlive() && canPlay()) {
            mUpdater.interrupt();
            mGifStartTime = 0;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void requestLayout() {
        if (!mBlockLayout) {
            super.requestLayout();
        }
    }

    @Override
    public void setMaxHeight(int maxHeight) {
        super.setMaxHeight(maxHeight);
        mMaxHeight = maxHeight;
    }

    @Override
    public void setMaxWidth(int maxWidth) {
        super.setMaxWidth(maxWidth);
        mMaxWidth = maxWidth;
    }

    ///////////////////////////////////////////////////////////
    ///                  EVENT HANDLERS                     ///
    ///////////////////////////////////////////////////////////

    /**
     * Draws the control
     *
     * @param canvas The canvas to drow onto.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        if (mGif != null) {
            long now = android.os.SystemClock.uptimeMillis();

            // first time	
            if (mGifStartTime == 0) {
                mGifStartTime = now;
            }

            int dur = mGif.duration();
            if (dur == 0) {
                dur = 1000;
            }
            int relTime = (int) ((now - mGifStartTime) % dur);
            mGif.setTime(relTime);
            int saveCnt = canvas.save(Canvas.MATRIX_SAVE_FLAG);

            canvas.scale(mScale, mScale);

            float[] gifDrawParams = applyScaleType(canvas);

            mGif.draw(canvas, gifDrawParams[0], gifDrawParams[1]);

            if (mImageAlign != ImageAlign.NONE) {
                // We have an alignment override.
                // Note: at the moment we only have TOP as custom alignment,
                // so the code here is simplified. Will need refactoring
                // if other custom alignments are implemented further on.

                // ImageAlign.TOP: align top edge with the View

                canvas.translate(0.0f, calcTopAlignYDisplacement());
            }

            canvas.restoreToCount(saveCnt);
        }
        else {
            // Reset the original scale type
            super.setScaleType(getScaleType());

            if (mImageAlign == ImageAlign.NONE) {
                // Everything is normal when there is no alignment override
                super.onDraw(canvas);
            }
            else {
                // We have an alignment override.
                // Note: at the moment we only have TOP as custom alignment,
                // so the code here is simplified. Will need refactoring
                // if other custom alignments are implemented further on.

                // ImageAlign.TOP: scaling forced to CENTER_CROP, align top edge with the View
                setScaleType(ScaleType.CENTER_CROP);

                int saveCnt = canvas.save(Canvas.MATRIX_SAVE_FLAG);
                canvas.translate(0.0f, calcTopAlignYDisplacement());

                super.onDraw(canvas);

                canvas.restoreToCount(saveCnt);
            }
        }
    }

    /**
     * Applies the scale type of the ImageViewEx to the GIF.
     * Use the returned value to draw the GIF and calculate
     * the right y-offset, if any has to be set.
     *
     * @param canvas The {@link Canvas} to apply the {@link ScaleType} to.
     *
     * @return A float array containing, for each position:
     *         - 0 The x position of the gif
     *         - 1 The y position of the gif
     *         - 2 The scaling applied to the y-axis
     */
    private float[] applyScaleType(Canvas canvas) {
        // Get the current dimensions of the view and the gif
        float vWidth = getWidth();
        float vHeight = getHeight();
        float gWidth = mGif.width() * mScale;
        float gHeight = mGif.height() * mScale;

        // Disable the default scaling, it can mess things up
        if (mScaleType == null) {
            mScaleType = getScaleType();
            setScaleType(ScaleType.MATRIX);
        }

        float x = 0;
        float y = 0;
        float s = 1;

        switch (mScaleType) {
            case CENTER:
                /* Center the image in the view, but perform no scaling. */
                x = (vWidth - gWidth) / 2 / mScale;
                y = (vHeight - gHeight) / 2 / mScale;
                break;

            case CENTER_CROP:
                /*
                 * Scale the image uniformly (maintain the image's aspect ratio)
        		 * so that both dimensions (width and height) of the image will
        		 * be equal to or larger than the corresponding dimension of the
        		 * view (minus padding). The image is then centered in the view.
        		 */
                float minDimensionCenterCrop = Math.min(gWidth, gHeight);
                if (minDimensionCenterCrop == gWidth) {
                    s = vWidth / gWidth;
                }
                else {
                    s = vHeight / gHeight;
                }
                x = (vWidth - gWidth * s) / 2 / (s * mScale);
                y = (vHeight - gHeight * s) / 2 / (s * mScale);
                canvas.scale(s, s);
                break;

            case CENTER_INSIDE:
        		/*
        		 * Scale the image uniformly (maintain the image's aspect ratio)
        		 * so that both dimensions (width and height) of the image will
        		 * be equal to or less than the corresponding dimension of the
        		 * view (minus padding). The image is then centered in the view.
        		 */
                // Scaling only applies if the gif is larger than the container!
                if (gWidth > vWidth || gHeight > vHeight) {
                    float maxDimensionCenterInside = Math.max(gWidth, gHeight);
                    if (maxDimensionCenterInside == gWidth) {
                        s = vWidth / gWidth;
                    }
                    else {
                        s = vHeight / gHeight;
                    }
                }
                x = (vWidth - gWidth * s) / 2 / (s * mScale);
                y = (vHeight - gHeight * s) / 2 / (s * mScale);
                canvas.scale(s, s);
                break;

            case FIT_CENTER:
        		/*
        		 * Compute a scale that will maintain the original src aspect ratio,
        		 * but will also ensure that src fits entirely inside dst.
        		 * At least one axis (X or Y) will fit exactly.
        		 * The result is centered inside dst.
        		 */
                // This scale type always scales the gif to the exact dimension of the View
                float maxDimensionFitCenter = Math.max(gWidth, gHeight);
                if (maxDimensionFitCenter == gWidth) {
                    s = vWidth / gWidth;
                }
                else {
                    s = vHeight / gHeight;
                }
                x = (vWidth - gWidth * s) / 2 / (s * mScale);
                y = (vHeight - gHeight * s) / 2 / (s * mScale);
                canvas.scale(s, s);
                break;

            case FIT_START:
        		/*
        		 * Compute a scale that will maintain the original src aspect ratio,
        		 * but will also ensure that src fits entirely inside dst.
        		 * At least one axis (X or Y) will fit exactly.
        		 * The result is centered inside dst.
        		 */
                // This scale type always scales the gif to the exact dimension of the View
                float maxDimensionFitStart = Math.max(gWidth, gHeight);
                if (maxDimensionFitStart == gWidth) {
                    s = vWidth / gWidth;
                }
                else {
                    s = vHeight / gHeight;
                }
                x = 0;
                y = 0;
                canvas.scale(s, s);
                break;

            case FIT_END:
        		/*
        		 * Compute a scale that will maintain the original src aspect ratio,
        		 * but will also ensure that src fits entirely inside dst.
        		 * At least one axis (X or Y) will fit exactly.
        		 * END aligns the result to the right and bottom edges of dst.
        		 */
                // This scale type always scales the gif to the exact dimension of the View
                float maxDimensionFitEnd = Math.max(gWidth, gHeight);
                if (maxDimensionFitEnd == gWidth) {
                    s = vWidth / gWidth;
                }
                else {
                    s = vHeight / gHeight;
                }
                x = (vWidth - gWidth * s) / mScale / s;
                y = (vHeight - gHeight * s) / mScale / s;
                canvas.scale(s, s);
                break;

            case FIT_XY:
        		/*
        		 * Scale in X and Y independently, so that src matches dst exactly.
        		 * This may change the aspect ratio of the src.
        		 */
                float sFitX = vWidth / gWidth;
                s = vHeight / gHeight;
                x = 0;
                y = 0;
                canvas.scale(sFitX, s);
                break;
            default:
                break;
        }

        return new float[] {x, y, s};
    }

    /** @see android.view.View#measure(int, int) */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mScale = getScale();

        int w;
        int h;

        // Desired aspect ratio of the view's contents (not including padding)
        float desiredAspect = 0.0f;

        // We are allowed to change the view's width
        boolean resizeWidth = false;

        // We are allowed to change the view's height
        boolean resizeHeight = false;

        final int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        final int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);

        final Drawable drawable = getDrawable();

        if (drawable != null) {
            w = drawable.getIntrinsicWidth();
            h = drawable.getIntrinsicHeight();
            if (w <= 0) w = 1;
            if (h <= 0) h = 1;
        }
        else if (mGif != null) {
            w = mGif.width();
            h = mGif.height();
            if (w <= 0) w = 1;
            if (h <= 0) h = 1;
        }
        else {
            // If no drawable, its intrinsic size is 0.
            w = 0;
            h = 0;
        }

        // We are supposed to adjust view bounds to match the aspect
        // ratio of our drawable. See if that is possible.
        if (w > 0 && h > 0) {
            if (mAdjustViewBounds) {
                resizeWidth = widthSpecMode != MeasureSpec.EXACTLY && mFillDirection != FillDirection.HORIZONTAL;
                resizeHeight = heightSpecMode != MeasureSpec.EXACTLY && mFillDirection != FillDirection.VERTICAL;

                desiredAspect = (float) w / (float) h;
            }
        }

        int pleft = getPaddingLeft();
        int pright = getPaddingRight();
        int ptop = getPaddingTop();
        int pbottom = getPaddingBottom();

        int widthSize;
        int heightSize;

        if (resizeWidth || resizeHeight) {
            // If we get here, it means we want to resize to match the
            // drawables aspect ratio, and we have the freedom to change at
            // least one dimension.

            // Get the max possible width given our constraints
            widthSize = resolveAdjustedSize(w + pleft + pright, mMaxWidth, widthMeasureSpec);

            // Get the max possible height given our constraints
            heightSize = resolveAdjustedSize(h + ptop + pbottom, mMaxHeight, heightMeasureSpec);

            if (desiredAspect != 0.0f) {
                // See what our actual aspect ratio is
                float actualAspect = (float) (widthSize - pleft - pright) /
                                     (heightSize - ptop - pbottom);

                if (Math.abs(actualAspect - desiredAspect) > 0.0000001) {

                    boolean done = false;

                    // Try adjusting width to be proportional to height
                    if (resizeWidth) {
                        int newWidth = (int) (desiredAspect * (heightSize - ptop - pbottom)) +
                                       pleft + pright;
                        if (newWidth <= widthSize || mFillDirection == FillDirection.VERTICAL) {
                            widthSize = newWidth;
                            done = true;
                        }
                    }

                    // Try adjusting height to be proportional to width
                    if (!done && resizeHeight) {
                        int newHeight = (int) ((widthSize - pleft - pright) / desiredAspect) +
                                        ptop + pbottom;
                        if (newHeight <= heightSize || mFillDirection == FillDirection.HORIZONTAL) {
                            heightSize = newHeight;
                        }
                    }
                }
            }
        }
        else {
            /* We either don't want to preserve the drawables aspect ratio,
               or we are not allowed to change view dimensions. Just measure in
               the normal way.
            */
            w += pleft + pright;
            h += ptop + pbottom;

            w = Math.max(w, getSuggestedMinimumWidth());
            h = Math.max(h, getSuggestedMinimumHeight());

            widthSize = resolveSize(w, widthMeasureSpec);
            heightSize = resolveSize(h, heightMeasureSpec);
        }

        setMeasuredDimension(widthSize, heightSize);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        return new SavedState(superState);
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
    }

    ///////////////////////////////////////////////////////////
    ///                  PRIVATE HELPERS                    ///
    ///////////////////////////////////////////////////////////

    /** Copied from {@link ImageView}'s implementation. */
    private int resolveAdjustedSize(int desiredSize, int maxSize,
                                    int measureSpec) {
        int result = desiredSize;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        switch (specMode) {
            case MeasureSpec.UNSPECIFIED:
                // Parent says we can be as big as we want. Just don't be larger
                // than max size imposed on ourselves.

                result = Math.min(desiredSize, maxSize);
                break;

            case MeasureSpec.AT_MOST:
                // Parent says we can be as big as we want, up to specSize.
                // Don't be larger than specSize, and don't be larger than
                // the max size imposed on ourselves.
                result = Math.min(Math.min(desiredSize, specSize), maxSize);
                break;

            case MeasureSpec.EXACTLY:
                // No choice. Do what we are told.
                result = specSize;
                break;
        }
        return result;
    }

    /**
     * Calculates the top displacement for the image to make sure it
     * is aligned at the top of the ImageViewEx.
     */
    private float calcTopAlignYDisplacement() {
        int viewHeight = getHeight();
        int imgHeight;
        float displacement = 0f;

        if (viewHeight <= 0) {
            Log.v(TAG, "The ImageViewEx is still initializing...");
            return displacement;
        }

        if (mGif == null) {
            final Drawable tmpDrawable = getDrawable();
            if (!(tmpDrawable instanceof BitmapDrawable) || mGif == null) {
                return 0f;     // Nothing to do here
            }

            // Retrieve the bitmap, its height and the ImageView height
            Bitmap bmp = ((BitmapDrawable) tmpDrawable).getBitmap();
            imgHeight = bmp.getScaledHeight(mDm);
        }
        else {
            // This is a GIF...
            imgHeight = mGif.height();
        }

        //noinspection IfMayBeConditional
        if (viewHeight > imgHeight) {
            displacement = -1 * (viewHeight - imgHeight);   // Just align to top edge
        }
        else {
            // Top displacement [px] = (image height / 2) - (view height / 2)
            displacement = -1 * ((imgHeight - viewHeight) / 2);        // This is in pixels...
        }
        return displacement;
    }

    /**
     * Blocks layout recalculation if the image is set as fixed size
     * to prevent unnecessary calculations and provide butteriness.
     */
    private void blockLayoutIfPossible() {
        if (mIsFixedSize) {
            mBlockLayout = true;
        }
    }

    /**
     * Internal method, deciding whether to trigger the custom decision method {@link #canAnimate()}
     * or to use the static class value of mCanAlwaysAnimate.
     *
     * @return true if the animation can be started, false otherwise.
     */
    private boolean internalCanAnimate() {
        return canAlwaysAnimate() ? canAnimate() : canAlwaysAnimate();
    }

    /**
     * Stops any currently running async loading (deserialization and
     * parsing of the image).
     */
    public void stopLoading() {
        //noinspection ConstantConditions
        if (mHandler != null) {
            mHandler.removeCallbacks(mSetDrawableRunnable);
            mHandler.removeCallbacks(mSetGifRunnable);
        }
    }

    /**
     * Temporarily shows the empty drawable (or empties
     * the view if none is defined). Note that this does not
     * follow all procedures {@link #setImageDrawable(android.graphics.drawable.Drawable)}
     * follows and is only intended for temporary assignments such as in
     * {@link ImageViewNext.ImageLoadCompletionListener#onLoadStarted(ImageViewNext, ImageViewNext.CacheLevel)}.
     */
    public void showEmptyDrawable() {
        setScaleType(ScaleType.CENTER_CROP);
        super.setImageDrawable(mEmptyDrawable);
    }


    ///////////////////////////////////////////////////////////
    ///                  PRIVATE CLASSES                    ///
    ///////////////////////////////////////////////////////////

    /** Class that represents a saved state for the ImageViewEx. */
    private static class SavedState extends BaseSavedState {
        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
        }

        @SuppressWarnings("unused")
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    /** A Runnable that sets a specified Drawable on the ImageView. */
    private class SetDrawableRunnable implements Runnable {

        private Drawable mDrawable;
        private final Object mDrawableLock = new Object();

        private void setDrawable(Drawable drawable) {
            synchronized (mDrawableLock) {
                mDrawable = drawable;
            }
        }

        @Override
        public void run() {
            synchronized (mDrawableLock) {
                if (mDrawable == null) {
                    Log.v(TAG, "Loading the Drawable has been aborted");
                    return;
                }

                setImageDrawable(mDrawable);
                measure(0, 0);
                requestLayout();

                try {
                    AnimationDrawable animationDrawable = (AnimationDrawable) getDrawable();
                    animationDrawable.start();
                }
                catch (Exception ignored) {
                }
            }
        }
    }

    /** A Runnable that sets a specified Movie on the ImageView. */
    private class SetGifRunnable implements Runnable {

        private Movie mGifMovie;
        private final Object mGifMovieLock = new Object();

        private void setGif(Movie drawable) {
            synchronized (mGifMovieLock) {
                mGifMovie = drawable;
            }
        }

        @Override
        public void run() {
            synchronized (mGifMovieLock) {
                if (mGifMovie == null) {
                    Log.v(TAG, "Loading the GIF has been aborted");
                    return;
                }

                initializeDefaultValues();
                mImageSource = IMAGE_SOURCE_GIF;
                setImageDrawable(null);
                mGif = mGifMovie;

                measure(0, 0);
                requestLayout();

                play();
            }
        }
    }

    /**
     * The fill direction for the image. All values other than
     * {@link FillDirection#NONE} imply having the
     * <code>adjustViewBounds</code> function active on the
     * {@link ImageViewEx}.
     */
    public enum FillDirection {
        /**
         * No fill direction. Acts just like a common
         * {@link ImageView} does.
         */
        NONE,

        /**
         * If the width of the {@link ImageViewEx} is longer
         * than the width of the image it contains, the image
         * is scaled to fit the width of the view. The height
         * of the view is then adjusted to fit the height of
         * the scaled image.
         */
        HORIZONTAL,

        /**
         * If the height of the {@link ImageViewEx} is longer
         * than the height of the image it contains, the image
         * is scaled to fit the height of the view. The width
         * of the view is then adjusted to fit the width of
         * the scaled image.
         */
        VERTICAL
    }
}
