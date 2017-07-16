package com.citrus.suzaku;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import static com.citrus.suzaku.R.string.file;


public class TrackDetailActivity extends AppCompatActivity
{
	private List<Track> mTracks;
	private Track mTrack;
	private File mRoot;			// For Storage Access Framework

	private SparseArray<String> mTag = new SparseArray<>();
	private SparseArray<String> mChangedTag = new SparseArray<>();

	private boolean isWaitingFinish = false;

	private DatabaseBroadcastReceiver receiver;

	private TabLayout mTabs;
	private ViewPager mViewPager;

	private MyPagerAdapter mPagerAdapter;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);    // キーボードを隠す
		setContentView(R.layout.activity_track_detail);

		receiver = new DatabaseBroadcastReceiver();
		
		// Data

		//! TENTATIVE
		mTracks = (ArrayList<Track>)getIntent().getSerializableExtra("TRACKS");
		if(mTracks.size() == 0){
			finish();
			return;
		}
		mTrack = mTracks.get(0);
		
		App.logd("TDA TID" + mTrack.id);

		//! EXPERIMENTAL
		TagLibHelper tagHelper = new TagLibHelper();
		tagHelper.setFile(mTrack.path);

		tagHelper.dumpTags();
		for(int i = 0; i < TagLibHelper.NUMBER_OF_KEY; i++){
			mTag.put(i, tagHelper.getTag(i));
		}

		tagHelper.release();

		// UI

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(mTrack.title);
        setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		// Tab

		mTabs = (TabLayout)findViewById(R.id.tabs);
		mViewPager = (ViewPager)findViewById(R.id.pager);

		mPagerAdapter = new MyPagerAdapter(getSupportFragmentManager());
		mViewPager.setAdapter(mPagerAdapter);

		mTabs.setupWithViewPager(mViewPager);

		LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		for(int i = 0; i < mPagerAdapter.getCount(); i++){
			View tab = inflater.inflate(R.layout.tab_item_main, null, false);
			TextView titleView = (TextView)tab.findViewById(R.id.title);

			titleView.setText(mPagerAdapter.getPageTitle(i));

			mTabs.getTabAt(i).setCustomView(tab);
		}
	}

	@Override
	protected void onStart()
	{
		super.onStart();
		registerReceiver(receiver, receiver.getIntentFilter());
	}

	@Override
	protected void onStop()
	{
		super.onStop();
		unregisterReceiver(receiver);
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
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.menu_activity_track_detail, menu);
		return true;
	}

	@Override
	public void onBackPressed()
	{
		confirmAndFinish();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId()){
			case android.R.id.home:				// up button
				confirmAndFinish();
				return true;

			case R.id.menu_save:
				saveTrackInfo();
				finish();
				return true;

			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private void confirmAndFinish()
	{
		if(mChangedTag.size() > 0){
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

	private SparseArray<String> getTag()
	{
		return mTag;
	}
	
	private void saveTagChanges(int key, String value)
	{
		mChangedTag.put(key, value);
	}

	private void saveTrackInfo()
	{
		int size = mChangedTag.size();

		String path = mTrack.path;
		boolean isTmp = false;

		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
			// どのSDカードにあるのか決定
			List<String> rootPaths = App.getSdCardFilesDirPathList();
			for(String rootPath : rootPaths){
				if(path.indexOf(rootPath) == 0){
					mRoot = new File(rootPath);
				}
			}
			if(mRoot != null){		// SDカードの時
				if(!checkSdcardAccessPermission()){
					return;
				}
				isTmp = true;
			}
		}

		SaveTagAsyncTask task = new SaveTagAsyncTask(isTmp);
		task.execute(mTrack.path);
	}


	// SD Card Access (内部ストレージに一時ファイルを作成)

	public String makeTempFile(String path)
	{
		File src = new File(path);
		File tmp = new File(App.getContext().getExternalCacheDir().getAbsolutePath() + "/" + src.getName());
		boolean result = copy(src, tmp);
		return (result)? tmp.getAbsolutePath() : null;
	}

	public boolean applyTempFile(String path)
	{
		if(mRoot == null){
			return false;
		}

		File src = new File(path);
		File tmp = new File(App.getContext().getExternalCacheDir().getAbsolutePath() + "/" + src.getName());

		// Tree Uri を取得　(Storage Access Framework)
		Uri treeUri = Uri.parse(PreferenceUtils.getString(PreferenceUtils.SD_TREE_URI + "_" + mRoot.getName()));

		// Document File を取得
		DocumentFile file = DocumentFile.fromTreeUri(this, treeUri);
		String[] names = path.substring(mRoot.getAbsolutePath().length() + 1).split("/");
		for(String name : names){
			file = file.findFile(name);
		}

		// Copy
		boolean result = false;
		try{
			InputStream in = new FileInputStream(tmp);
			OutputStream out = getContentResolver().openOutputStream(file.getUri());
			result = copy2(in, out);
		}catch(FileNotFoundException e){
			e.printStackTrace();
		}
		return result;
	}

	private static boolean copy(File src, File dst)
	{
		boolean result = false;

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
			result = true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (inStream != null) inStream.close();
				if (outStream != null) outStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if ((! result) && dst.exists()){
			dst.delete();
		}

		return result;
	}

	private static boolean copy2(InputStream src, OutputStream dst)
	{
		boolean result = false;

		InputStream inStream = src;
		OutputStream outStream = dst;
		try {
			outStream = new BufferedOutputStream(dst);

			int data = -1;
			byte[] buf = new byte[4096];
			while ((data = inStream.read(buf)) != -1) {
				outStream.write(buf, 0, data);
			}

			result = true;
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (inStream != null) inStream.close();
				if (outStream != null) outStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return result;
	}

	// SD Card へのアクセス許可
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public boolean checkSdcardAccessPermission()
	{
		if(mRoot == null){
			return false;
		}

		// Tree Uri を取得　(Storage Access Framework)
		String uriString = PreferenceUtils.getString(PreferenceUtils.SD_TREE_URI + "_" + mRoot.getName());
		if(uriString == null){
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

	public void onActivityResult(int requestCode, int resultCode, Intent resultData)
	{
		if(requestCode == 1 && resultCode == Activity.RESULT_OK && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
			// IntentからtreeのURIを取得します。
			Uri treeUri = resultData.getData();
			DocumentFile file = DocumentFile.fromTreeUri(this, treeUri);

			if(!file.getName().equals(mRoot.getName())){
				checkSdcardAccessPermission();
				return;
			}

			PreferenceUtils.putString(PreferenceUtils.SD_TREE_URI + "_" + mRoot.getName(), treeUri.toString());
			App.logd("URI : " + treeUri.toString());

			getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

			if(isWaitingFinish){
				saveTrackInfo();
			}
		}
	}

	private class SaveTagAsyncTask extends AsyncTask<String, Void, Boolean>
	{
		boolean isTmp;

		public SaveTagAsyncTask(boolean isTmp)
		{
			this.isTmp = isTmp;
		}

		@Override
		protected Boolean doInBackground(String... params)
		{
			String path = params[0];

			if(isTmp){
				path = makeTempFile(params[0]);			// 内部ストレージに退避
			}

			// ファイルに書き込み
			TagLibHelper tagHelper = new TagLibHelper();
			tagHelper.setFile(path);

			int size = mChangedTag.size();
			for(int i = 0; i < size; i++){
				tagHelper.setTag(mChangedTag.keyAt(i), mChangedTag.valueAt(i));
			}
			boolean ret = tagHelper.saveTag();
			tagHelper.release();

			if(isTmp){
				ret = applyTempFile(params[0]);			// 復帰
			}
			return ret;
		}

		@Override
		protected void onPostExecute(Boolean result)
		{
			super.onPostExecute(result);

			if(result){
				// データベースを更新
				Intent intent = new Intent(MusicDBService.ACTION_UPDATE_TRACK);
				intent.putExtra(MusicDBService.INTENT_KEY_PATH, mTrack.path);
				intent.setPackage(App.PACKAGE);
				startService(intent);

				Toast.makeText(TrackDetailActivity.this, R.string.notify_saved_changes, Toast.LENGTH_SHORT).show();

				if(isWaitingFinish){
					finish();
				}
			}else{
				Toast.makeText(TrackDetailActivity.this, R.string.cant_save_changes, Toast.LENGTH_SHORT).show();
			}
		}
	}

	private class MyPagerAdapter extends FragmentStatePagerAdapter
	{
		private static final int NUM_PAGES = 5;

		public MyPagerAdapter(FragmentManager fm)
		{
			super(fm);
		}

		@Override
		public Fragment getItem(int position)
		{
			Fragment fragment;

			switch(position){
				case 0:
					fragment = InfoFragment.newInstance(mTrack);
					break;
				case 1:
					fragment = ArtworkFragment.newInstance(mTrack);
					break;
				case 2:
					fragment = LyricsFragment.newInstance(mTrack);
					break;
				case 3:
					fragment = SortFragment.newInstance(mTrack);
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
			return NUM_PAGES;
		}

		public String getPageTitle(int position){
			switch(position){
				case 0:
					return getString(R.string.detail);
				case 1:
					return getString(R.string.artwork);
				case 2:
					return getString(R.string.lyrics);
				case 3:
					return getString(R.string.sort);
				case 4:
					return getString(file);
			}
			return null;
		}

		public Fragment getFragmentAtPosition(int position)
		{
			return (Fragment)instantiateItem(mViewPager, position);
		}
	}
	
	// BroadcastReceiver
	private class DatabaseBroadcastReceiver extends BroadcastReceiver
	{
		private IntentFilter filter;
		
		public DatabaseBroadcastReceiver()
		{
			filter = new IntentFilter();
			filter.addAction(MusicDBService.ACTION_DATABASE_CHANGED);
		}
		
		@Override
		public void onReceive(Context context, Intent intent)
		{
			MusicDB mdb = new MusicDB();
			mTrack = mdb.getTrack(mTrack.id);
			
			mPagerAdapter.notifyDataSetChanged();
		}
		
		public IntentFilter getIntentFilter()
		{
			return filter;
		}
	}
	
	
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
			R.id.title,
			R.id.artist,
			R.id.album,
			R.id.album_artist,
			R.id.composer,
			R.id.genre,
			R.id.track_no,
			R.id.track_count,
			R.id.disc_no,
			R.id.disc_count,
			R.id.year,
			R.id.comment
		};
	
		private EditText[] mEditTexts = new EditText[ITEM_NUM];
		private CheckBox compilationCheckBox;

		private boolean isEditing;
		
		private static InfoFragment newInstance(Track track)
		{
			InfoFragment fragment = new InfoFragment();
		//	Bundle bundle = new Bundle();
		//	bundle.putSerializable("TRACK", track);
		//	fragment.setArguments(bundle);
			
			return fragment;
		}
		
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
		{
			// mTrack = (Track)getArguments().getSerializable("TRACK");
			
			View view = inflater.inflate(R.layout.fragment_track_detail_info, container, false);
			
			for(int i = 0; i < ITEM_NUM; i++){
				mEditTexts[i] = (EditText)view.findViewById(IDS[i]);
				mEditTexts[i].addTextChangedListener(new MyTextWatcher(i));
			}

			compilationCheckBox = (CheckBox)view.findViewById(R.id.compilation);
			compilationCheckBox.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					String values = (compilationCheckBox.isChecked())? "1" : "0";
					((TrackDetailActivity)getActivity()).saveTagChanges(TagLibHelper.KEY_COMPILATION, values);
				}
			});
		
			updateView();
			
			return view;
		}

		private void updateView()
		{
			SparseArray<String> tag = ((TrackDetailActivity)getActivity()).getTag();

			isEditing = true;
			for(int i = 0; i < ITEM_NUM; i++){
				String value = tag.get(KEYS[i]);

				// 複数フィールドの値でタグ1つのとき
				switch(IDS[i]){
					case R.id.track_no:
					case R.id.disc_no: {
						String[] values = value.split("/", 2);
						value = (values.length >= 1) ? values[0] : "";
						break;
					}
					case R.id.track_count:
					case R.id.disc_count: {
						String[] values = value.split("/", 2);
						value = (values.length >= 2) ? values[1] : "";
						break;
					}
				}

				mEditTexts[i].setText(value);
			}
			isEditing = false;

			boolean isCompilation = false;
			try{
				isCompilation = (Integer.parseInt(tag.get(TagLibHelper.KEY_COMPILATION)) > 0);
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
					case R.id.track_no:
					case R.id.disc_no:
						values = values + "/" + mEditTexts[mNumber+1].getText().toString();
						break;
					case R.id.track_count:
					case R.id.disc_count:
						values = mEditTexts[mNumber-1].getText().toString() + "/" + values;
						break;
				}

				((TrackDetailActivity)getActivity()).saveTagChanges(KEYS[mNumber], values);
			}

			@Override
			public void afterTextChanged(Editable s)
			{
			}
		}
	}
	
	public static class ArtworkFragment extends Fragment
	{
		private Track mTrack;
	
		private ImageView artworkImageView;
		
		
		private static ArtworkFragment newInstance(Track track)
		{
			ArtworkFragment fragment = new ArtworkFragment();
			Bundle bundle = new Bundle();
			bundle.putSerializable("TRACK", track);
			fragment.setArguments(bundle);

			return fragment;
		}
		
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
		{
			mTrack = (Track)getArguments().getSerializable("TRACK");
			
			View view = inflater.inflate(R.layout.fragment_track_detail_artwork, container, false);
			artworkImageView = (ImageView)view.findViewById(R.id.artwork);
			
			updateView();
			
			return view;
		}
		
		private void updateView()
		{
			ArtworkCache.Large.setArtworkView(artworkImageView, mTrack);
		}
	}
	
	public static class LyricsFragment extends Fragment
	{
		private EditText lyricsEditText;

		private boolean isEditing;
		
		private static LyricsFragment newInstance(Track track)
		{
			LyricsFragment fragment = new LyricsFragment();
		//	Bundle bundle = new Bundle();
		//	bundle.putSerializable("TRACK", track);
		//	fragment.setArguments(bundle);
			
			return fragment;
		}
		
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
		{
			// mTrack = (Track)getArguments().getSerializable("TRACK");
			
			View view = inflater.inflate(R.layout.fragment_track_detail_lyrics, container, false);
			lyricsEditText = (EditText)view.findViewById(R.id.lyrics);
			lyricsEditText.addTextChangedListener(new MyTextWatcher());
			
			updateView();
			
			return view;
		}
		
		private void updateView()
		{
			isEditing = true;
			SparseArray<String> tag = ((TrackDetailActivity)getActivity()).getTag();
			lyricsEditText.setText(tag.get(TagLibHelper.KEY_LYRICS));
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
				R.id.title,
				R.id.title_sort,
				R.id.artist,
				R.id.artist_sort,
				R.id.album,
				R.id.album_sort,
				R.id.album_artist,
				R.id.album_artist_sort
		};

		private EditText[] mEditTexts = new EditText[ITEM_NUM];

		private boolean isEditing;

		private static SortFragment newInstance(Track track)
		{
			SortFragment fragment = new SortFragment();
		//	Bundle bundle = new Bundle();
		//	bundle.putSerializable("TRACK", track);
		//	fragment.setArguments(bundle);

			return fragment;
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
		{
			// mTrack = (Track)getArguments().getSerializable("TRACK");

			View view = inflater.inflate(R.layout.fragment_track_detail_sort, container, false);

			for(int i = 0; i < ITEM_NUM; i++){
				mEditTexts[i] = (EditText)view.findViewById(IDS[i]);
				mEditTexts[i].addTextChangedListener(new MyTextWatcher(i));
			}

			updateView();

			return view;
		}

		private void updateView()
		{
			SparseArray<String> tag = ((TrackDetailActivity)getActivity()).getTag();

			isEditing = true;
			for(int i = 0; i < ITEM_NUM; i++){
				mEditTexts[i].setText(tag.get(KEYS[i]));
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
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
		{
			mTrack = (Track)getArguments().getSerializable("TRACK");

			View view = inflater.inflate(R.layout.fragment_track_detail_file, container, false);

			pathTextView = (TextView)view.findViewById(R.id.path);

			lengthTextView = (TextView)view.findViewById(R.id.length);
			bitrateTextView = (TextView)view.findViewById(R.id.bitrate);
			sampleRateTextView = (TextView)view.findViewById(R.id.sample_rate);
			channelsTextView = (TextView)view.findViewById(R.id.channels);

			updateView();

			return view;
		}

		private void updateView()
		{
			pathTextView.setText(mTrack.path);

			//! EXPERIMENTAL
			TagLibHelper tagHelper = new TagLibHelper();
			tagHelper.setFile(mTrack.path);

			int length = tagHelper.getLength();
			lengthTextView.setText(String.format("%d:%02d", length / 60, length % 60));
			bitrateTextView.setText(tagHelper.getBitrate() + "kbps");
			sampleRateTextView.setText((tagHelper.getSampleRate() / 1000.0) + "kHz");
			channelsTextView.setText(Integer.toString(tagHelper.getChannels()));

			tagHelper.release();
		}
	}
	
}
