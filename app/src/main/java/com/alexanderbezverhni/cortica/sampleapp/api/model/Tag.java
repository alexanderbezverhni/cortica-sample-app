package com.alexanderbezverhni.cortica.sampleapp.api.model;

import java.util.List;

public class Tag {

	private String label;
	private List<Photo> photos;

	public String getLabel() {
		return label;
	}

	public List<Photo> getPhotos() {
		return photos;
	}
}
