import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import javax.imageio.ImageIO;
import javax.swing.*;

public class ANPR {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("ANPR System");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(400, 200);

            JPanel panel = new JPanel();
            JButton uploadButton = new JButton("Upload Image");
            panel.add(uploadButton);
            frame.add(panel);

            uploadButton.addActionListener(e -> {
                JFileChooser fileChooser = new JFileChooser();
                int returnValue = fileChooser.showOpenDialog(null);
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    processImage(selectedFile.getAbsolutePath());
                }
            });

            frame.setVisible(true);
        });
    }

    public static void processImage(String imgPath) {
        try {
            // Load the input image
            BufferedImage img = ImageIO.read(new File(imgPath));
            displayImage(img, "Original Image");

            // Convert to grayscale
            RGBToGrayscale(img);
            displayImage(img, "Grayscale Image");

            // Apply median filter
            BufferedImage imgFiltered = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
            applyMedianFilter(img, imgFiltered);
            displayImage(imgFiltered, "Median Filter Image");

            // Apply Sobel vertical edge detection
            BufferedImage edgeVertical = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
            applySobel(imgFiltered, edgeVertical, "Vertical");
            displayImage(edgeVertical, "Vertical Edge Image");

            // Compute vertical projection
            int[] magnitude = new int[img.getHeight()];
            int[] magnitudeSmoothed = new int[img.getHeight()];
            computeVerticalProjection(edgeVertical, magnitude, magnitudeSmoothed);

            // Compute band candidates
            int candidateNum = 3;
            int[][] bands = new int[candidateNum][2];
            computeBandCandidates(magnitudeSmoothed, bands, candidateNum);

            // Crop and display candidate bands
            ArrayList<BufferedImage> bandImages = new ArrayList<>();
            for (int i = 0; i < candidateNum; i++) {
                if (bands[i][0] == 0 && bands[i][1] == 0) break;
                int startRow = bands[i][0];
                int endRow = Math.min(bands[i][1] + 15, img.getHeight());
                BufferedImage candidate = img.getSubimage(0, startRow, img.getWidth(), endRow - startRow);
                bandImages.add(candidate);
                displayImage(candidate, "Candidate Band " + (i + 1));
            }

            // Process and display Sobel-filtered candidate bands
            for (int i = 0; i < bandImages.size(); i++) {
                BufferedImage band = bandImages.get(i);
                BufferedImage bandSobel = new BufferedImage(band.getWidth(), band.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
                applySobel(band, bandSobel, "Sobel");
                displayImage(bandSobel, "Candidate Band Sobel " + (i + 1));
            }

            // Display final selected band
            if (!bandImages.isEmpty()) {
                displayImage(bandImages.get(0), "Final Output Band");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void RGBToGrayscale(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int p = img.getRGB(x, y);
                int a = (p >> 24) & 0xff;
                int r = (p >> 16) & 0xff;
                int g = (p >> 8) & 0xff;
                int b = p & 0xff;

                int avg = (r + g + b) / 3;
                p = (a << 24) | (avg << 16) | (avg << 8) | avg;
                img.setRGB(x, y, p);
            }
        }
    }

    public static void applyMedianFilter(BufferedImage sourceImg, BufferedImage targetImg) {
        int width = sourceImg.getWidth();
        int height = sourceImg.getHeight();
        int[][] kernel = new int[3][3];
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int index = 0;
                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        kernel[index / 3][index % 3] = sourceImg.getRGB(x + kx, y + ky) & 0xff;
                        index++;
                    }
                }
                int[] values = Arrays.stream(kernel).flatMapToInt(Arrays::stream).toArray();
                Arrays.sort(values);
                int median = values[4];
                targetImg.setRGB(x, y, (median << 16) | (median << 8) | median);
            }
        }
    }

    public static void applySobel(BufferedImage sourceImg, BufferedImage targetImg, String type) {
        int width = sourceImg.getWidth();
        int height = sourceImg.getHeight();
        int[][] sobelKernel = type.equalsIgnoreCase("Vertical")
                ? new int[][] { { 1, 0, -1 }, { 2, 0, -2 }, { 1, 0, -1 } }
                : new int[][] { { 1, 2, 1 }, { 0, 0, 0 }, { -1, -2, -1 } };

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int sum = 0;
                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        int pixel = sourceImg.getRGB(x + kx, y + ky) & 0xff;
                        sum += pixel * sobelKernel[ky + 1][kx + 1];
                    }
                }
                sum = Math.min(Math.max(sum, 0), 255);
                targetImg.setRGB(x, y, (sum << 16) | (sum << 8) | sum);
            }
        }
    }

    public static void computeVerticalProjection(BufferedImage img, int[] magnitude, int[] magnitudeSmoothed) {
        int width = img.getWidth();
        int height = img.getHeight();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int p = img.getRGB(x, y);
                int r = (p >> 16) & 0xff;
                magnitude[y] += r;
            }
        }
        for (int i = 4; i < magnitude.length - 4; i++) {
            magnitudeSmoothed[i] = (magnitude[i - 4] + magnitude[i - 3] + magnitude[i - 2] + magnitude[i - 1] +
                                     magnitude[i] + magnitude[i + 1] + magnitude[i + 2] + magnitude[i + 3] +
                                     magnitude[i + 4]) / 9;
        }
    }

    public static void computeBandCandidates(int[] magnitudeSmoothed, int[][] bands, int candidateNum) {
        int[] zeroized = Arrays.copyOf(magnitudeSmoothed, magnitudeSmoothed.length);
        for (int i = 0; i < candidateNum; i++) {
            int max = 0, index = -1;
            for (int j = 0; j < zeroized.length; j++) {
                if (zeroized[j] > max) {
                    max = zeroized[j];
                    index = j;
                }
            }
            if (index == -1) break;
            int y0 = index, y1 = index;
            while (y0 > 0 && zeroized[y0] > max * 0.55) y0--;
            while (y1 < zeroized.length - 1 && zeroized[y1] > max * 0.55) y1++;
            bands[i][0] = y0;
            bands[i][1] = y1;
            Arrays.fill(zeroized, y0, y1 + 1, 0);
        }
    }

    public static void displayImage(BufferedImage img, String title) {
        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        ImageIcon icon = new ImageIcon(img);
        JLabel label = new JLabel(icon);
        frame.add(label);
        frame.pack();
        frame.setVisible(true);
    }
}
