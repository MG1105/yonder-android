package com.vidici.android;

import java.io.File;
import java.util.ArrayList;

class Loadable {

	ArrayList<String> videos = new ArrayList<>();
	protected String id;
	String videoId = "";
	String channelId = "";
	String thumbnailId = "";
	Boolean empty = false;
	boolean fetchingVideos = false;
	int loaded = 0;

	public String getId() {
		return id;
	}

	public String getVideoId() {
		return videoId;
	}

	public String getChannelId() {
		return channelId;
	}

	public String getThumbnailId() {
		return thumbnailId;
	}

	public int getRemaining() {
		int count = 0;
		if (empty)
			return -2;
		if (videos.isEmpty())
			return -1;
		for (String id : videos) {
			if (!new File(Video.loadedDir.getAbsolutePath()+"/"+id+".mp4").isFile()){
				count++;
			} else {
				loaded++;
			}
		}
		return count;
	}

	public boolean canPlay() {
		if (loaded > 0) {
			return true;
		}
		return false;
	}

	public void setEmpty (boolean empty) {
		this.empty = empty;
	}

	public void setFetchingVideos (boolean fetching) {
		fetchingVideos = fetching;
	}

	public boolean isFetchingVideos() {
		return fetchingVideos;
	}

	public void setReload() {
//		videos.clear();
	}

	public void addVideo(String video) {
		videos.add(video);
	}
}
