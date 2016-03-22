package com.vidici.android;

import java.io.File;
import java.util.ArrayList;

class Loadable {

	ArrayList<String> videos = new ArrayList<>();
	protected String id;
	String videoId = "";
	String channelId = "";

	public String getId() {
		return id;
	}

	public String getVideoId() {
		return videoId;
	}

	public String getChannelId() {
		return channelId;
	}

	public int getRemaining() {
		int count = 0;
		if (videos.isEmpty())
			return -1;
		for (String id : videos) {
			if (!new File(Video.loadedDir.getAbsolutePath()+"/"+id+".mp4").isFile()){
				count++;
			}
		}
		return count;
	}

	public void setReload() {
		videos.clear();
	}

	public void addVideo(String video) {
		videos.add(video);
	}
}
