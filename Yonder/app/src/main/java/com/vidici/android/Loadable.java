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
	boolean downloading = false;
	boolean canPlay = false;

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

	public void setPlayable() {
		canPlay = true;
	}

	public boolean isPlayable() {
		return canPlay;
	}

	public void setEmpty (boolean empty) {
		this.empty = empty;
	}

	public boolean isEmpty () {
		return empty;
	}

	public boolean isVideosEmpty () {
		return videos.isEmpty();
	}

	public void setDownloading (boolean downloading) {
		this.downloading = downloading;
	}

	public boolean isDownloading() {
		return downloading;
	}

	public void setReload() {
//		videos.clear();
	}

	public void addVideo(String video) {
		videos.add(video);
	}
}
