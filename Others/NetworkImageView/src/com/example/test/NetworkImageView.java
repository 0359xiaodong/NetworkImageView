package com.example.test;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.widget.ImageView;

public class NetworkImageView extends ImageView {
	private String mUrl = null;
	private Drawable mDefaultImage = null;
	private Drawable mErrorImage = null;
	private Drawable mBorderImage = null;
	private boolean mRound = false;
	private Bitmap mBitmap = null;
	private int BROAD_SLIDER = 0;
	private static final int MSG_SUCCUSS_INVALIDATE = 0x00;
	private static final int MSG_ERROR_INVALIDATE = 0x01;
	private static final int MSG_CANCEL_INVALIDATE = 0x02;
	private static final int ST_NULL = -1;
	private static final int ST_IDLE = 0;
	@SuppressWarnings("unused")
	private static final int ST_PROGRESS = 1;
	@SuppressWarnings("unused")
	private int mStatus = ST_NULL;
	private boolean mCacheable = true;//save image to disk
	private OnImageLoaderListener mListener = null;
	private String mTag = NetworkImageView.class.getSimpleName();
	private String mSingleTag = "";
	private static Object token = new Object();

	private ScaleType SCALE_TYPE = ScaleType.CENTER_CROP;
    private static final Config BITMAP_CONFIG = Config.ARGB_8888;
    private static final int COLORDRAWABLE_DIMENSION = 1;
    private static final int DEFAULT_BORDER_WIDTH = 0;
    private static final int DEFAULT_BORDER_COLOR = Color.TRANSPARENT;
    private RectF mDrawableRect = new RectF();
    private RectF mBorderRect = new RectF();
    private RectF mBorderDrawbleRect = new RectF();
    private Matrix mShaderMatrix = new Matrix();
    private Matrix mBorderShaderMatrix = new Matrix();
    private Paint mBitmapPaint = new Paint();
    private Paint mBorderPaint = new Paint();
    private Paint mBorderDrawblePaint = new Paint();
    private int mBorderColor = DEFAULT_BORDER_COLOR;
    private int mBorderWidth = DEFAULT_BORDER_WIDTH;
    private BitmapShader mBitmapShader;
    private BitmapShader mBorderShader;
    private int mBitmapWidth;
    private int mBitmapHeight;
    private int mBorderBitmapWidth;
    private int mBorderBitmapHeight;
    private float mDrawableRadius;
    private float mBorderRadius;
    private float mBorderDrawableRadius;
    private boolean mReady;
    private boolean mSetupPending;
    private boolean mSHowlastImage = false;
    
