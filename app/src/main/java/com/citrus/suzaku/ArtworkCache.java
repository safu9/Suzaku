package com.citrus.suzaku;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.ImageView;

import com.citrus.suzaku.album.Album;
import com.citrus.suzaku.track.Track;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


// アルバムアートワークの読み込み
public class ArtworkCache
{
	private static final int HASH_LENGTH = 32;
	
	public static void initialize()
	{
		String status = Environment.getExternalStorageState();
		if(!status.equals(Environment.MEDIA_MOUNTED)){
			return;
		}
		
		Small.initialize();
		Large.initialize();
	}
	
	public static void release()
	{
		Small.release();
		Large.release();
	}

	// ファイルキャッシュ

	private static Bitmap createArtworkCacheFile(Track track, String dir, int size, int quality)
	{
		Log.d("Suzaku", "AC Creating Cache  hash : " + track.artworkHash);

		File file = new File(track.path);
		if(!file.exists()){
			return null;
		}

		// MediaMetadataRetriever retriever = new MediaMetadataRetriever();
		// retriever.setDataSource(track.path);
		// byte[] bytes = retriever.getEmbeddedPicture();

		TagLibHelper tag = new TagLibHelper();
		tag.setFile(track.path);
		byte[] bytes = tag.getArtwork();
		tag.release();

		if(bytes == null){
			return null;
		}

		Bitmap bmp = decodeBitmap(bytes, size);
		if(bmp == null){
			return null;
		}

		try{
			String fileName = track.artworkHash + ".jpg";

			FileOutputStream fo = new FileOutputStream(new File(dir, fileName));
			bmp.compress(Bitmap.CompressFormat.JPEG, quality, fo);
			fo.close();

		}catch(FileNotFoundException e){
			e.printStackTrace();
		}catch(IOException e){
			e.printStackTrace();
		}

		return bmp;
	}	

	private static Bitmap loadArtworkCacheFile(String hash, String dir)
	{
		if(!isCorrectHash(hash)){
			return null;
		}

		String fileName = hash + ".jpg";
		File file = new File(dir, fileName);

		if(!file.exists()){
			return null;
		}

		return BitmapFactory.decodeFile(file.getPath());
	}
	
	// Bitmap 作成
	
	// １辺reqSizeの正方形に収まるように読み込み
	private static Bitmap decodeBitmap(byte[] bytes, int reqSize)
	{
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPreferredConfig = Bitmap.Config.RGB_565;
		
		// メモリに展開せずに情報だけ取得
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
		
		// 出来るだけ縮小
		int size = Math.max(options.outWidth, options.outHeight);	// 画像の元サイズ
		int inSampleSize = 1;										// 縮小率(2の乗数)

		while(size / inSampleSize > reqSize * 2){
			inSampleSize *= 2;
		}
		options.inSampleSize = inSampleSize;
		
		// 読み込み
		options.inJustDecodeBounds = false;
		Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);

		if(bitmap == null){
			// 不正なデータ
			App.loge("decodeBitmap Error  hash : " + getHash(bytes));
			return null;
		}

