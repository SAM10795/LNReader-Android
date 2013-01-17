package com.erakk.lnreader.activity;

import android.app.Activity;
import android.app.TabActivity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.R;
import com.erakk.lnreader.UIHelper;


@SuppressWarnings("deprecation")
public class DisplayNovelPagerActivity extends TabActivity {
    // TabSpec Names
    private static final String MAIN_SPEC = "Main";
    private static final String TEASER_SPEC = "Teaser";
    private static final String ORIGINAL_SPEC = "Original";
    static TabHost tabHost;
	private boolean isInverted;
 
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ) {
            UIHelper.SetTheme(this, R.layout.activity_display_novel_pager);
            UIHelper.SetActionBarDisplayHomeAsUp(this, false);
            setContentView(R.layout.activity_display_novel_pager);
        }
		else{
            UIHelper.SetTheme(this, R.layout.activity_display_novel_pager_fix);
            UIHelper.SetActionBarDisplayHomeAsUp(this, false);
            setContentView(R.layout.activity_display_novel_pager_fix);
		} 
        tabHost = getTabHost();
        isInverted = getColorPreferences();
        
        // First Tab - Normal Novels
        TabSpec firstSpec = tabHost.newTabSpec(MAIN_SPEC);
        firstSpec.setIndicator(MAIN_SPEC);
        Intent firstIntent = new Intent(this, DisplayLightNovelListActivity.class);
    	firstIntent.putExtra(Constants.EXTRA_ONLY_WATCHED, false);
        firstSpec.setContent(firstIntent);
        
        // Second Tab - Teasers
        TabSpec secondSpec = tabHost.newTabSpec(TEASER_SPEC);
        secondSpec.setIndicator(TEASER_SPEC);
        Intent secondIntent = new Intent(this, DisplayTeaserListActivity.class);
        secondSpec.setContent(secondIntent);
        
        // Third Tab - Original
        TabSpec thirdSpec = tabHost.newTabSpec(ORIGINAL_SPEC);
        thirdSpec.setIndicator(ORIGINAL_SPEC);
        Intent thirdIntent = new Intent(this, DisplayOriginalListActivity.class);
        thirdSpec.setContent(thirdIntent);
 
        // Adding all TabSpec to TabHost
        tabHost.addTab(firstSpec); // Adding First tab
        tabHost.addTab(secondSpec); // Adding Second tab
        tabHost.addTab(thirdSpec);
        setTabColor();
        
        tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            public void onTabChanged(String tabId) {
            	setTabColor();
            }
        });
        
        //Cheap preload list hack.
        tabHost.setCurrentTabByTag(TEASER_SPEC);
        tabHost.setCurrentTabByTag(ORIGINAL_SPEC);
        tabHost.setCurrentTabByTag(MAIN_SPEC);
        
    }
    
    @Override
    protected void onRestart() {
        super.onRestart();
        if(isInverted != getColorPreferences()) {
        	UIHelper.Recreate(this);
        }
    }
    
    public static void setTabColor() {
        for(int i=0;i<tabHost.getTabWidget().getChildCount();i++)
        {
//            tabHost.getTabWidget().getChildAt(i).setBackgroundColor(Color.parseColor("#2D5A9C")); //unselected
            tabHost.getTabWidget().getChildAt(i).setBackgroundColor(Color.parseColor("#000000")); //unselected
        }
//        tabHost.getTabWidget().getChildAt(tabHost.getCurrentTab()).setBackgroundColor(Color.parseColor("#234B7E")); // selected
        tabHost.getTabWidget().getChildAt(tabHost.getCurrentTab()).setBackgroundColor(Color.parseColor("#708090")); // selected
    }
    
    public static TabHost getMainTabHost() {
    	return tabHost;
    }
    
    private boolean getColorPreferences(){
    	return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_INVERT_COLOR, true);
	}
    
    
    // Option Menu related
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_display_light_novel_list, menu);
		return true;
	}
    
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
    	Activity activity = getCurrentActivity();
		switch (item.getItemId()) {
		case R.id.menu_settings:
			Intent launchNewIntent = new Intent(this, DisplaySettingsActivity.class);
			startActivity(launchNewIntent);
			return true;
		case R.id.menu_refresh_novel_list:			
			if(activity instanceof DisplayLightNovelListActivity) {
				((DisplayLightNovelListActivity)activity).refreshList();								
			}
			else if(activity instanceof DisplayTeaserListActivity) {
				((DisplayTeaserListActivity)activity).refreshList();
			}
			else if(activity instanceof DisplayOriginalListActivity) {
				((DisplayOriginalListActivity)activity).refreshList();
			}
			return true;
		case R.id.invert_colors:			
			UIHelper.ToggleColorPref(this);
			UIHelper.Recreate(this);
			return true;
		case R.id.menu_manual_add:			
			if(activity instanceof DisplayLightNovelListActivity) {
				((DisplayLightNovelListActivity)activity).manualAdd();								
			}
			else if(activity instanceof DisplayTeaserListActivity) {
				((DisplayTeaserListActivity)activity).refreshList();
			}
			else if(activity instanceof DisplayOriginalListActivity) {
				((DisplayOriginalListActivity)activity).refreshList();
			}
			return true;
		case R.id.menu_download_all:
			if(activity instanceof DisplayLightNovelListActivity) {
				((DisplayLightNovelListActivity)activity).downloadAllNovelInfo();								
			}
			else if(activity instanceof DisplayTeaserListActivity) {
				((DisplayTeaserListActivity)activity).downloadAllNovelInfo();
			}
			else if(activity instanceof DisplayOriginalListActivity) {
				((DisplayOriginalListActivity)activity).downloadAllNovelInfo();
			}
			return true;
		case R.id.menu_bookmarks:
    		Intent bookmarkIntent = new Intent(this, DisplayBookmarkActivity.class);
        	startActivity(bookmarkIntent);
			return true;    
		case R.id.menu_downloads:
    		Intent downloadsItent = new Intent(this, DownloadListActivity.class);
        	startActivity(downloadsItent);
			return true; 
		case android.R.id.home:
			super.onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}