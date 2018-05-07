package com.citrus.suzaku.track;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.provider.DocumentFile;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.citrus.suzaku.App;
import com.citrus.suzaku.ArtworkCache;
import com.citrus.suzaku.pref.PreferenceUtils;
import com.citrus.suzaku.view.ProgressDialog;
import com.citrus.suzaku.R;
import com.citrus.suzaku.TagLibHelper;
import com.citrus.suzaku.base.BaseAsyncTaskLoader;
import com.citrus.suzaku.database.MusicDB;
import com.citrus.suzaku.database.MusicDBService;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class TrackDetailActivity extends AppCompatActivity
{
	private static final int READ_TAG_LOADER_ID = 1;
	private static final int SAVE_TAG_LOADER_ID = 2;

	private static final String READ_DIALOG_TAG = "ReadProgressDialog";
	private static final String SAVE_DIALOG_TAG = "SaveProgressDialog";

	private static final int MSG_UPDATE_DIALOG = 1;
	private static final int MSG_DISMISS_DIALOG = 2;
	

	private List<Track> mTracks;
	private boolean isMultiple = false;
	private Track mTrack;

	private String[] mTag = new String[TagLibHelper.NUMBER_OF_KEY];
	private boolean[] mMultipleValue = new boolean[TagLibHelper.NUMBER_OF_KEY];
	private SparseArray<String> mChangedTag = new SparseArray<>();

	private Uri mArtworkUri;
	private boolean isDeletingArtwork = false;			// アートワークを消去

	private File mRoot;									// Storage Access Framework のURIを取得中のルートディレクトリ
	private boolean isWaitingFinish = false;			// SAF の許可待ち

	private Toolbar mToolbar;
	private TabLayout mTabs;
	private ViewPager mViewPager;

	private MyPagerAdapter mPagerAdapter;

	private static DialogHandler handler;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);    // キーボードを隠す
		setContentView(R.layout.activity_track_detail);

		handler = new DialogHandler(this);

		// UI

		mToolbar = findViewById(R.id.toolbar);
		setSupportActionBar(mToolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		// Tab

		mTabs = findViewById(R.id.tabs);
		mViewPager = findViewById(R.id.pager);
		
		// Data

		if(savedInstanceState != null){										// 再生成時
			for(int i = 0; i < TagLibHelper.NUMBER_OF_KEY; i++){
				String tag = savedInstanceState.getString("CHANGED_TAG_" + i);
				if(tag != null){
					saveTagChanges(i, tag);
				}
			}

			saveArtworkUri((Uri)savedInstanceState.getParcelable("ARTWORK_URI"));
			setDeletingArtwork(savedInstanceState.getBoolean("DELETING_ARTWORK", false));

			getSupportLoaderManager().initLoader(SAVE_TAG_LOADER_ID, null, new SaveTagLoaderCallbacks());	// 再度 Callbacks への紐づけのみを行う
		}

		getSupportLoaderManager().initLoader(READ_TAG_LOADER_ID, null, new ReadTagLoaderCallbacks());
	}

	private void setupView()
	{
		if(!isMultiple){
			mToolbar.setTitle(mTrack.title);
		}else{
			int size = mTracks.size();
			mToolbar.setTitle(getResources().getQuantityString(R.plurals.num_songs, size, size));
		}

		// Tabs

		mPagerAdapter = new MyPagerAdapter(getSupportFragmentManager());
		mViewPager.setAdapter(mPagerAdapter);

		mTabs.setupWithViewPager(mViewPager);

		LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		for(int i = 0; i < mPagerAdapter.getCount(); i++){
			View tab = inflater.inflate(R.layout.tab_item_main, null, false);
			TextView titleView = tab.findViewById(R.id.title_view);

			titleView.setText(mPagerAdapter.getPageTitle(i));

			mTabs.getTabAt(i).setCustomView(tab);
		}
	}

	@Override
	protected void onStart()
	{
		super.onStart();
		EventBus.getDefault().register(this);
	}

	@Override
	protected void onStop()
	{
		super.onStop();
		EventBus.getDefault().unregister(this);
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();

		mViewPager.clearOnPageChangeListeners();
		mTabs.removeAllTabs();

		mViewPager = null;
		mPagerAdapter = null;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);

		for(int i = 0; i < TagLibHelper.NUMBER_OF_KEY; i++){
			outState.putString("CHANGED_TAG_" + i, mChangedTag.get(i));
		}

		outState.putParcelable("ARTWORK_URI", mArtworkUri);
		outState.putBoolean("DELETING_ARTWORK", isDeletingArtwork);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.menu_activity_track_detail, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId()){
			case android.R.id.home:				// up button
				confirmAndFinish();
				return true;

			case R.id.menu_save:
				isWaitingFinish = true;
				saveTrackInfo();
				return true;

			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onBackPressed()
	{
		confirmAndFinish();
	}

	private void confirmAndFinish()
	{
		if(mChangedTag.size() > 0 || mArtworkUri != null || isDeletingArtwork){
			new AlertDialog.Builder(this)
					.setTitle(R.string.save)
					.setMessage(R.string.msg_confirm_saving)
					.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							isWaitingFinish = true;
							saveTrackInfo();
						}
					})
					.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							finish();
						}
					})
					.setNeutralButton(R.string.cancel, null)
					.show();
		}else{
			finish();
		}
	}

	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(MusicDBService.DatabaseChangedEvent event)
	{
		getSupportLoaderManager().restartLoader(READ_TAG_LOADER_ID, null, new ReadTagLoaderCallbacks());
	}

	private String[] getTag()
	{
		return mTag;
	}

	private boolean[] getMultipleValue()
	{
		return mMultipleValue;
	}
	
	private void saveTagChanges(int key, String value)
	{
		mChangedTag.put(key, value);
	}

	private void saveArtworkUri(Uri artwork)
	{
		mArtworkUri = artwork;
	}

	private Uri getArtworkUri()
	{
		return mArtworkUri;
	}

	private void setDeletingArtwork(boolean deletingArtwork)
	{
		isDeletingArtwork = deletingArtwork;
	}

	private boolean getDeletingArtwork()
	{
		return isDeletingArtwork;
	}

	private void saveTrackInfo()
	{
		List<Bundle> pathInfoList = new ArrayList<>();

		for(int i = 0; i < mTracks.size(); i++){
			String path = mTracks.get(i).path;
			boolean isTmp = false;
			File root = null;

			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
				// どのSDカードにあるのか決定
				List<String> rootPaths = App.getSdCardFilesDirPathList();
				for(String rootPath : rootPaths){
					if(path.indexOf(rootPath) == 0){
						root = new File(rootPath);
					}
				}
				if(root != null){		// SDカードの時
					if(!checkSdcardAccessPermission(root)){
						return;
					}
					isTmp = true;
				}
			}

			Bundle pathInfo = new Bundle();
			pathInfo.putString("PATH", path);
			pathInfo.putBoolean("NEED_TMP", isTmp);
			pathInfo.putSerializable("ROOT", root);
			pathInfoList.add(pathInfo);
		}

		Bundle arg = new Bundle();
		arg.putSerializable("PATH_INFO_LIST", (Serializable)pathInfoList);
		getSupportLoaderManager().initLoader(SAVE_TAG_LOADER_ID, arg, new SaveTagLoaderCallbacks());
	}

	// SD Card へのアクセス許可
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private boolean checkSdcardAccessPermission(File root)
	{
		if(root == null){
			return false;
		}

		// Tree Uri を取得　(Storage Access Framework)
		String uriString = PreferenceUtils.getString(PreferenceUtils.SD_TREE_URI + "_" + root.getName());
		if(uriString == null){
			mRoot = root;

			new AlertDialog.Builder(this)
					.setTitle(R.string.sd_card_access)
					.setMessage(R.string.msg_get_sd_permission)
					.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
							startActivityForResult(intent, 1);
						}
					})
					.setNegativeButton(R.string.later, null)
					.show();
			return false;
		}

		return true;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent resultData)
	{
		super.onActivityResult(requestCode, resultCode, resultData);

		if(requestCode == 1 && resultCode == Activity.RESULT_OK && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
			// IntentからtreeのURIを取得します。
			Uri treeUri = resultData.getData();
			DocumentFile file = DocumentFile.fromTreeUri(this, treeUri);

			if(!file.getName().equals(mRoot.getName())){
				checkSdcardAccessPermission(mRoot);
				return;
			}

			PreferenceUtils.putString(PreferenceUtils.SD_TREE_URI + "_" + mRoot.getName(), treeUri.toString());
			App.logd("URI : " + treeUri.toString());

			getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

			// 保存処理を再開
			saveTrackInfo();
		}
	}

	// AsyncTaskLoader

	private static class ReadTagLoader extends BaseAsyncTaskLoader<Bundle>
	{
		private List<Long> ids;

		public ReadTagLoader(Context context, List<Long> ids)
		{
			super(context);
			this.ids = ids;
		}

		@Override
		public Bundle loadInBackground()
		{
			List<Track> tracks = new ArrayList<>();
			boolean isMultiple = false;
			String[] tags = new String[TagLibHelper.NUMBER_OF_KEY];
			boolean[] multipleValue = new boolean[TagLibHelper.NUMBER_OF_KEY];

			if(ids.size() == 0){
				return null;
			}

			MusicDB mdb = new MusicDB();
			for(long id : ids){
				tracks.add(mdb.getTrack(id));
			}

			int size = tracks.size();
			if(size > 1){
				isMultiple = true;
			}

			TagLibHelper tagHelper = new TagLibHelper();
			for(int i = 0; i < size; i++){
				Track track = tracks.get(i);

				updateDialog(track.path + " (" + (i + 1) + "/" + size + ")");
				App.logd("TDA TrackID : " + track.id);

				tagHelper.setFile(track.path);
				tagHelper.dumpTags();

				if(i == 0){
					for(int j = 0; j < TagLibHelper.NUMBER_OF_KEY; j++){
						tags[j] = tagHelper.getTag(j);
					}
				}else{
					for(int j = 0; j < TagLibHelper.NUMBER_OF_KEY; j++){
						if(multipleValue[j]){
							continue;
						}

						String tag = tagHelper.getTag(j);
						if((tag != null && !tag.equals(tags[j])) || (tag == null && tags[j] != null)){
							multipleValue[j] = true;
						}
					}
				}

				tagHelper.release();
			}

			Bundle result = new Bundle();
			result.putSerializable("TRACKS", (Serializable)tracks);
			result.putBoolean("MULTIPLE", isMultiple);
			result.putStringArray("TAG", tags);
			result.putBooleanArray("MULTIPLE_VALUE", multipleValue);

			setResult(result);
			return result;
		}

		private void updateDialog(String text)
		{
			Message msg = new Message();
			msg.obj = READ_DIALOG_TAG;
			msg.what = MSG_UPDATE_DIALOG;
			Bundle arg = new Bundle();
			arg.putString("TEXT", text);
			msg.setData(arg);
			TrackDetailActivity.handler.sendMessage(msg);
		}
	}

	private class ReadTagLoaderCallbacks implements LoaderManager.LoaderCallbacks<Bundle>
	{
		@NonNull
		@Override
		public Loader<Bundle> onCreateLoader(int id, Bundle args)
		{
			List<Long> ids = (List<Long>)getIntent().getSerializableExtra("IDS");
			if(ids == null || ids.size() == 0){
				finish();
				return null;
			}

			ProgressDialog dialog = ProgressDialog.newInstance(getString(R.string.msg_now_loading));
			dialog.setCancelable(false);
			dialog.show(getSupportFragmentManager(), READ_DIALOG_TAG);

			return new ReadTagLoader(TrackDetailActivity.this, ids);
		}

		@Override
		public void onLoadFinished(@NonNull Loader<Bundle> loader, Bundle result)
		{
			// Using FragmentTransaction
			Message msg = new Message();
			msg.obj = READ_DIALOG_TAG;
			msg.what = MSG_DISMISS_DIALOG;
			TrackDetailActivity.handler.sendMessage(msg);

			if(result != null){
				mTracks = (List<Track>)result.getSerializable("TRACKS");
				isMultiple = result.getBoolean("MULTIPLE");
				mTag = result.getStringArray("TAG");
				mMultipleValue = result.getBooleanArray("MULTIPLE_VALUE");

				//! TENTATIVE
				mTrack = mTracks.get(0);
				setupView();
			}else{
				finish();
			}
		}

		@Override
		public void onLoaderReset(@NonNull Loader<Bundle> loader)
		{
		}
	}
	
	private static class SaveTagLoader extends BaseAsyncTaskLoader<Bundle>
	{
		private Context mContext;
		private List<Bundle> mPathInfoList;
		private SparseArray<String> mChangedTag;
		private Uri mArtworkUri;
		private boolean isDeletingArtwork;

		public SaveTagLoader(Context context, List<Bundle> pathInfo, SparseArray<String> changedTag, Uri artworkUri, boolean deletingArtwork)
		{
			super(context);

			mContext = context;

			mPathInfoList = pathInfo;
			mChangedTag = changedTag;
			mArtworkUri = artworkUri;
			isDeletingArtwork = deletingArtwork;
		}

		@Override
		public Bundle loadInBackground()
		{
			List<String> succeededPaths = new ArrayList<>();
			List<String> failedPaths = new ArrayList<>();

			// artwork
			byte[] artworkData = null;
			String mime = null;
			if(mArtworkUri != null){
				ContentResolver cr = getContext().getContentResolver();
				mime = cr.getType(mArtworkUri);

				InputStream in = null;
				try{
					in = cr.openInputStream(mArtworkUri);
					artworkData = getBytes(in);
				}catch(IOException e){
					e.printStackTrace();
				}finally{
					try{
						if(in != null){
							in.close();
						}
					}catch(IOException e){
						e.printStackTrace();
					}
				}
			}

			for(int i = 0; i < mPathInfoList.size(); i++){
				Bundle pathInfo = mPathInfoList.get(i);
				String orgPath = pathInfo.getString("PATH");
				String path = orgPath;
				boolean isTmp = pathInfo.getBoolean("NEED_TMP");

				updateDialog(path + " (" + (i + 1) + "/" + mPathInfoList.size() + ")");

				try{
					if(isTmp){
						path = makeTempFile(path);            // 内部ストレージに退避
					}

					// ファイルに書き込み
					TagLibHelper tagHelper = new TagLibHelper();
					tagHelper.setFile(path);

					int size = mChangedTag.size();
					for(int j = 0; j < size; j++){
						tagHelper.setTag(mChangedTag.keyAt(j), mChangedTag.valueAt(j));
					}
					if(mArtworkUri != null){
						tagHelper.setArtwork(artworkData, mime);
					}else if(isDeletingArtwork){
						tagHelper.deleteArtwork();
					}

					boolean result = tagHelper.saveTag();
					tagHelper.release();

					if(!result){
						throw new IOException("Tags were Rejected.");
					}

					if(isTmp){
						File root = (File)pathInfo.getSerializable("ROOT");
						applyTempFile(orgPath, root);            // 復帰
					}

					succeededPaths.add(path);
				}catch(IOException e){
					e.printStackTrace();
					failedPaths.add(path);
				}finally{
					if(isTmp){
						deleteTempFile(path);
					}
				}
			}

			Bundle result = new Bundle();
			result.putSerializable("SUCCEEDED_PATHS", (Serializable)succeededPaths);
			result.putSerializable("FAILED_PATHS", (Serializable)failedPaths);

			setResult(result);
			return result;
		}

		private void updateDialog(String text)
		{
			Message msg = new Message();
			msg.obj = SAVE_DIALOG_TAG;
			msg.what = MSG_UPDATE_DIALOG;
			Bundle arg = new Bundle();
			arg.putString("TEXT", text);
			msg.setData(arg);
			TrackDetailActivity.handler.sendMessage(msg);
		}

		// SD Card Access (内部ストレージに一時ファイルを作成)

		private String makeTempFile(String path) throws IOException
		{
			File src = new File(path);
			File tmp = new File(App.getContext().getExternalCacheDir().getAbsolutePath() + "/" + src.getName());
			copy(src, tmp);
			return tmp.getAbsolutePath();
		}

		private void applyTempFile(String path, File root) throws IOException, IllegalArgumentException
		{
			File src = new File(path);
			File tmp = new File(App.getContext().getExternalCacheDir().getAbsolutePath() + "/" + src.getName());

			// Tree Uri を取得　(Storage Access Framework)
			Uri treeUri = Uri.parse(PreferenceUtils.getString(PreferenceUtils.SD_TREE_URI + "_" + root.getName()));

			// Document File を取得
			DocumentFile file = DocumentFile.fromTreeUri(mContext, treeUri);
			String[] names = path.substring(root.getAbsolutePath().length() + 1).split("/");
			for(String name : names){
				file = file.findFile(name);
			}

			// Copy
			InputStream in = new FileInputStream(tmp);
			OutputStream out = mContext.getContentResolver().openOutputStream(file.getUri());
			copy2(in, out);
		}

		private static void deleteTempFile(String path)
		{
			File tmp = new File(path);
			if(tmp.exists()){
				tmp.delete();
			}
		}

		private static void copy(File src, File dst) throws IOException
		{
			FileInputStream inStream = null;
			FileOutputStream outStream = null;
			try {
				inStream = new FileInputStream(src);
				outStream = new FileOutputStream(dst);
				FileChannel inChannel = inStream.getChannel();
				FileChannel outChannel = outStream.getChannel();

				long pos = 0;
				while (pos < inChannel.size()) {
					pos += inChannel.transferTo(pos, inChannel.size(), outChannel);
				}
			} finally {
				try {
					if (inStream != null) inStream.close();
					if (outStream != null) outStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		private static void copy2(InputStream src, OutputStream dst) throws IOException
		{
			InputStream inStream = src;
			OutputStream outStream = null;
			try {
				outStream = new BufferedOutputStream(dst);

				int data;
				byte[] buf = new byte[4096];
				while ((data = inStream.read(buf)) != -1) {
					outStream.write(buf, 0, data);
				}
			} finally {
				try {
					if (inStream != null) inStream.close();
					if (outStream != null) outStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		private static byte[] getBytes(InputStream inputStream) throws IOException
		{
			ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
			int bufferSize = 1024;
			byte[] buffer = new byte[bufferSize];

			int len;
			while ((len = inputStream.read(buffer)) != -1) {
				byteBuffer.write(buffer, 0, len);
			}
			return byteBuffer.toByteArray();
		}
	}

	private class SaveTagLoaderCallbacks implements LoaderManager.LoaderCallbacks<Bundle>
	{
		@NonNull
		@Override
		public Loader<Bundle> onCreateLoader(int id, Bundle args)
		{
			if(args == null){
				return null;
			}

			ProgressDialog dialog = ProgressDialog.newInstance(getString(R.string.msg_now_saving));
			dialog.setCancelable(false);
			dialog.show(getSupportFragmentManager(), SAVE_DIALOG_TAG);

			List<Bundle> pathInfoList = (List<Bundle>)args.getSerializable("PATH_INFO_LIST");

			return new SaveTagLoader(TrackDetailActivity.this, pathInfoList, mChangedTag, mArtworkUri, isDeletingArtwork);
		}

		@Override
		public void onLoadFinished(@NonNull Loader<Bundle> loader, Bundle result)
		{
			getSupportLoaderManager().destroyLoader(SAVE_TAG_LOADER_ID);

			// Using FragmentTransaction
			Message msg = new Message();
			msg.obj = SAVE_DIALOG_TAG;
			msg.what = MSG_DISMISS_DIALOG;
			TrackDetailActivity.handler.sendMessage(msg);

			List<String> failedPaths = (List<String>)result.getSerializable("FAILED_PATHS");
			if(failedPaths != null && failedPaths.size() == 0){
				// データベースを更新
				Intent intent = new Intent(MusicDBService.ACTION_UPDATE_TRACKS);
				intent.putExtra(MusicDBService.INTENT_KEY_PATHS, result.getSerializable("SUCCEEDED_PATHS"));
				intent.setPackage(App.PACKAGE);
				startService(intent);

				Toast.makeText(TrackDetailActivity.this, R.string.msg_saved_changes, Toast.LENGTH_SHORT).show();

				if(isWaitingFinish){
					finish();
				}
			}else{
				Toast.makeText(TrackDetailActivity.this, R.string.cant_save_changes, Toast.LENGTH_SHORT).show();
			}
		}

		@Override
		public void onLoaderReset(@NonNull Loader<Bundle> loader)
		{
		}
	}

	// Handler for ProgressDialog
	private static class DialogHandler extends Handler
	{
		private final WeakReference<FragmentActivity> mActivityRef;

		public DialogHandler(FragmentActivity activity)
		{
			mActivityRef = new WeakReference<>(activity);
		}

		@Override
		public void handleMessage(Message msg)
		{
			String tag = msg.obj.toString();
			FragmentActivity activity = mActivityRef.get();
			if(activity == null){
				return;
			}

			ProgressDialog dialog = (ProgressDialog)activity.getSupportFragmentManager().findFragmentByTag(tag);
			if(dialog != null){
				switch(msg.what){
					case MSG_UPDATE_DIALOG:
						String text = msg.getData().getString("TEXT");
						dialog.updateMessage(text);
						break;
					case MSG_DISMISS_DIALOG:
						dialog.dismiss();
						break;
				}
			}
		}
	}

	// ViewPager Adapter
	private class MyPagerAdapter extends FragmentStatePagerAdapter
	{
		private static final int NUM_PAGES = 5;
		private final int[] FRAGMENT_IDS = {
				0, 1, 2, 3, 4
		};
		private static final int NUM_PAGES_MULTIPLE = 3;		// 複数曲モードのとき
		private final int[] FRAGMENT_IDS_MULTIPLE = {
				0, 1, 3
		};

		public MyPagerAdapter(FragmentManager fm)
		{
			super(fm);
		}

		@Override
		public Fragment getItem(int position)
		{
			Fragment fragment;

			int id = (!isMultiple) ? FRAGMENT_IDS[position] : FRAGMENT_IDS_MULTIPLE[position];
			switch(id){
				case 0:
					fragment = InfoFragment.newInstance(isMultiple);
					break;
				case 1:
					fragment = ArtworkFragment.newInstance(mTrack);
					break;
				case 2:
					fragment = LyricsFragment.newInstance();
					break;
				case 3:
					fragment = SortFragment.newInstance(isMultiple);
					break;
				case 4:
					fragment = FileFragment.newInstance(mTrack);
					break;
				default:
					fragment = null;
			}

			return fragment;
		}

		@Override
		public int getCount()
		{
			return (!isMultiple) ? NUM_PAGES : NUM_PAGES_MULTIPLE;
		}

		@Override
		public String getPageTitle(int position)
		{
			int id = (!isMultiple) ? FRAGMENT_IDS[position] : FRAGMENT_IDS_MULTIPLE[position];
			switch(id){
				case 0:
					return getString(R.string.detail);
				case 1:
					return getString(R.string.artwork);
				case 2:
					return getString(R.string.lyrics);
				case 3:
					return getString(R.string.sort);
				case 4:
					return getString(R.string.file);
			}
			return null;
		}

		public Fragment getFragmentAtPosition(int position)
		{
			return (Fragment)instantiateItem(mViewPager, position);
		}
	}

	// Fragments

	public static class InfoFragment extends Fragment
	{
		private static final int ITEM_NUM = 12;
		
		private static final int[] KEYS = {
			TagLibHelper.KEY_TITLE,
			TagLibHelper.KEY_ARTIST,
			TagLibHelper.KEY_ALBUM,
			TagLibHelper.KEY_ALBUMARTIST,
			TagLibHelper.KEY_COMPOSER,
			TagLibHelper.KEY_GENRE,
			TagLibHelper.KEY_TRACKNUMBER,
			TagLibHelper.KEY_TRACKNUMBER,
			TagLibHelper.KEY_DISCNUMBER,
			TagLibHelper.KEY_DISCNUMBER,
			TagLibHelper.KEY_YEAR,
			TagLibHelper.KEY_COMMENT
		};
		
		private static final int[] IDS = {
			R.id.title_edit,
			R.id.artist_edit,
			R.id.album_edit,
			R.id.album_artist_edit,
			R.id.composer_edit,
			R.id.genre_edit,
			R.id.track_num_edit,
			R.id.track_count_edit,
			R.id.disc_num_edit,
			R.id.disc_count_edit,
			R.id.year_edit,
			R.id.comment_edit
		};
	
		private EditText[] mEditTexts = new EditText[ITEM_NUM];
		private CheckBox compilationCheckBox;

		private boolean isEditing;
		
		private static InfoFragment newInstance(boolean isMultiple)
		{
			InfoFragment fragment = new InfoFragment();
			Bundle args = new Bundle();

			args.putBoolean("MULTIPLE", isMultiple);
			fragment.setArguments(args);
			return fragment;
		}
		
		@Override
		public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
		{
			View view = inflater.inflate(R.layout.fragment_track_detail_info, container, false);
			
			for(int i = 0; i < ITEM_NUM; i++){
				mEditTexts[i] = view.findViewById(IDS[i]);
				mEditTexts[i].addTextChangedListener(new MyTextWatcher(i));
			}

			compilationCheckBox = view.findViewById(R.id.compilation_check);
			compilationCheckBox.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					String values = (compilationCheckBox.isChecked())? "1" : "0";
					((TrackDetailActivity)getActivity()).saveTagChanges(TagLibHelper.KEY_COMPILATION, values);
				}
			});

			boolean isMultiple = getArguments().getBoolean("MULTIPLE");
			if(isMultiple){														// 複数曲モードではタイトル非表示
				TextView titleLabel = view.findViewById(R.id.title_label);
				titleLabel.setVisibility(View.GONE);
				mEditTexts[0].setVisibility(View.GONE);
			}
		
			updateView();
			
			return view;
		}

		private void updateView()
		{
			String[] tag = ((TrackDetailActivity)getActivity()).getTag();
			boolean[] multipleValue = ((TrackDetailActivity)getActivity()).getMultipleValue();

			isEditing = true;
			for(int i = 0; i < ITEM_NUM; i++){
				String value = tag[KEYS[i]];

				// 複数フィールドの値でタグ1つのとき
				if(value != null){
					switch(IDS[i]){
						case R.id.track_num_edit:
						case R.id.disc_num_edit:{
							String[] values = value.split("/", 2);
							value = (values.length >= 1) ? values[0] : "";
							break;
						}
						case R.id.track_count_edit:
						case R.id.disc_count_edit:{
							String[] values = value.split("/", 2);
							value = (values.length >= 2) ? values[1] : "";
							break;
						}
					}
				}

				if(!multipleValue[KEYS[i]]){
					mEditTexts[i].setText(value);
				}else{
					if(IDS[i] != R.id.track_num_edit && IDS[i] != R.id.track_count_edit
							&& IDS[i] != R.id.disc_num_edit && IDS[i] != R.id.disc_count_edit){
						mEditTexts[i].setHint(R.string.mix);
					}else{
						mEditTexts[i].setHint(" - ");
					}
				}
			}
			isEditing = false;

			boolean isCompilation = false;
			try{
				isCompilation = (Integer.parseInt(tag[TagLibHelper.KEY_COMPILATION]) > 0);
			}catch(NumberFormatException e){
				// e.printStackTrace();
			}
			compilationCheckBox.setChecked(isCompilation);
		}
		
		private class MyTextWatcher implements TextWatcher
		{
			private int mNumber;
			private String mText;
			
			private MyTextWatcher(int number)
			{
				mNumber = number;
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after)
			{
				mText = s.toString();
			}
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count)
			{
				if(isEditing || s.toString().equals(mText)){
					return;
				}
				
				String values = mEditTexts[mNumber].getText().toString();

				// 複数フィールドの値でタグ1つのとき
				switch(IDS[mNumber]){
					case R.id.track_num_edit:
					case R.id.disc_num_edit:
						values = values + "/" + mEditTexts[mNumber+1].getText().toString();
						break;
					case R.id.track_count_edit:
					case R.id.disc_count_edit:
						values = mEditTexts[mNumber-1].getText().toString() + "/" + values;
						break;
				}

				((TrackDetailActivity)getActivity()).saveTagChanges(KEYS[mNumber], values);

				mEditTexts[mNumber].setHint(null);
			}

			@Override
			public void afterTextChanged(Editable s)
			{
			}
		}
	}
	
	public static class ArtworkFragment extends Fragment
	{
		private static final int REQUEST_GALLERY = 1000;
		private static final String[] FILE_TYPES = {
				"image/jpeg",
				"image/jpg",
				"image/png",
		};

		private Track mTrack;

		private ImageView artworkImageView;
		private Button deleteButton;
		private Button undoButton;
		
		
		private static ArtworkFragment newInstance(Track track)
		{
			ArtworkFragment fragment = new ArtworkFragment();
			Bundle bundle = new Bundle();
			bundle.putSerializable("TRACK", track);
			fragment.setArguments(bundle);

			return fragment;
		}
		
		@Override
		public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
		{
			mTrack = (Track)getArguments().getSerializable("TRACK");
			
			View view = inflater.inflate(R.layout.fragment_track_detail_artwork, container, false);

			artworkImageView = view.findViewById(R.id.artwork_view);

			Button galleryButton = view.findViewById(R.id.gallery_button);
			galleryButton.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					// ギャラリー呼び出し
					Intent intentGallery;
					if(Build.VERSION.SDK_INT < 19){
						intentGallery = new Intent(Intent.ACTION_GET_CONTENT);
						intentGallery.setType("image/*");
					}else{
						intentGallery = new Intent(Intent.ACTION_GET_CONTENT);
						intentGallery.addCategory(Intent.CATEGORY_OPENABLE);
						intentGallery.setType("image/*");
						intentGallery.putExtra(Intent.EXTRA_MIME_TYPES, FILE_TYPES);
					}

					Intent intent = Intent.createChooser(intentGallery, "画像の選択");
					startActivityForResult(intent, REQUEST_GALLERY);
				}
			});

			deleteButton = view.findViewById(R.id.delete_button);
			deleteButton.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					((TrackDetailActivity)getActivity()).saveArtworkUri(null);
					((TrackDetailActivity)getActivity()).setDeletingArtwork(true);
					updateView();
				}
			});

			undoButton = view.findViewById(R.id.undo_button);
			undoButton.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					((TrackDetailActivity)getActivity()).saveArtworkUri(null);
					((TrackDetailActivity)getActivity()).setDeletingArtwork(false);
					updateView();
				}
			});

			updateView();
			
			return view;
		}
		
		private void updateView()
		{
			Uri uri = ((TrackDetailActivity)getActivity()).getArtworkUri();
			boolean isDeletingArtwork = ((TrackDetailActivity)getActivity()).getDeletingArtwork();

			if(uri != null){
				new UpdateImageTask(uri).execute();
			}else if(isDeletingArtwork){
				artworkImageView.setImageDrawable(null);
			}else if(mTrack != null){
				ArtworkCache.Large.setArtworkView(artworkImageView, mTrack);
			}

			deleteButton.setEnabled(!isDeletingArtwork);
			undoButton.setEnabled(uri != null || isDeletingArtwork);
		}

		@Override
		public void onActivityResult(int requestCode, int resultCode, Intent data)
		{
			if(requestCode == REQUEST_GALLERY && resultCode == RESULT_OK){
				Uri uri = data.getData();
				((TrackDetailActivity)getActivity()).saveArtworkUri(uri);
				((TrackDetailActivity)getActivity()).setDeletingArtwork(false);
				updateView();
			}
		}

		private class UpdateImageTask extends AsyncTask<Void, Void, Bitmap>
		{
			private Uri mUri;

			UpdateImageTask(Uri uri)
			{
				mUri = uri;
			}

			@Override
			protected Bitmap doInBackground(Void... params)
			{
				Bitmap bmp = null;
				InputStream in = null;
				try{
					in = App.getContext().getContentResolver().openInputStream(mUri);
					bmp = BitmapFactory.decodeStream(in);
				}catch(IOException e){
					e.printStackTrace();
				}finally{
					try{
						if(in != null){
							in.close();
						}
					}catch(IOException e){
						e.printStackTrace();
					}
				}
				return bmp;
			}

			@Override
			protected void onPostExecute(Bitmap bmp)
			{
				artworkImageView.setImageBitmap(bmp);
			}
		}
	}
	
	public static class LyricsFragment extends Fragment
	{
		private EditText lyricsEditText;

		private boolean isEditing;
		
		private static LyricsFragment newInstance()
		{
			LyricsFragment fragment = new LyricsFragment();
			Bundle args = new Bundle();
			fragment.setArguments(args);
			return fragment;
		}
		
		@Override
		public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
		{
			View view = inflater.inflate(R.layout.fragment_track_detail_lyrics, container, false);
			lyricsEditText = view.findViewById(R.id.lyrics_edit);
			lyricsEditText.addTextChangedListener(new MyTextWatcher());
			
			updateView();
			
			return view;
		}
		
		private void updateView()
		{
			isEditing = true;
			String[] tag = ((TrackDetailActivity)getActivity()).getTag();
			lyricsEditText.setText(tag[TagLibHelper.KEY_LYRICS]);
			isEditing = false;
		}

		private class MyTextWatcher implements TextWatcher
		{
			private String mText;

			private MyTextWatcher()
			{
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after)
			{
				mText = s.toString();
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count)
			{
				if(isEditing || s.toString().equals(mText)){
					return;
				}

				String values = lyricsEditText.getText().toString();
				((TrackDetailActivity)getActivity()).saveTagChanges(TagLibHelper.KEY_LYRICS, values);
			}

			@Override
			public void afterTextChanged(Editable s)
			{
			}
		}
	}

	public static class SortFragment extends Fragment
	{
		private static final int ITEM_NUM = 8;

		private static final int[] KEYS = {
				TagLibHelper.KEY_TITLE,
				TagLibHelper.KEY_TITLESORT,
				TagLibHelper.KEY_ARTIST,
				TagLibHelper.KEY_ARTISTSORT,
				TagLibHelper.KEY_ALBUM,
				TagLibHelper.KEY_ALBUMSORT,
				TagLibHelper.KEY_ALBUMARTIST,
				TagLibHelper.KEY_ALBUMARTISTSORT
		};

		private static final int[] IDS = {
				R.id.title_edit,
				R.id.title_sort_edit,
				R.id.artist_edit,
				R.id.artist_sort_edit,
				R.id.album_edit,
				R.id.album_sort_edit,
				R.id.album_artist_edit,
				R.id.album_artist_sort_edit
		};

		private EditText[] mEditTexts = new EditText[ITEM_NUM];

		private boolean isEditing;

		private static SortFragment newInstance(boolean isMultiple)
		{
			SortFragment fragment = new SortFragment();
			Bundle args = new Bundle();

			args.putBoolean("MULTIPLE", isMultiple);
			fragment.setArguments(args);
			return fragment;
		}

		@Override
		public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
		{
			View view = inflater.inflate(R.layout.fragment_track_detail_sort, container, false);

			for(int i = 0; i < ITEM_NUM; i++){
				mEditTexts[i] = view.findViewById(IDS[i]);
				mEditTexts[i].addTextChangedListener(new MyTextWatcher(i));
			}

			boolean isMultiple = getArguments().getBoolean("MULTIPLE");
			if(isMultiple){														// 複数曲モードではタイトル非表示
				TextView titleLabel = view.findViewById(R.id.title_label);
				titleLabel.setVisibility(View.GONE);
				mEditTexts[0].setVisibility(View.GONE);

				TextView titleSortLabel = view.findViewById(R.id.title_sort_label);
				titleSortLabel.setVisibility(View.GONE);
				mEditTexts[1].setVisibility(View.GONE);
			}

			updateView();

			return view;
		}

		private void updateView()
		{
			String[] tag = ((TrackDetailActivity)getActivity()).getTag();
			boolean[] multipleValue = ((TrackDetailActivity)getActivity()).getMultipleValue();

			isEditing = true;
			for(int i = 0; i < ITEM_NUM; i++){
				if(!multipleValue[KEYS[i]]){
					mEditTexts[i].setText(tag[KEYS[i]]);
				}else{
					mEditTexts[i].setHint(R.string.mix);
				}
			}
			isEditing = false;
		}

		private class MyTextWatcher implements TextWatcher
		{
			private int mNumber;
			private String mText;

			private MyTextWatcher(int number)
			{
				mNumber = number;
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after)
			{
				mText = s.toString();
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count)
			{
				if(isEditing || s.toString().equals(mText)){
					return;
				}

				String values = mEditTexts[mNumber].getText().toString();
				((TrackDetailActivity)getActivity()).saveTagChanges(KEYS[mNumber], values);

				mEditTexts[mNumber].setHint(null);
			}

			@Override
			public void afterTextChanged(Editable s)
			{
			}
		}
	}

	public static class FileFragment extends Fragment
	{
		private Track mTrack;

		private TextView pathTextView;

		private TextView lengthTextView;
		private TextView bitrateTextView;
		private TextView sampleRateTextView;
		private TextView channelsTextView;


		private static FileFragment newInstance(Track track)
		{
			FileFragment fragment = new FileFragment();
			Bundle bundle = new Bundle();
			bundle.putSerializable("TRACK", track);
			fragment.setArguments(bundle);

			return fragment;
		}

		@Override
		public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
		{
			mTrack = (Track)getArguments().getSerializable("TRACK");

			View view = inflater.inflate(R.layout.fragment_track_detail_file, container, false);

			pathTextView = view.findViewById(R.id.path_view);

			lengthTextView = view.findViewById(R.id.length_view);
			bitrateTextView = view.findViewById(R.id.bitrate_view);
			sampleRateTextView = view.findViewById(R.id.sample_rate_view);
			channelsTextView = view.findViewById(R.id.channels_view);

			updateView();

			return view;
		}

		private void updateView()
		{
			if(mTrack == null){
				return;
			}

			pathTextView.setText(mTrack.path);

			//! EXPERIMENTAL
			TagLibHelper tagHelper = new TagLibHelper();
			tagHelper.setFile(mTrack.path);

			int length = tagHelper.getLength();
			Locale locale = Locale.getDefault();
			lengthTextView.setText(String.format(locale, "%d:%02d", length / 60, length % 60));
			bitrateTextView.setText(String.format(locale, "%d kbps", tagHelper.getBitrate()));
			sampleRateTextView.setText(String.format(locale, "%.1f kHz", tagHelper.getSampleRate() / 1000.0));
			channelsTextView.setText(String.format(locale, "%d", tagHelper.getChannels()));

			tagHelper.release();
		}
	}
	
}
