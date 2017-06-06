package com.citrus.suzaku;

import java.io.*;
import java.util.*;


// Use in PlayerService
public class PlaylistManager
{
	public static final int PLAY_RANGE_ALL = 1;
	public static final int PLAY_RANGE_TRACKS = 2;
	public static final int PLAY_RANGE_QUEUE = 3;
	
	public static final int LOOPMODE_OFF = 0;
	public static final int LOOPMODE_ALL = 1;
	public static final int LOOPMODE_ONE = 2;

	private static final String FILE_PLAYLIST = "playlist.dat";

	
	private int playRange;
	private TrackGroup item;
	
	private List<Track> playlist;
	private int currentPosition;
		
	private int loopMode = 0;
	private boolean isOneLooping = false;
	private boolean isAllLooping = false;
		
	private boolean shuffleMode = false;
	private ArrayList<Integer> shuffleList;
	
	private int startTime = 0;
	
	private long fileLastModified = 0;
	
	
	public PlaylistManager()
	{
		initalize();
	}
	
	private void initalize()
	{
		playRange = 0;
		item = null;
		
		playlist = null;
		currentPosition = 0;
		
		shuffleList = null;
		
		startTime = 0;
	}
	
	public void setPlaylistInfo(int playRange, TrackGroup item, int position, boolean shuffleStart)
	{
		this.playRange = playRange;
		this.item = item;
		this.currentPosition = position;
		
		queryPlaylist();
		
		if(shuffleStart){					// start with random position
			shuffleMode = true;
			currentPosition = (int)(Math.random() * playlist.size());
		}
		setShuffleMode(shuffleMode);
		
		startTime = 0;
	}
	
	private void queryPlaylist()
	{
		MusicDB mdb = new MusicDB();
		
		switch(playRange){
			case PLAY_RANGE_ALL:
				playlist = mdb.getAllTracks();
				break;
			case PLAY_RANGE_TRACKS:
				playlist = item.getTracks();
				break;
			case PLAY_RANGE_QUEUE:
			//	playlist = mdb.getQueueTracks();
				break;
			default:
				playlist = null;
		}
	}

	public List<Track> getPlaylist()
	{
		return playlist;
	}

	public Track getTrack(int no)
	{
		if(playlist == null || no < 0 || playlist.size() <= no){
			return null;
		}
		
		int pos = (!shuffleMode)? no : shuffleList.get(no);

		return playlist.get(pos);
	}

	public Track getCurrentTrack()
	{
		return getTrack(currentPosition);
	}

	public Track forwardTrack()
	{
		if(playlist == null){
			return null;
		}
		
		currentPosition++;

		if(currentPosition >= playlist.size()){
			if(isAllLooping){
				currentPosition = 0;
				if(shuffleMode){					// 再シャッフル
					Collections.shuffle(shuffleList);
				}
			}else{
				initalize();
				return null;
			}
		}

		startTime = 0;
		
		return getTrack(currentPosition);
	}

	public Track backTrack()
	{
		if(playlist == null){
			return null;
		}
		
		currentPosition--;
			
		if(currentPosition < 0){
			if(isAllLooping){
				currentPosition = playlist.size() - 1;
				if(shuffleMode){								// 再シャッフル
					Collections.shuffle(shuffleList);
				}
			}else{
				initalize();
				return null;
			}
		}
		
		startTime = 0;

		return getTrack(currentPosition);
	}

	public int getSize()
	{
		if(playlist == null){
			return 0;
		}

		return playlist.size();
	}

	//! UNUSE
	public void setCurrentPosition(int position)
	{
		currentPosition = position;
		startTime = 0;
	}

	public int getCurrentPosition()
	{
		return currentPosition;
	}

	public void setLoopMode(int loop)
	{
		loopMode = loop;
		switch(loop){
			case LOOPMODE_OFF:
				isOneLooping = false;
				isAllLooping = false;
				break;
			case LOOPMODE_ALL:
				isOneLooping = false;
				isAllLooping = true;
				break;
			case LOOPMODE_ONE:
				isOneLooping = true;
				isAllLooping = false;
				break;
		}
	}
	
	public void switchLoopMode()
	{
		switch(loopMode){
			case LOOPMODE_OFF:
				setLoopMode(LOOPMODE_ALL);
				break;
			case LOOPMODE_ALL:
				setLoopMode(LOOPMODE_ONE);
				break;
			case LOOPMODE_ONE:
				setLoopMode(LOOPMODE_OFF);
				break;
		}
	}
	
	public int getLoopMode()
	{
		return loopMode;
	}

	public void setShuffleMode(boolean shuffle)
	{
		shuffleMode = shuffle;

		if(playlist == null || playlist.size() == 0){
			return;
		}

		if(shuffleMode){
			shuffleList = new ArrayList<>();

			for(int i = 0; i < playlist.size(); i++){
				shuffleList.add(i);
			}

			Collections.shuffle(shuffleList);

			// 現在の位置を最初に
			int index = shuffleList.indexOf(currentPosition);
			shuffleList.set(index, shuffleList.get(0));
			shuffleList.set(0, currentPosition);

			currentPosition = 0;

		}else{
			if(shuffleList == null){
				return;
			}

			currentPosition = shuffleList.get(currentPosition);

			shuffleList = null;
		}
	}
	
	public void switchShuffleMode()
	{
		setShuffleMode(!shuffleMode);
	}
	
	public boolean getShuffleMode()
	{
		return shuffleMode;
	}
	
	public void setStartTime(int time)
	{
		startTime = time;
	}
	
	public int getStartTime()
	{
		return startTime;
	}
	
	// File IO
	
	public void save()
	{
		try{
			FileOutputStream fos = new FileOutputStream(App.getContext().getExternalFilesDir(null) + "/" + FILE_PLAYLIST);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			
			oos.writeInt(playRange);
			oos.writeObject(item);
			oos.writeInt(currentPosition);
			oos.writeInt(loopMode);
			oos.writeBoolean(shuffleMode);
			oos.writeObject(shuffleList);
			oos.writeInt(startTime);
			
			oos.close();
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	public void load()
	{
		try{
			File file = new File(App.getContext().getExternalFilesDir(null) + "/" + FILE_PLAYLIST);

			if(file.lastModified() == fileLastModified){
				return;
			}

			fileLastModified = file.lastModified();

			FileInputStream fis = new FileInputStream(file);
			ObjectInputStream ois = new ObjectInputStream(fis);
			
			playRange = ois.readInt();
			item = (TrackGroup)ois.readObject();
			currentPosition = ois.readInt();
			setLoopMode(ois.readInt());
			shuffleMode = ois.readBoolean();
			shuffleList = (ArrayList<Integer>)ois.readObject();
			startTime = ois.readInt();
			
			ois.close();
		}catch(IOException e){
			e.printStackTrace();
		}catch(ClassNotFoundException e){
			e.printStackTrace();
		}
		
		queryPlaylist();
	}
	
}
