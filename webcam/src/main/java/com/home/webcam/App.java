package com.home.webcam;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;

import com.github.sarxos.webcam.Webcam;

public class App implements NativeKeyListener {

	static int pos[] = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

	static boolean monitoring = false;

	public static void registerPos(int val) {
		for (int i = 0; i < 9; i++) {
			pos[i] = pos[i + 1];
		}
		pos[9] = val;
	}

	public static void printPos() {
		String s = "";
		for (int i = 0; i < 10; i++)
			s += " " + pos[i];
		System.out.println(s);
	}

	public static void resetPos() {
		for (int i = 0; i < 10; i++)
			pos[i] = 0;
	}

	public static void analysePos(Robot robot) {
		int diff = 0;
		if (pos[4] != 0) {
			int non_zero = 0;
			for (int i = 5; i < 10; i++) {
				if (pos[i] != 0 && pos[i - 1] != 0) {
					diff += (pos[i] - pos[i - 1]);
					non_zero++;
				}
			}
			if (non_zero > 3) {
				System.out.println(diff);
				resetPos();
				if (monitoring) {
					if (diff > 0)
						robot.keyPress(KeyEvent.VK_DOWN);
					else
						robot.keyPress(KeyEvent.VK_UP);
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	public static BufferedImage grayScale(BufferedImage image) {
		BufferedImage result = new BufferedImage(image.getWidth(),
				image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
		result.getGraphics().drawImage(image, 0, 0, null);
		return result;
	}

	public void start() throws AWTException {
		Webcam webcam = Webcam.getWebcams().get(0);
		Robot robot = new Robot();
		if (webcam != null) {
			webcam.setViewSize(new Dimension(176, 144));
			webcam.open();
			JFrame f = new JFrame("Panel Example");
			JPanel panel = new JPanel();
			JLabel picLabel = null;
			panel.setBounds(0, 0, 320, 240);
			f.add(panel);
			f.setSize(320, 240);
			f.setLayout(null);
			f.setVisible(true);
			BufferedImage current_frame = null, previous_frame = null, image_to_plot = null;

			while (true) {
				current_frame = webcam.getImage();
				current_frame = grayScale(current_frame);
				if (previous_frame != null) {
					image_to_plot = getCompare(current_frame, previous_frame);
					image_to_plot = erosion(image_to_plot);
					image_to_plot = plot_center(image_to_plot, robot);
					if (picLabel != null)
						panel.remove(picLabel);
					picLabel = new JLabel(new ImageIcon(image_to_plot));
					panel.add(picLabel);
					panel.revalidate();
					panel.repaint();
				}
				previous_frame = current_frame;
			}
			// webcam.close();
		} else {
			System.out.println("No webcam detected");
		}
	}

	public static void main(String[] args) throws IOException, AWTException,
			NativeHookException {
		App app = new App();
		GlobalScreen.registerNativeHook();
		GlobalScreen.addNativeKeyListener(app);
		app.start();
	}

	private static BufferedImage plot_center(BufferedImage img, Robot robot) {
		int width = img.getWidth();
		int height = img.getHeight();
		BufferedImage result = new BufferedImage(width, height,
				BufferedImage.TYPE_BYTE_BINARY);
		result.getGraphics().drawImage(img, 0, 0, null);
		Raster raster_in = img.getRaster();
		WritableRaster raster_out = result.getRaster();
		int[] pixels = new int[1];
		int[] black_pixel = { 0 };
		int[] white_pixel = { 1 };
		long sum_x = 0;
		long sum_y = 0;
		long count = 0;
		for (int y = 1; y < height - 1; y++) {
			for (int x = 1; x < width - 1; x++) {
				raster_in.getPixels(x, y, 1, 1, pixels);
				if (pixels[0] == 1) {
					sum_x += x;
					sum_y += y;
					count++;
				}
				raster_out.setPixel(x, y, black_pixel);
			}
		}
		if (count != 0) {
			sum_x = sum_x / count;
			sum_y = sum_y / count;
			raster_out.setPixel((int) sum_x, (int) sum_y, white_pixel);
			registerPos((int) sum_y);
		} else
			registerPos(0);
		// printPos();
		analysePos(robot);
		return result;
	}

	public static BufferedImage erosion(BufferedImage img) {

		int width = img.getWidth();
		int height = img.getHeight();
		BufferedImage result = new BufferedImage(width, height,
				BufferedImage.TYPE_BYTE_BINARY);
		result.getGraphics().drawImage(img, 0, 0, null);
		Raster raster_in = img.getRaster();
		WritableRaster raster_out = result.getRaster();
		int[] pixels = new int[9];
		int[] black_pixel = { 0 };
		int[] white_pixel = { 1 };
		int score;
		for (int y = 1; y < height - 1; y++) {
			for (int x = 1; x < width - 1; x++) {
				raster_in.getPixels(x - 1, y - 1, 3, 3, pixels);
				score = 0;
				for (int i = 0; i < 9; i++)
					score += pixels[i];
				if (score > 5)
					raster_out.setPixel(x, y, white_pixel);
				else
					raster_out.setPixel(x, y, black_pixel);
			}
		}
		for (int y = 0; y < height; y++) {
			raster_out.setPixel(0, y, black_pixel);
			raster_out.setPixel(width - 1, y, black_pixel);
		}
		for (int x = 0; x < width; x++) {
			raster_out.setPixel(x, 0, black_pixel);
			raster_out.setPixel(x, height - 1, black_pixel);
		}
		return result;
	}

	private static BufferedImage getCompare(BufferedImage current_frame,
			BufferedImage previous_frame) {
		BufferedImage result = new BufferedImage(current_frame.getWidth(),
				current_frame.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
		WritableRaster raster = result.getRaster();
		int height = current_frame.getHeight();
		int width = current_frame.getWidth();
		int[] pixels = new int[width + 1];
		int[] current_frame_pixels = new int[width + 1];
		int[] previous_frame_pixels = new int[width + 1];

		for (int y = 0; y < height; y++) {
			raster.getPixels(0, y, width, 1, pixels);
			current_frame.getRaster().getPixels(0, y, width, 1,
					current_frame_pixels);
			previous_frame.getRaster().getPixels(0, y, width, 1,
					previous_frame_pixels);
			for (int x = 0; x < pixels.length; x++) {
				if (Math.abs(current_frame_pixels[x] - previous_frame_pixels[x]) < 20) {
					pixels[x] = 0;
				} else {
					pixels[x] = 1;
				}
			}
			raster.setPixels(0, y, width, 1, pixels);
		}
		return result;
	}

	public void nativeKeyPressed(NativeKeyEvent arg0) {
		if (NativeKeyEvent.getKeyText(arg0.getKeyCode()).contains("Control")) {
			monitoring = !(monitoring);
			System.out.println(monitoring);
		}
	}

	public void nativeKeyReleased(NativeKeyEvent arg0) {

	}

	public void nativeKeyTyped(NativeKeyEvent arg0) {

	}
}