		bitmap = matrixSquareBitmap(bitmap, reqSize);
		return bitmap;
	}

	// 正方形内にピッタリ縮小
	private static Bitmap matrixSquareBitmap(Bitmap bitmap, int reqSize)
	{
		int width = bitmap.getWidth(), height = bitmap.getHeight();
		float scale = (float)reqSize / Math.max(width, height);
		
		if(scale >= 1){
			return bitmap;
		}
		
		Matrix matrix = new Matrix();
		matrix.postScale(scale, scale);

		return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
	}
	
	// アートワークハッシュ

	public static String getHash(byte[] bytes)
	{
		if(bytes == null){
			return null;
		}
		
		StringBuilder builder = new StringBuilder();

		try{
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] hash = md.digest(bytes);

			// 16進文字列の作成
			//! USE Apache Commons
			for(byte num : hash){				// 2桁ずつ0でパディング
				builder.append(String.format("%02X", num & 0xff));
			}

		}catch(NoSuchAlgorithmException e){
			e.printStackTrace();
			return null;
		}

		return builder.toString();
	}

	public static boolean isCorrectHash(String hash)
	{
		return (hash != null && hash.length() == HASH_LENGTH);
	}
	
	//
	public static class Small
	{
		private static final int SIZE;
		static{
			SIZE = App.dp2px(48);
		}
		
		private static String dir;
		private static LruCache<String, Bitmap> cache;

		
		public static void initialize()
		{
			if(cache != null){
				return;
			}
			
			int maxMem = ((ActivityManager)App.getContext().getSystemService(Activity.ACTIVITY_SERVICE)).getMemoryClass() * 1024 * 1024;

			// メモリ使用量に注意!
			cache = new LruCache<String, Bitmap>(maxMem / 8){
				@Override
				protected int sizeOf(String key, Bitmap bitmap){
					return bitmap.getByteCount();
					// bitmap.getRowBytes() * bitmap.getHeight();
				}
			};

			File extDir = App.getContext().getExternalCacheDir();
			if(extDir != null){
				dir = extDir.getAbsolutePath() + "/SMALL";
				new File(dir).mkdirs();
			}
		}

		public static void release()
		{
			cache.evictAll();
			cache = null;
		}

		// ImageView に画像を設定

		public static void setArtworkView(ImageView image, Track track)
		{
			if(!isCorrectHash(track.artworkHash)){
				return;
			}

			Bitmap bmp = cache.get(track.artworkHash);

			if(bmp != null){
				image.setImageBitmap(bmp);
			}else{
				new ImageGetTask(track, image).executeOnExecutor(ImageGetTask.MY_THREAD_POOL_EXECUTOR);
			}
		}

		public static void setArtworkView(ImageView image, Album album)
		{
			if(!isCorrectHash(album.artworkHash)){
				return;
			}

			Bitmap bmp = cache.get(album.artworkHash);

			if(bmp != null){
				image.setImageBitmap(bmp);
			}else{
				new ImageGetTask(album.getTracks().get(0), image).executeOnExecutor(ImageGetTask.MY_THREAD_POOL_EXECUTOR);
			}
		}

		// メモリキャッシュ

		private static Bitmap setArtworkCache(String hash, Bitmap artwork)
		{
			if(cache.get(hash) != null){
				return cache.get(hash);
			}else{
				cache.put(hash, artwork);
				return artwork;
			}
		}

		// AsyncTask : ImageView に画像を設定
		private static class ImageGetTask extends AsyncTask<Void, Void, Bitmap>
		{
			private Track track;
			private ImageView image;
		
			public ImageGetTask(Track track, ImageView image)
			{
				super();

				this.track = track;
				this.image = image;

				if(image != null){
					image.setTag(track.artworkHash);
				}
			}

			public ImageGetTask(Track track)
			{
				super();

				this.track = track;
				image = null;
			}

			@Override
			protected Bitmap doInBackground(Void[] v)
			{
				Bitmap artwork;

				artwork = loadArtworkCacheFile(track.artworkHash, dir);

				if(artwork == null){
					artwork = createArtworkCacheFile(track, dir, SIZE, 100);
				}

				if(artwork != null){
					artwork = Small.setArtworkCache(track.artworkHash, artwork);
				}

				return artwork;
			}

			@Override
			protected void onPostExecute(Bitmap result)
			{
				if(result == null){
					return;
				}

				if(image != null && track.artworkHash.equals(image.getTag())){
					image.setImageBitmap(result);
				}

				super.onPostExecute(result);
			}
			
			// ThreadPool (RejectedExecutionException が出ないように一番古いタスクを破棄)
			
			private static final int CORE_POOL_SIZE = 5;
			private static final int MAXIMUM_POOL_SIZE = 128;
			private static final int KEEP_ALIVE = 1;
			private static final ThreadFactory sThreadFactory = new ThreadFactory(){
				private final AtomicInteger mCount = new AtomicInteger(1);
				@Override
				public Thread newThread(@NonNull Runnable r){
					return new Thread(r, "AsyncTask #" + mCount.getAndIncrement());
				}
			};
			private static final BlockingQueue<Runnable> sPoolWorkQueue = new LinkedBlockingQueue<>(10);
			public static final Executor MY_THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE, TimeUnit.SECONDS, sPoolWorkQueue, sThreadFactory, new ThreadPoolExecutor.DiscardOldestPolicy());
		}
	}
	
	public static class Large
	{
		private static final int SIZE;
		static{
			WindowManager wm = (WindowManager)App.getContext().getSystemService(Context.WINDOW_SERVICE);
			Display disp = wm.getDefaultDisplay();
			Point size = new Point();
			disp.getSize(size);
			SIZE = Math.min(size.x, size.y);
		}

		private static String dir;
		private static LruCache<String, Bitmap> cache;

//		private static String largeHash;
//		private static Bitmap largeCache;


		public static void initialize()
		{
			cache = new LruCache<>(2);			// 2枚まで

			File extDir = App.getContext().getExternalCacheDir();
			if(extDir != null){
				dir = extDir.getAbsolutePath() + "/LARGE";
				new File(dir).mkdirs();
			}
		}

		public static void release()
		{
			cache.evictAll();
			cache = null;
		}

		// ImageView に画像を設定

		public static void setArtworkView(ImageView image, Track track)
		{
			if(!isCorrectHash(track.artworkHash)){
				return;
			}

			Bitmap bmp = cache.get(track.artworkHash);

			if(bmp != null){
				image.setImageBitmap(bmp);
			}else{
				new ImageGetTask(track, image, false).execute();
			}
		}

		public static void setArtworkViewWithCache(ImageView image, Track track)
		{
			if(!isCorrectHash(track.artworkHash)){
				return;
			}

			Bitmap bmp = cache.get(track.artworkHash);

			if(bmp != null){
				image.setImageBitmap(bmp);
			}else{
				new ImageGetTask(track, image, true).execute();
			}
		}

		public static void setArtworkView(ImageView image, Album album)
		{
			if(!isCorrectHash(album.artworkHash)){
				return;
			}

			Bitmap bmp = cache.get(album.artworkHash);

			if(bmp != null){
				image.setImageBitmap(bmp);
			}else{
				new ImageGetTask(album.getTracks().get(0), image, false).execute();
			}
		}

		// メモリキャッシュ

		private static Bitmap setArtworkCache(String hash, Bitmap artwork)
		{
			if(cache.get(hash) != null){
				return cache.get(hash);
			}else{
				cache.put(hash, artwork);
				return artwork;
			}
		}

		public static Bitmap getArtworkCache(Track track)
		{
			if(!isCorrectHash(track.artworkHash)){
				return null;
			}

			Bitmap artwork = cache.get(track.artworkHash);
			if(artwork != null && artwork.isRecycled()){
				artwork = null;
				cache.remove(track.artworkHash);
				App.logd("AC Bitmap has been recycled!");
			}
			return artwork;
		}

		// AsyncTask : ImageView に画像を設定
		public static class ImageGetTask extends AsyncTask<Void, Void, Bitmap>
		{
			private Track track;
			private ImageView image;
			private boolean saveCache;

			public ImageGetTask(Track track, ImageView image, boolean saveCache)
			{
				super();

				this.track = track;
				this.image = image;
				this.saveCache = saveCache;

				if(image != null){
					image.setTag(track.artworkHash);
				}
			}

			@Override
			protected Bitmap doInBackground(Void[] v)
			{
				Bitmap artwork;

				artwork = loadArtworkCacheFile(track.artworkHash, dir);

				if(artwork == null){
					artwork = createArtworkCacheFile(track, dir, SIZE, 90);
				}

				if(saveCache){
					Large.setArtworkCache(track.artworkHash, artwork);
				}

				return artwork;
			}

			@Override
			protected void onPostExecute(Bitmap result)
			{
				if(result == null){
					return;
				}

				if(image != null && track.artworkHash.equals(image.getTag())){
					image.setImageBitmap(result);
				}

				super.onPostExecute(result);
			}
		}
	}
	
}
