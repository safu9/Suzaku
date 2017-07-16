package com.citrus.suzaku;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.PermissionChecker;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.List;


public class MainActivity extends AppCompatActivity implements ListView.OnItemClickListener
{
	private static final String FRAGMENT_TAG = "MainFragment";

	private static final int REQUEST_CODE_EXTERNAL_STORAGE = 1;

	private Toolbar toolbar;
	private ActionBarDrawerToggle mDrawerToggle;
	private DrawerLayout mDrawer;
	private ListView mDrawerList;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
	//	LocaleUtils.updateConfig(this);
		ArtworkCache.initialize();

        getPermission();

		// UI

/*		// 透過ステータスバー関連
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

		View content = findViewById(android.R.id.content);
		content.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
*/
		toolbar = (Toolbar) findViewById(R.id.toolbar);
		toolbar.setTitle(R.string.app_name);
		setSupportActionBar(toolbar);

		
		mDrawer = (DrawerLayout)findViewById(R.id.drawerLayout);
		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawer, R.string.drawer_open, R.string.drawer_close){
			@Override
			public void onDrawerOpened(View drawer){
			}
			
			@Override
			public void onDrawerClosed(View drawer){
			}
			
			@Override
			public void onDrawerSlide(View drawer, float slideOffset){
				super.onDrawerSlide(drawer, slideOffset);
			}
			
			@Override
			public void onDrawerStateChanged(int newState){
			}
		};
		
		mDrawer.addDrawerListener(mDrawerToggle);
		
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);
		
		mDrawerList = (ListView)findViewById(R.id.drawerList);
		String[] tabTitles = getResources().getStringArray(R.array.tabs);
		mDrawerList.setAdapter(new ArrayAdapter<>(this, R.layout.listitem_text, R.id.title, tabTitles));
		mDrawerList.setOnItemClickListener(this);
		
		// Fragment

		if(savedInstanceState != null){			// 再生成
			return;
		}
		
		getSupportFragmentManager()
		.beginTransaction()
		.replace(R.id.container, new MainFragment(), FRAGMENT_TAG)
		.commit();
			
		getSupportFragmentManager()
		.beginTransaction()
		.replace(R.id.dock, new DockFragment())
		.commit();
		
		// Intent
		processIntent(getIntent());
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState)
	{
		super.onPostCreate(savedInstanceState);
		mDrawerToggle.syncState();
	}
	
	@Override
	protected void onNewIntent(Intent intent)
	{
		super.onNewIntent(intent);
		processIntent(intent);
	}

	private void processIntent(Intent intent)
	{
		if(intent == null){
			return;
		}
		
		Uri uri = intent.getData();
		if(uri == null){
			return;
		}
		
		String path = uri.getPath();
		App.logd("MA Received Uri : " + path);

		List<Track> tracks = (new MusicDB()).getTracks(MusicDB.Tracks.PATH + " = ?", new String[]{path}, null);

		if(tracks.size() != 0){
			
			//! TODO : Add Tracks to Queue
			
			Intent serviceIntent = PlayerService.newPlayIntent(PlaylistManager.PLAY_RANGE_QUEUE, null, 0, false);
			startService(serviceIntent);

			boolean ps = PreferenceUtils.getBoolean(PreferenceUtils.PLAYER_SCREEN);
			if(ps){
				startActivity(new Intent(this, TrackActivity.class));
			}
		}
	}
	
	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		ArtworkCache.release();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.menu_activity_main, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		super.onPrepareOptionsMenu(menu);
		
		String status = Environment.getExternalStorageState();
		if(!status.equals(Environment.MEDIA_MOUNTED)){
			menu.findItem(R.id.menu_scan).setEnabled(false);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		Intent intent;
		
		if(mDrawerToggle.onOptionsItemSelected(item)){
			return true;
		}

		switch(item.getItemId()){
			case android.R.id.home:
				getSupportFragmentManager().popBackStack();
				return true;
				
			case R.id.menu_settings:
				intent = new Intent(this, SettingActivity.class);
				startActivity(intent);
				return true;

			case R.id.menu_scan:
				intent = new Intent(MusicDBService.ACTION_UPDATE_DATABASE);
				intent.setPackage(App.PACKAGE);
				startService(intent);
				return true;
				
			default:
				return super.onOptionsItemSelected(item);
		}
	}

/*	@Override
	public void onBackPressed()
	{
		FragmentManager fm = getSupportFragmentManager();
		
		if(fm.getBackStackEntryCount() > 0){
			fm.popBackStack();
		}else{
			super.onBackPressed();
		}
	}
*/

	// Drawer List Listener
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id)
	{
		FragmentManager fm = getSupportFragmentManager();
		if(fm.getBackStackEntryCount() >= 1){
			FragmentManager.BackStackEntry entry = fm.getBackStackEntryAt(0);
			fm.popBackStackImmediate(entry.getId(), FragmentManager.POP_BACK_STACK_INCLUSIVE);
		}
		
		MainFragment fragment = (MainFragment)fm.findFragmentByTag(FRAGMENT_TAG);
		fragment.setCurrentPage(position);
		
		mDrawer.closeDrawers();
	}
	
	public void replaceFragment(Fragment fragment)
	{
		getSupportFragmentManager()
		.beginTransaction()
		.replace(R.id.container, fragment)
		.addToBackStack(null)
		.commit();
	}
	
	public void showDrawerIndicator(boolean show)
	{
		mDrawerToggle.setDrawerIndicatorEnabled(show);
	}

	public Toolbar getToolbar(){
		return toolbar;
	}

	// パーミッションをリクエスト
	private void getPermission()
    {
        if(PermissionChecker.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
			return;
        }

		// 権限チェックした結果、持っていない場合はダイアログを出す
		if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)){

			new AlertDialog.Builder(this)
					.setTitle("パーミッションの追加説明")
					.setMessage("このアプリは外部ストレージへアクセスします")
					.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							ActivityCompat.requestPermissions(MainActivity.this, new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE }, REQUEST_CODE_EXTERNAL_STORAGE);
						}
					})
					.create()
					.show();
			return;
		}

		// 権限を取得する
		ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE }, REQUEST_CODE_EXTERNAL_STORAGE);
    }

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){

		if (requestCode == REQUEST_CODE_EXTERNAL_STORAGE){
			if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED){

				new AlertDialog.Builder(this)
						.setTitle("パーミッション取得エラー")
						.setMessage("今後は許可しないが選択されました。アプリ設定＞権限をチェックしてください")
						.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){
							@Override
							public void onClick(DialogInterface dialogInterface, int i){
								// 設定を開く
								Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
								// Fragmentの場合はgetContext().getPackageName()
								Uri uri = Uri.fromParts("package", getPackageName(), null);
								intent.setData(uri);
								startActivity(intent);
							}
						})
						.create()
						.show();
			}
		} else {
			super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		}
	}
	
}

