package com.example.junaid.bloodpressuremonitor;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

public class HowToMeasureBP extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_how_to_measure_bp);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ViewPager mViewPager = (ViewPager) findViewById(R.id.viewpage);                             // view pager to flip left and right through pages of data
        ImageAdapter adapterView = new ImageAdapter(this);                                          // image adapter to show images in the view pager
        mViewPager.setAdapter(adapterView);
    }

}
