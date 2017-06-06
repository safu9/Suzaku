package com.citrus.suzaku;

import android.content.*;
import android.content.res.*;
import android.util.*;
import android.widget.*;

/*
 View を正方形に調整
 */
public class SquareImageButton extends ImageButton
{
	public static final int ADJUST_WIDTH = 0;
	public static final int ADJUST_HEIGHT = 1;

	private int mAdjust;

	public SquareImageButton(Context context)
	{
		super(context);
	}

	public SquareImageButton(Context context, AttributeSet attrs)
	{
		this(context, attrs, 0);
	}

	public SquareImageButton(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);

		if(attrs == null){
			return;
		}

		TypedArray attrsArray = context.obtainStyledAttributes(attrs, R.styleable.SquareImageView);
		mAdjust = attrsArray.getInt(R.styleable.SquareImageView_adjust, 0);
		attrsArray.recycle();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		int size;
		if(mAdjust == ADJUST_WIDTH){
			size = MeasureSpec.getSize(heightMeasureSpec);
		}else{
			size = MeasureSpec.getSize(widthMeasureSpec);
		}

		setMeasuredDimension(size, size);
	}
}
