package com.citrus.suzaku;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.citrus.suzaku.database.MusicDBService;
import com.citrus.suzaku.pref.LibraryFolderDialog;
import com.citrus.suzaku.pref.LibraryFolderPreference;
import com.citrus.suzaku.pref.PreferenceUtils;


public class SettingActivity extends AppCompatActivity
{
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_setting);

		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		getSupportFragmentManager()
				.beginTransaction()
				.replace(R.id.container, new SettingFragment())
				.commit();
	}

	private void replaceFragment(Fragment fragment)
	{
		getSupportFragmentManager()
				.beginTransaction()
				.replace(R.id.container, fragment)
				.addToBackStack(null)
				.commit();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId()){
			case android.R.id.home:				// Up
				FragmentManager fm = getSupportFragmentManager();
				if(fm.getBackStackEntryCount() > 0){
					fm.popBackStack();
				}else{
					finish();
				}
				return true;

			default:
				return super.onOptionsItemSelected(item);
		}
	}

	public static class SettingFragment extends PreferenceFragmentCompat
	{
		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey)
		{
			addPreferencesFromResource(R.xml.preference);

			findPreference("pref_lib").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					SettingActivity activity = (SettingActivity)getActivity();
					activity.replaceFragment(new LibrarySettingFragment());
					activity.getSupportActionBar().setTitle(R.string.library);
					return true;
				}
			});

			findPreference("pref_ui").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					((SettingActivity)getActivity()).replaceFragment(new UiSettingFragment());
					return true;
				}
			});
		}

		@Override
		public void onActivityCreated(Bundle savedInstanceState)
		{
			super.onActivityCreated(savedInstanceState);
			((SettingActivity)getActivity()).getSupportActionBar().setTitle(R.string.settings);
		}
	}

	public static class UiSettingFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener
	{
		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey)
		{
			addPreferencesFromResource(R.xml.preference_ui);
		}

		@Override
		public void onActivityCreated(Bundle savedInstanceState)
		{
			super.onActivityCreated(savedInstanceState);
			((SettingActivity)getActivity()).getSupportActionBar().setTitle(R.string.ui);
		}

		@Override
		public void onResume()
		{
			super.onResume();
			getPreferenceScreen().getSharedPreferences()
			.registerOnSharedPreferenceChangeListener(this);
		}

		@Override
		public void onPause()
		{
			super.onPause();
			getPreferenceScreen().getSharedPreferences()
			.unregisterOnSharedPreferenceChangeListener(this);
		}

		@Override
		public void onSharedPreferenceChanged(SharedPreferences pref, String key)
		{
		/*
			if(key.equals(PreferenceUtils.LANGUAGE)){
				PreferenceUtils.updateLocale();
			}
		*/
		}
	}
	
	public static class LibrarySettingFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener
	{
		private boolean mUpdateFlag = false;

		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey)
		{
			addPreferencesFromResource(R.xml.preference_library);
		}

		@Override
		public void onActivityCreated(Bundle savedInstanceState)
		{
			super.onActivityCreated(savedInstanceState);
			((SettingActivity)getActivity()).getSupportActionBar().setTitle(R.string.library);
		}

		@Override
		public void onResume()
		{
			super.onResume();
			getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
		}

		@Override
		public void onPause()
		{
			super.onPause();
			getPreferenceScreen().getSharedPreferences()
				.unregisterOnSharedPreferenceChangeListener(this);
			
			if(mUpdateFlag){
				Intent intent = new Intent(MusicDBService.ACTION_UPDATE_DATABASE);
				intent.setPackage(App.PACKAGE);
				getActivity().startService(intent);

				mUpdateFlag = false;
			}
		}

		@Override
		public void onSharedPreferenceChanged(SharedPreferences pref, String key)
		{
			if(key.equals(PreferenceUtils.GROUP_COMPILATION) || key.equals(PreferenceUtils.MUSIC_FOLDER)){
				mUpdateFlag = true;
			}
		}

		@Override
		public void onDisplayPreferenceDialog(Preference preference)
		{
			DialogFragment dialogFragment = null;
			if (preference instanceof LibraryFolderPreference){
				dialogFragment = new LibraryFolderDialog();
				Bundle bundle = new Bundle(1);
				bundle.putString("key", preference.getKey());
				dialogFragment.setArguments(bundle);
			}

			if (dialogFragment != null){
				dialogFragment.setTargetFragment(this, 0);
				dialogFragment.show(this.getFragmentManager(), "android.support.v7.preference.PreferenceFragment.DIALOG");
			}else{
				super.onDisplayPreferenceDialog(preference);
			}
		}
	}

}
