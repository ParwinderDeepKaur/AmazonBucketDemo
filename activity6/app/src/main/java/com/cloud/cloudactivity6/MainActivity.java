package com.cloud.cloudactivity6;

import android.os.Bundle;
import android.os.StrictMode;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class MainActivity extends AppCompatActivity {

    TabLayout tabLayout;
    ViewPager2 viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        viewPager = (ViewPager2) findViewById(R.id.viewPager);

        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.upload_image)));
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.upload_text_file)));
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.text_file)));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        final MyAdapter adapter = new MyAdapter(this);
        adapter.addFragment(new UploadImageFragment(), "Upload image");
        adapter.addFragment(new TextFileFragment(), "Upload Text");
        adapter.addFragment(new TextFragment(), "Text File");
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(0);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> tab.setText(adapter.getTabTitle(position))).attach();
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }
}