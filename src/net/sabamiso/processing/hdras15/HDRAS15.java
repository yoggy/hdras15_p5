package net.sabamiso.processing.hdras15;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import javax.imageio.ImageIO;

import processing.core.PImage;

public class HDRAS15 extends Thread {
	String host = "10.0.0.1";
	int control_port = 10000;
	int liveview_port = 60152;
	boolean is_connect = false;

	PImage image;

	Socket socket;
	Thread thread;

	public HDRAS15() {
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getControl_port() {
		return control_port;
	}

	public void setControl_port(int control_port) {
		this.control_port = control_port;
	}

	public int getLiveview_port() {
		return liveview_port;
	}

	public void setLiveview_port(int liveview_port) {
		this.liveview_port = liveview_port;
	}

	private boolean isConnect() {
		return is_connect;
	}

	synchronized void setPImage(PImage img) {
		this.image = img;
	}

	public synchronized PImage getImage() {
		if (isConnect() == false)
			return null;
		return image;
	}

	boolean request_liveview() {
		try {
			Socket s = new Socket(host, control_port);
			InputStream is = s.getInputStream();
			OutputStream os = s.getOutputStream();

			String req = "POST /camera HTTP/1.1\r\nContent-Length: 45\r\n\r\n{\"method\":\"startLiveview\",\"params\":[],\"id\":6}";
			os.write(req.getBytes());

			// dummy
			byte[] buf = new byte[256];
			is.read(buf, 0, 256);

			// for debug..
			System.out.println(new String(buf));

			s.close();

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	boolean start_liveview() {
		try {
			socket = new Socket(host, liveview_port);
			OutputStream os = socket.getOutputStream();

			String req = "GET /liveview.JPG?%211234%21http%2dget%3a%2a%3aimage%2fjpeg%3a%2a%21%21%21%21%21 HTTP/1.1\r\n" 
					+ "Host: " + host + ":" + liveview_port + "\r\n" 
					+ "Connection: Keep-Alive\r\nAccept-Encoding: gzip\r\n\r\n";
			os.write(req.getBytes());

			// thread start...
			start();
		} catch (Exception e) {
			e.printStackTrace();
			try {
				socket.close();
			} catch (Exception e1) {
			}
			socket = null;
			return false;
		}

		return true;
	}

	private String read_until(InputStream is, String end_str) throws Exception {
		String recv_data;
		byte[] buf = new byte[256];
		byte[] eos = end_str.getBytes();
		int size = 0;
		while (true) {
			int c = is.read();
			if (c < 0)
				throw new Exception("read_until() failed...");

			buf[size] = (byte) c;
			size += 1;

			// check eos
			if (size > end_str.length()) {
				boolean flag = true;
				for (int i = 0; i < eos.length; ++i) {
					if (buf[size - eos.length + i] != eos[i]) {
						flag = false;
						break;
					}
				}
				if (flag == true)
					break;
			}
		}

		recv_data = new String(buf, 0, size - end_str.length()); // remove
																	// end_str
		return recv_data;
	}

	private int read_chunk_size(InputStream is) throws Exception {
		String recv_data = read_until(is, "\r\n");
		if (recv_data == null || recv_data.length() == 0)
			throw new Exception("read_chunk_size() failed...");

		return Integer.parseInt(recv_data, 16); // size string is HEX...
	}

	private byte[] read_chunk_data(InputStream is, int buf_size)
			throws Exception {
		byte[] buf = new byte[buf_size];

		int size = 0;
		while (true) {
			size += is.read(buf, size, buf_size - size);
			if (size == buf_size)
				break;
		}

		// read "\r\n"
		int cr = is.read();
		int lf = is.read();
		if (cr != 13 || lf != 10) {
			throw new Exception("read_chunk_data() failed...");
		}

		return buf;
	}

	private void decode_jpeg_data(byte[] jpeg_buf, int size) throws Exception {
		ByteArrayInputStream bis = new ByteArrayInputStream(jpeg_buf);
		BufferedImage bufferd_img = null;
		bufferd_img = ImageIO.read(bis);
		bis.close();

		int w = bufferd_img.getWidth();
		int h = bufferd_img.getHeight();

		Raster raster = bufferd_img.getRaster();
		DataBufferByte data_buffer = (DataBufferByte) (raster.getDataBuffer());
		byte[] buf = data_buffer.getData();
		PImage pimg = new PImage(w, h, PImage.ARGB);

		for (int y = 0; y < h; ++y) {
			for (int x = 0; x < w; ++x) {
				int buf_idx = x * 3 + y * w * 3;
				byte b = buf[buf_idx + 0];
				byte g = buf[buf_idx + 1];
				byte r = buf[buf_idx + 2];

				int c = (b << 0) & 0x000000ff | (g << 8) & 0x0000ff00
						| (r << 16) & 0x00ff0000 | (0xff << 24) & 0xff000000;

				int img_idx = x + y * w;
				pimg.pixels[img_idx] = c;
			}
		}
		pimg.updatePixels();

		setPImage(pimg);
	}

	// read thread
	public void run() {
		try {
			InputStream is = socket.getInputStream();
			int size;
			byte[] buf;

			read_until(is, "\r\n\r\n");

			while (true) {
				size = read_chunk_size(is);
				buf = read_chunk_data(is, size);
				if (size > 1000) {
					decode_jpeg_data(buf, size);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public boolean connect() {
		if (isConnect() == true)
			return true;

		if (request_liveview() == false) {
			System.err.println("err: request_start_liveview() failed...");
			return false;
		}

		if (start_liveview() == false) {
			System.err.println("err: start_liveview() failed...");
			return false;
		}

		is_connect = true;

		return true;
	}

	public void close() {
		if (isConnect() == true) {
			try {
				socket.close();
			} catch (Exception e) {
			}
			socket = null;
		}

		is_connect = false;
	}
}
