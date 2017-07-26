package com.citrus.suzaku;

import android.annotation.TargetApi;
import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.util.DisplayMetrics;
import android.util.Log;

import com.google.firebase.analytics.FirebaseAnalytics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;


public class App extends Application
{
	public static final String PACKAGE = "com.citrus.suzaku";				// for explicit service intent

	private static Context appContext;

	private static FirebaseAnalytics mFirebaseAnalytics;
	
	@Override
	public void onCreate()
	{
		super.onCreate();
		
		appContext = getApplicationContext();

		// Obtain the FirebaseAnalytics instance.
		mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

		// Init
		PreferenceUtils.initPreference();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
	//	LocaleUtils.updateConfig(newConfig);
	}
	
	
	// Statics
	
	public static Context getContext()
	{
		return appContext;
	}
	
	// Utils

	// SDカードのfilesディレクトリパスのリストを取得する
	public static List<String> getSdCardFilesDirPathList()
	{
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
			return getSdCardFilesDirPathListForLollipop();
		}else{
			return getSdCardFilesDirPathListUnderLollippop();
		}
	}

	// SDカードのfilesディレクトリパスのリストを取得する
	// Android5.0 (API 21) 以上対応
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private static List<String> getSdCardFilesDirPathListForLollipop()
	{
		List<String> sdCardFilesDirPathList = new ArrayList<>();

		// getExternalFilesDirsはAndroid4.4 (API 19) から利用できるAPI
		// filesディレクトリのリストを取得できる
		File[] dirArr = appContext.getExternalFilesDirs(null);

		for (File dir : dirArr) {
			if (dir != null) {
				String path = dir.getAbsolutePath();
				path = path.substring(0, path.indexOf("/Android/data/com.citrus.suzaku/files"));

				// isExternalStorageRemovableはAndroid5.0 (API 21) から利用できるAPI
				// 取り外し可能かどうか（SDカードかどうか）を判定している
				if (Environment.isExternalStorageRemovable(dir)) {

					// 取り外し可能であればSDカード
					if (!sdCardFilesDirPathList.contains(path)) {
						sdCardFilesDirPathList.add(path);
					}

				} else {
					// 取り外し不可能であれば内部ストレージ
				}
			}
		}
		return sdCardFilesDirPathList;
	}

	// SDカードのfilesディレクトリパスのリストを取得する
	private static List<String> getSdCardFilesDirPathListUnderLollippop()
	{
		List<String> sdCardFilesDirPathList = new ArrayList<>();
		StorageManager sm = (StorageManager) appContext.getSystemService(Context.STORAGE_SERVICE);
		try {

			// StorageVolumeの一覧を取得する。非公開メソッドなので、リフレクションを使用
			// Environment.getExternalStorageDirectory を追うと StorageManagerを使ってStorageVolumeを取得しているのがわかる
			Method getVolumeListMethod = sm.getClass().getDeclaredMethod("getVolumeList");
			Object[] volumeList = (Object[]) getVolumeListMethod.invoke(sm);

			for (Object volume : volumeList) {

				// getPathFileメソッドは、StorageVolumeのFileオブジェクトを取得するメソッド。
				Method getPathFileMethod = volume.getClass().getDeclaredMethod("getPathFile");
				File file = (File) getPathFileMethod.invoke(volume);
				String storageBasePath = file.getAbsolutePath();

				// isRemovableメソッドは、StorageVolumeが取り外し可能かどうかを判定するメソッド。
				Method isRemovableMethod = volume.getClass().getDeclaredMethod("isRemovable");
				boolean isRemovable = (boolean) isRemovableMethod.invoke(volume);

				// ストレージが取り外し可能かどうか（SDカードかどうか）を判定。
				if (isRemovable) {

					// ベースパスがマウントされているかどうかを判定。
					if (isMountedBasePath(storageBasePath)) {

						// StorageVolumeの中で、取り外し可能でかつマウント済みのパスは、SDカード。
						// マウント済みかどうかを確認しないと、機種によっては /mnt/Private などのパスも含まれてしまうことがある。
						if (!sdCardFilesDirPathList.contains(storageBasePath)) {
							String sdCardFilesDirPath = storageBasePath;// + "/Android/data/com.citrus.suzaku/files";
							sdCardFilesDirPathList.add(sdCardFilesDirPath);
						}

					}
				} else {
					// StorageVolumeの中で、取り外し不可能なパスは、内部ストレージ。
				}
			}

		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}

		// Android4.4系のみ、getExternalFilesDirs で一度filesディレクトリを生成する必要がある
		// Android4.4系は、File.mkdirsなどでfilesディレクトリを生成できないため
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			appContext.getExternalFilesDirs(null);
		}

		return sdCardFilesDirPathList;
	}

	// 指定したベースパスが、マウントされていれば、trueを返す
	private static boolean isMountedBasePath(String basePath)
	{
		boolean isMounted = false;
		BufferedReader br = null;
		File mounts = new File("/proc/mounts");

		// /proc/mountsが存在しなければ処理を終了する
		if (!mounts.exists()) {
			return isMounted;
		}

		try {

			// マウントポイントを取得する
			br = new BufferedReader(new FileReader(mounts));
			String line;

			// マウントポイントに該当するパスがあるかチェックする
			while ((line = br.readLine()) != null) {

				if (line.contains(basePath)) {
					// 該当するパスがあればマウントされているため、処理を終える
					isMounted = true;
					break;
				}

			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return isMounted;
	}


	public static int dp2px(int dp)
	{
		DisplayMetrics metrics = appContext.getResources().getDisplayMetrics();
		return (int)(dp * metrics.density);
	}

	// DEBUG Utils
	
	public static void logd(String msg)
	{
		Log.d("Suzaku", msg);
	}
	
	public static void loge(String msg)
	{
		Log.e("Suzaku", msg);
	}
	
}