	public NetworkImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		TypedArray a = context.obtainStyledAttributes(attrs,R.styleable.NetworkImageView);
		mErrorImage = a.getDrawable(R.styleable.NetworkImageView_image_error);//error image
		mDefaultImage = a.getDrawable(R.styleable.NetworkImageView_image_default);//default image
		mBorderImage = a.getDrawable(R.styleable.NetworkImageView_image_border);//border image
		mRound = a.getBoolean(R.styleable.NetworkImageView_image_round, false);//is round imageview or not
		mBorderWidth = a.getDimensionPixelSize(R.styleable.NetworkImageView_image_border_width, DEFAULT_BORDER_WIDTH);//border line width
        mBorderColor = a.getColor(R.styleable.NetworkImageView_image_border_color, DEFAULT_BORDER_COLOR);//border line color
        mSHowlastImage = a.getBoolean(R.styleable.NetworkImageView_image_show_last, false);//just show last url image
        BROAD_SLIDER = a.getDimensionPixelSize(R.styleable.NetworkImageView_image_border_slider, 0);//gap between border and image
		mSingleTag = MD5.ToMD5(String.valueOf(System.currentTimeMillis()) + String.valueOf(Math.random()));//cancel tag
		a.recycle();
		if(mDefaultImage != null)
			setImageDrawable(mDefaultImage);
		mReady = true;
		if (mSetupPending) {
            setup();
            mSetupPending = false;
        }
	}

	public NetworkImageView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}
	
	public NetworkImageView(Context context) {
		this(context, null);
	}
	
	public void setImageDefault(Drawable d) {
		mDefaultImage = d;
	}
	
	public void setImageError(Drawable d) {
		mErrorImage = d;
	}
	
	public void setImageRound(Drawable d) {
		mBorderImage = d;
	}
	
	public void setIsRound(boolean round) {
		mRound = round;
	}
	
	public void setImageUrl(String url) {
		if(url != null && !url.equals("")) {
			mUrl = url;
			loadImage();
		}
	}

	public void setCacheable(boolean cacheable) {
		mCacheable = cacheable;
	}
	
	public void setLoaderListener(OnImageLoaderListener l) {
		mListener = l;
	}
	
	public void setCancelCategoryTag(String tag) {
		mTag = tag;
	}

	public void setBroadSlider(int broadslider){
		BROAD_SLIDER = broadslider;
		setup();
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		mStatus = ST_IDLE;
		mReady = true;
        Message msg = new Message();
        msg.what = MSG_SUCCUSS_INVALIDATE;
        msg.obj = token;
        mHandler.sendMessage(msg);
	}
	
	
	@Override
	protected void onDetachedFromWindow() {
		mReady = false;
		super.onDetachedFromWindow();
		ImageLoader.getInstance().canncelByTag(mSingleTag);
		if(mHandler != null) {
			mHandler.removeCallbacksAndMessages(null);
		}
	}

	@Override
    public ScaleType getScaleType() {
        return SCALE_TYPE;
    }

    @Override
    public void setScaleType(ScaleType scaleType) {
    	SCALE_TYPE = scaleType;
    	super.setScaleType(scaleType);
    }

    public void setImageAlpha(int alpha) {
    	mBitmapPaint.setAlpha(alpha);
    	setup();
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        if (getDrawable() == null) {
            return;
        }
        if(mRound) {
	        canvas.drawCircle(getWidth() / 2, getHeight() / 2, mDrawableRadius, mBitmapPaint);
	        if (mBorderWidth != 0) {
	            canvas.drawCircle(getWidth() / 2, getHeight() / 2, mBorderRadius, mBorderPaint);
	        }
	        if(mBorderImage != null) {
        		canvas.drawCircle(getWidth() / 2, getHeight() / 2, mBorderDrawableRadius, mBorderDrawblePaint);
        	}
        } else {
        	canvas.drawRect(0, 0, getWidth() , getHeight() , mBitmapPaint);
        	if (mBorderWidth != 0) {
        		canvas.drawRect(mBorderRect, mBorderPaint);
        	}
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        setup();
    }

    public int getBorderColor() {
        return mBorderColor;
    }

    public void setBorderColor(int borderColor) {
        if (borderColor == mBorderColor) {
            return;
        }

        mBorderColor = borderColor;
        mBorderPaint.setColor(mBorderColor);
        invalidate();
    }

    public int getBorderWidth() {
        return mBorderWidth;
    }

    public void setBorderWidth(int borderWidth) {
        if (borderWidth == mBorderWidth) {
            return;
        }
        mBorderWidth = borderWidth;
        setup();
    }
    
    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
        mBitmap = bm;
        setup();
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
        mBitmap = getBitmapFromDrawable(drawable);
        setup();
    }

    @Override
    public void setImageResource(int resId) {
        super.setImageResource(resId);
        mBitmap = getBitmapFromDrawable(getDrawable());
        setup();
    }

    private Bitmap getBitmapFromDrawable(Drawable drawable) {
        if (drawable == null) {
            return null;
        }
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }
        try {
            Bitmap bitmap;
            if (drawable instanceof ColorDrawable) {
                bitmap = Bitmap.createBitmap(COLORDRAWABLE_DIMENSION, COLORDRAWABLE_DIMENSION, BITMAP_CONFIG);
            } else {
                bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), BITMAP_CONFIG);
            }
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            return bitmap;
        } catch (OutOfMemoryError e) {
            return null;
        }
    }

    private void setup() {
        if (!mReady) {
            mSetupPending = true;
            return;
        }
        if (mBitmap == null) {
        	if(mDefaultImage != null)
    			setImageDrawable(mDefaultImage);
            return;
        }
        if(mBorderWidth != 0) {
	        mBorderPaint.setStyle(Paint.Style.STROKE);
	        mBorderPaint.setAntiAlias(true);
	        mBorderPaint.setColor(mBorderColor);
	        mBorderPaint.setStrokeWidth(mBorderWidth);
	        mBorderRect.set(0, 0, getWidth(), getHeight());
	        mBorderRadius = Math.min((mBorderRect.height() - mBorderWidth) / 2, (mBorderRect.width() - mBorderWidth) / 2);
        }
        if(mBorderImage != null) {
        	mBorderShader = new BitmapShader(((BitmapDrawable)mBorderImage).getBitmap(), TileMode.CLAMP, TileMode.CLAMP);
        	mBorderDrawblePaint.setAntiAlias(true);
            mBorderDrawblePaint.setShader(mBorderShader);
            mBorderBitmapWidth = ((BitmapDrawable)mBorderImage).getBitmap().getWidth();
        	mBorderBitmapHeight = ((BitmapDrawable)mBorderImage).getBitmap().getHeight();
        	mBorderDrawbleRect.set(0, 0, getWidth(), getHeight());
        	mBorderDrawableRadius = Math.min((mBorderDrawbleRect.height()) / 2, (mBorderDrawbleRect.width()) / 2);
        	updateBorderShaderMatrix();
        }
        mBitmapShader = new BitmapShader(mBitmap, TileMode.CLAMP, TileMode.CLAMP);
        mBitmapPaint.setAntiAlias(true);
        mBitmapPaint.setShader(mBitmapShader);
        mBitmapHeight = mBitmap.getHeight();
        mBitmapWidth = mBitmap.getWidth();
        mDrawableRect.set((mBorderWidth + BROAD_SLIDER), (mBorderWidth + BROAD_SLIDER), getWidth() - (mBorderWidth + BROAD_SLIDER), getHeight() - (mBorderWidth + BROAD_SLIDER));
        mDrawableRadius = Math.min((mDrawableRect.height()) / 2, (mDrawableRect.width()) / 2);
        updateShaderMatrix();
    }
    
    private void updateBorderShaderMatrix() {
    	float scale;
        float dx = 0;
        float dy = 0;
        mBorderShaderMatrix.set(null);
        if (mBorderBitmapWidth * mBorderDrawbleRect.height() > mBorderDrawbleRect.width() * mBorderBitmapHeight) {
            scale = mBorderDrawbleRect.height() / (float) mBorderBitmapHeight;
            dx = (mBorderDrawbleRect.width() - mBorderBitmapWidth * scale) * 0.5f;
        } else {
            scale = mBorderDrawbleRect.width() / (float) mBorderBitmapWidth;
            dy = (mBorderDrawbleRect.height() - mBorderBitmapHeight * scale) * 0.5f;
        }
        mBorderShaderMatrix.setScale(scale, scale);
        mBorderShaderMatrix.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));
        mBorderShader.setLocalMatrix(mBorderShaderMatrix);
    }

    private void updateShaderMatrix() {
        float scale;
        float dx = BROAD_SLIDER;
        float dy = BROAD_SLIDER;
        mShaderMatrix.set(null);
        if (mBitmapWidth * mDrawableRect.height() > mDrawableRect.width() * mBitmapHeight) {
            scale = (mDrawableRect.height()) / (float) mBitmapHeight;
            dx += (mDrawableRect.width() - mBitmapWidth * scale) * 0.5f;
        } else {
            scale = (mDrawableRect.width()) / (float) mBitmapWidth;
            dy += (mDrawableRect.height() - mBitmapHeight * scale) * 0.5f;
        }
        mShaderMatrix.setScale(scale, scale);
        mShaderMatrix.postTranslate((int) (dx + 0.5f) + mBorderWidth, (int) (dy + 0.5f) + mBorderWidth);
        mBitmapShader.setLocalMatrix(mShaderMatrix);
    }
    
	private synchronized void loadImage() {
		ImageLoader.getInstance().excute(mUrl, l, mSingleTag, mTag, mCacheable);
	}
	
	private OnImageLoaderListener l = new OnImageLoaderListener() {
		
		@Override
		public void onLoaderError(String mseeage) {
			if(mReady) {
				Message msg = new Message();
				msg.what = MSG_ERROR_INVALIDATE;
				msg.obj = token;
				mHandler.sendMessage(msg);
			}
		}
		
		@Override
		public void onLoaderComplition(Bitmap bitmap, String tag) {
			if(!mUrl.equals(tag) & mSHowlastImage) {
				return;
			}
			mBitmap = bitmap;
			if(mReady) {
				Message msg = new Message();
				msg.what = MSG_SUCCUSS_INVALIDATE;
				msg.obj = token;
				mHandler.sendMessage(msg);
			}
		}

		@Override
		public void onLoaderCanncel() {
			if(mReady) {
				Message msg = new Message();
				msg.what = MSG_CANCEL_INVALIDATE;
				msg.obj = token;
				mHandler.sendMessage(msg);
			}
		}
	};
	
	public void canncelRequestByCategoryTag(String tag) {
		ImageLoader.getInstance().canncelByCategoryTag(tag);
	}
	
	private Handler mHandler = new Handler(Looper.getMainLooper()) {
		public void handleMessage(Message msg) {
			switch(msg.what){
			case MSG_SUCCUSS_INVALIDATE:
				if(mBitmap != null) {
					NetworkImageView.this.setImageBitmap(mBitmap);
					NetworkImageView.this.invalidate();
				}
				if(mListener != null) {
					mListener.onLoaderComplition(null, mUrl);
				}
				break;
			case MSG_ERROR_INVALIDATE:
				if(mErrorImage != null) {
					setImageDrawable(mErrorImage);
				} else {
					if(mDefaultImage != null) {
						setImageDrawable(mDefaultImage);
					}
				}
				if(mListener != null) {
					mListener.onLoaderError(null);
				}
				break;
			case MSG_CANCEL_INVALIDATE:
				if(mDefaultImage != null) {
					setImageDrawable(mDefaultImage);
				}
				if(mListener != null) {
					mListener.onLoaderCanncel();
				}
				break;
			}
			mStatus = ST_IDLE;
		}
	};
}
