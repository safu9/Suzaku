package com.citrus.suzaku;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;


public class PreferenceUtils
{
	// Used in Xml
	public static final String PLAYER_SCREEN = "playerScreen";
	public static final String LANGUAGE = "language";
	public static final String GROUP_COMPILATION = "groupCompilations";
	public static final String MUSIC_FOLDER = "musicFolder";
	
	// Used in Code only
	public static final String TAB_POSITION = "tabPosition";
	public static final String DB_LAST_UPDATED = "dbLastUpdated";
	public static final String SD_TREE_URI = "sdTreeUri";
	
	
	public static void initPreference()
	{
		Context context = App.getContext();
		
		PreferenceManager.setDefaultValues(context, R.xml.preference_ui, false);
		PreferenceManager.setDefaultValues(context, R.xml.preference_library, false);
		
		String language = PreferenceManager.getDefaultSharedPreferences(context).getString(LANGUAGE, "");
		if(language.isEmpty()){
			language = Locale.getDefault().getLanguage();
			String[] languages = context.getResources().getStringArray(R.array.languageValues);
			if(!Arrays.asList(languages).contains(language)){
				language = "en";
			}
			
			PreferenceManager.getDefaultSharedPreferences(context).edit().putString(LANGUAGE, language).apply();
		}
		updateLocale();
	}
	
	public static void updateLocale()
	{
		Context context = App.getContext();
		String language = PreferenceManager.getDefaultSharedPreferences(context).getString(LANGUAGE, "en");
		
	//	LocaleUtils.setLocale(new Locale(language));
	//	LocaleUtils.updateConfig(context.getResources().getConfiguration());
	}
	
	public static boolean getBoolean(String key)
	{
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(App.getContext());
		return pref.getBoolean(key, false);
	}
	
	public static void putInt(String key, int value)
	{
		PreferenceManager.getDefaultSharedPreferences(App.getContext()).edit().putInt(key, value).apply();
	}
	
	public static int getInt(String key)
	{
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(App.getContext());
		return pref.getInt(key, 0);
	}
	
	public static void putLong(String key, long value)
	{
		PreferenceManager.getDefaultSharedPreferences(App.getContext()).edit().putLong(key, value).apply();
	}
	
	public static long getLong(String key)
	{
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(App.getContext());
		return pref.getLong(key, 0);
	}

	public static void putString(String key, String value)
	{
		PreferenceManager.getDefaultSharedPreferences(App.getContext()).edit().putString(key, value).apply();
	}
	
	public static String getString(String key)
	{
		return getString(key, null);
	}
	
	public static String getString(String key, String defaultVal)
	{
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(App.getContext());
		return pref.getString(key, defaultVal);
	}

	// 設定値 ArrayList<String> を保存
	public static void putStringList(String key, ArrayList<String> list)
	{
		PreferenceManager.getDefaultSharedPreferences(App.getContext()).edit().putString(key, toJsonString(list)).apply();
	}

	public static String toJsonString(List<String> list)
	{
		if(list == null){
			return null;
		}

		JSONArray jsonAry = new JSONArray();
		for(String str : list) {
			jsonAry.put(str);
		}
		return jsonAry.toString();
	}

	// 設定値 ArrayList<String> を取得
	public static ArrayList<String> getStringList(String key)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(App.getContext());
		return toStringList(prefs.getString(key, ""));
	}

	public static ArrayList<String> toStringList(String strJson)
	{
		ArrayList<String> list = new ArrayList<>();
		if(strJson != null && !strJson.equals("")) {
			try {
				JSONArray jsonAry = new JSONArray(strJson);
				for(int i=0; i<jsonAry.length(); i++) {
					list.add(jsonAry.getString(i));
				}
			} catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		return list;
	}
}
