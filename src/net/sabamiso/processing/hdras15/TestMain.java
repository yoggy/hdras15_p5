package net.sabamiso.processing.hdras15;

import processing.core.PApplet;
import processing.core.PImage;

public class TestMain extends PApplet{
	private static final long serialVersionUID = 8657253119950039897L;

	HDRAS15 hdras15_img = new HDRAS15();
	
	public void setup() {
		size(640, 360);
		
		boolean rv;
		rv = hdras15_img.connect();
		if (rv == false) {
			println("error: connect() failed...");
			return;
		}
	}
	
	public void draw() {
		PImage img = hdras15_img.getImage();
		if (img != null) {
			image(img, 0, 0);
		}
	}
	
	public static void main(String[] args) {
		PApplet.main(new String[] { "net.sabamiso.processing.hdras15.TestMain"});
	}
}
