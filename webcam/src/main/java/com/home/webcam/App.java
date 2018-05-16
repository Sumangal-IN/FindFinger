package com.home.webcam;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.github.sarxos.webcam.Webcam;

public class App {
	static int max(int r, int g, int b) {
		if (r > g && r > b)
			return r;
		if (g > r && g > b)
			return g;
		return b;
	}

	static int min(int r, int g, int b) {
		if (r < g && r < b)
			return r;
		if (g < r && g < b)
			return g;
		return b;
	}

	static boolean isSkin(int r, int g, int b) {
		if (r > 95 && g > 40 && b > 20) {
			if ((max(r, g, b) - min(r, g, b)) > 15) {
				if (Math.abs(r - g) > 15 && r > g && r > b) {
					return true;
				}
			}
		}
		return false;
	}

	static boolean YCbCr(int r, int g, int b) {
		double Y = (0.257 * r) + (0.504 * g) + (0.098 * b) + 16;
		double Cb = -(0.148 * r) - (0.291 * g) + (0.439 * b) + 128;
		double Cr = (0.439 * r) - (0.368 * g) - (0.071 * b) + 128;
		if (Cr >= (1.5862 * Cb) + 20)
			return false;
		if (Cr <= ((0.3448 * Cb) + 76.2069))
			return false;
		if (Cr <= ((-4.5652 * Cb) + 234.5652))
			return false;
		if (Cr >= ((-1.15 * Cb) + 301.75))
			return false;
		if (Cr >= ((-2.2857 * Cb) + 432.85))
			return false;
		return true;
	}

	static boolean HSI(int r, int g, int b) {
		int mx = max(r, g, b);
		int mn = min(r, g, b);
		double d = mx - mn;
		double h = 0;
		if (mx == r)
			h = (g - b) / d;
		else if (mx == g)
			h = 2 + (b - r) / d;
		else
			h = 4 + (r - g) / d;
		h = h * 60;
		if (h < 0)
			h += 360;
		if (h > 4 && h < 45)
			return true;
		return false;
	}

	public static BufferedImage filterSkin(BufferedImage image) {
		BufferedImage result = new BufferedImage(image.getWidth(),
				image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);

		result.getGraphics().drawImage(image, 0, 0, null);
		WritableRaster raster = result.getRaster();
		int width = image.getWidth() - 1;
		int[] pixels = new int[width + 1];
		int x = 0, y = 0;

		try {
			for (y = 0; y < image.getHeight(); y++) {
				raster.getPixels(0, y, image.getWidth(), 1, pixels);
				for (x = 0; x < pixels.length; x++) {
					Color c = new Color(image.getRGB(x, y));
					int r = c.getRed();
					int g = c.getGreen();
					int b = c.getBlue();
					int a = c.getAlpha();
					float yy = 16f + (0.257f * (float) r)
							+ (0.504f * (float) g) + (0.098f * (float) b);
					float cb = 128f + (-0.148f * (float) r)
							+ (-0.291f * (float) g) + (0.439f * (float) b);
					float cr = 128f + (0.439f * (float) r)
							+ (-0.368f * (float) g) + (-0.071f * (float) b);

					// if (yy > 80 && 85 < cb && cb < 135 && 135 < cr && cr <
					// 180)
					if (isSkin(r, g, b) && YCbCr(r, g, b) && HSI(r, g, b))
						pixels[x] = 0;
					else
						pixels[x] = 255;
				}
				raster.setPixels(0, y, image.getWidth(), 1, pixels);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(x + " : " + y);
		}
		return result;

	}

	public static void main(String[] args) throws IOException {
		Webcam webcam = Webcam.getDefault();
		if (webcam != null) {
			System.out.println("Webcam: " + webcam.getName());
			System.out.println("opening....");
			webcam.open();
			System.out.println("opened.");

			// ImageIO.write(image, "PNG", new File("c:\\downloads\\test.png"));
			JFrame f = new JFrame("Panel Example");
			JPanel panel = new JPanel();
			panel.setBounds(40, 80, 200, 200);
			f.add(panel);
			f.setSize(400, 400);
			f.setLayout(null);
			f.setVisible(true);

			BufferedImage image = null;
			JLabel picLabel = null;

			for (int i = 0; i < 1000; i++) {
				image = webcam.getImage();
				//image = filterSkin(image);
				if (picLabel != null)
					panel.remove(picLabel);
				picLabel = new JLabel(new ImageIcon(image));
				panel.add(picLabel);

				panel.revalidate(); // invoke the layout manager
				panel.repaint(); // paint components
			}

			System.out.println("closing....");
			webcam.close();
			System.out.println("closed.");
		} else {
			System.out.println("No webcam detected");
		}
	}
}