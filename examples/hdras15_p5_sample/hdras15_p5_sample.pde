/**
 * Sample sketch for HDRAS15 library
 *
 * by yoggy, 2012
 * https://github.com/yoggy
 */
import net.sabamiso.processing.hdras15.*;

HDRAS15 hdras15;

void setup() {
  size(640, 360);

  hdras15 = new HDRAS15();

  // connect to Sony HDR-AS15
  boolean rv;
  rv = hdras15.connect();
  if (rv == false) {
    println("error: connect() failed...");
    return;
  }
}
	
void draw() {
  PImage img = hdras15.getImage();
  if (img != null) {
    image(img, 0, 0);
  }
}
