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
	public static BufferedImage thresholdImage(BufferedImage image,
			int threshold) {
		BufferedImage result = new BufferedImage(image.getWidth(),
				image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
		result.getGraphics().drawImage(image, 0, 0, null);
		WritableRaster raster = result.getRaster();
		int[] pixels = new int[image.getWidth()];
		for (int y = 0; y < image.getHeight(); y++) {
			raster.getPixels(0, y, image.getWidth(), 1, pixels);
			for (int i = 0; i < pixels.length; i++) {
				if (pixels[i] < threshold)
					pixels[i] = 0;
				else
					pixels[i] = 255;
			}
			raster.setPixels(0, y, image.getWidth(), 1, pixels);
		}
		return result;
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

					// System.out.println(r + "," + g + "," + b);
					int u = r - g;
					if (u > 20 && u < 74)
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

			for (int i = 0; i < 50; i++) {
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