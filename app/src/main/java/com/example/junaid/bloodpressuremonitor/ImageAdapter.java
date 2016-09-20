package com.example.junaid.bloodpressuremonitor;


import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class ImageAdapter  extends PagerAdapter{
    Context mContext;

    ImageAdapter(Context context) {
        this.mContext = context;
    }

    @Override
    public int getCount() {
        return sliderImagesId.length;                                                               // get total no. of the images
    }

    private int[] sliderImagesId = new int[]{                                                       // images to show in the view pager
            R.drawable.bpm_1, R.drawable.bpm_2, R.drawable.bpm_3,
            R.drawable.bpm_4, R.drawable.bpm_5, R.drawable.bpm_6,
            R.drawable.bpm_7, R.drawable.bpm_8, R.drawable.bpm_9,
            R.drawable.bpm_10

    };

    @Override
    public boolean isViewFromObject(View v, Object obj) {
        return v == ((ImageView) obj);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int i) {                                     // set new image to the view pager
        ImageView mImageView = new ImageView(mContext);
        mImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        mImageView.setImageResource(sliderImagesId[i]);
        ((ViewPager) container).addView(mImageView, 0);
        return mImageView;
    }

    @Override
    public void destroyItem(ViewGroup container, int i, Object obj) {                               // destroy the last image from the view pager
        ((ViewPager) container).removeView((ImageView) obj);
    }
}
