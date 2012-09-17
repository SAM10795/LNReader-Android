package com.erakk.lnreader.activity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.R;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.service.UpdateService;


public class MainActivity extends Activity {
	  private UpdateService service;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        UIHelper.SetTheme(this, R.layout.activity_main);
        UIHelper.SetActionBarDisplayHomeAsUp(this, false);

        doBindService();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
	@Override
    protected void onRestart() {
        super.onRestart();
        UIHelper.Recreate(this);
        doBindService();
    }
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindService(mConnection);
	}
    
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        	case R.id.last_novel:        		
        		/*
        		 * Implement code to load last chapter read
        		 */
        		String lastReadPage = PreferenceManager.getDefaultSharedPreferences(this).getString(Constants.PREF_LAST_READ, "");
        		if(lastReadPage.length() > 0) {
        			Intent intent = new Intent(getApplicationContext(), DisplayLightNovelContentActivity.class);
			        intent.putExtra(Constants.EXTRA_PAGE, lastReadPage);
			        startActivity(intent);
        			Toast.makeText(this, "Loading: " + lastReadPage, Toast.LENGTH_SHORT).show();
        		}
        		else{
        			Toast.makeText(this, "No last read novel.", Toast.LENGTH_SHORT).show();
        		}
        		return true;
        	case R.id.invert_colors:    			
        		UIHelper.ToggleColorPref(this);
        		UIHelper.Recreate(this);    			
    			return true;
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
	
	private ServiceConnection mConnection = new ServiceConnection() {

	    public void onServiceConnected(ComponentName className, IBinder binder) {
	    	service = ((UpdateService.MyBinder) binder).getService();
			Log.d("DERVICE", "onServiceConnected");
	      	Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_SHORT).show();
	    }

	    public void onServiceDisconnected(ComponentName className) {
	    	service = null;
			Log.d("DERVICE", "onServiceDisconnected");
	   	}
	};
	  

	void doBindService() {
	    bindService(new Intent(this, UpdateService.class), mConnection, Context.BIND_AUTO_CREATE);
		Log.d("DERVICE", "doBindService");
	}

//	  public void showServiceData(View view) {
//	      Toast.makeText(this, "ShowServiceData", Toast.LENGTH_SHORT).show();
//	  }
	
    public void openNovelList(View view) {
    	Intent intent = new Intent(this, DisplayLightNovelListActivity.class);
    	intent.putExtra(Constants.EXTRA_ONLY_WATCHED, false);
    	startActivity(intent);
    }
    
    public void openWatchList(View view) {
    	Intent intent = new Intent(this, DisplayLightNovelListActivity.class);
    	intent.putExtra(Constants.EXTRA_ONLY_WATCHED, true);
    	startActivity(intent);
    }
    
    public void openSettings(View view) {
    	Intent intent = new Intent(this, DisplaySettingsActivity.class);
    	startActivity(intent);
    }    
}
