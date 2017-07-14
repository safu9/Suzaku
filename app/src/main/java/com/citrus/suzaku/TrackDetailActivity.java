package com.citrus.suzaku;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
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

import java.util.ArrayList;
import java.util.List;


public class TrackDetailActivity extends AppCompatActivity
{
	private List<Track> mTracks;
	private Track mTrack;

	private SparseArray<String> mTag = new SparseArray<>();
	private SparseArray<String> mChangedTag = new SparseArray<>();

	private DatabaseBroadcastReceiver receiver;

	private TabLayout mTabs;
	private ViewPager mViewPager;

	private MyPagerAdapter mPagerAdapter;

	private int mPosition = 0;
	
	
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

		mViewPager.setCurrentItem(mPosition);
		mViewPager.addOnPageChangeListener(new PageChangeListener());

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
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId()){
			case android.R.id.home:				// up button
				//! TENTATIVE
				finish();
				return true;
				
			case R.id.menu_save:
				saveTrackInfo();
				finish();
				return true;

			default:
				return super.onOptionsItemSelected(item);
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
		if(size == 0){
			return;
		}

		// ファイルに書き込み
		TagLibHelper tagHelper = new TagLibHelper();
		tagHelper.setFile(mTrack.path);

		for(int i = 0; i < size; i++){
			tagHelper.setTag(mChangedTag.keyAt(i), mChangedTag.valueAt(i));
		}
		boolean ret = tagHelper.saveTag();
		tagHelper.release();

		if(ret){
			// データベースを更新
			Intent intent = new Intent(MusicDBService.ACTION_UPDATE_TRACK);
			intent.putExtra(MusicDBService.INTENT_KEY_PATH, mTrack.path);
			intent.setPackage(App.PACKAGE);
			startService(intent);

			Toast.makeText(this, R.string.notify_saved_changes, Toast.LENGTH_SHORT).show();
		}else{
			Toast.makeText(this, R.string.cant_save_changes, Toast.LENGTH_SHORT).show();
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
					return getString(R.string.file);
			}
			return null;
		}

		public Fragment getFragmentAtPosition(int position)
		{
			return (Fragment)instantiateItem(mViewPager, position);
		}
	}
	
	private class PageChangeListener implements ViewPager.OnPageChangeListener
	{
		@Override
		public void onPageSelected(int position)
		{
			mPosition = position;
		}

		@Override
		public void onPageScrollStateChanged(int state)
		{
		}

		@Override
		public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels)
		{
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
