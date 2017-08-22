package com.citrus.suzaku.base;

import com.citrus.suzaku.track.Track;

import java.util.*;
import java.io.*;

public abstract class TrackGroup implements Serializable
{
	private static final long serialVersionUID = 1L;

	public abstract List<Track> getTracks();
	public abstract List<Long> getTrackIds();
}
