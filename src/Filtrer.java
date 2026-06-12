import java.awt.*;
import java.awt.image.BufferedImage;

public class Filtrer {

    /**
     * Applique un noyau de convolution sur les 3 canaux R, G, B .
     * Les bords sont gérés par clamp (pixel le plus proche).
     */
    public static BufferedImage apply(BufferedImage image, double[][] matrice) {
        int width   = image.getWidth();
        int height  = image.getHeight();
        int tailleMatrice   = matrice.length;
        int centreMatrice = tailleMatrice / 2;

        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                double sumR = 0, sumG = 0, sumB = 0;

                for (int ki = 0; ki < tailleMatrice; ki++) {
                    for (int kj = 0; kj < tailleMatrice; kj++) {
                        // Clamp aux bords
                        int nx = Math.min(Math.max(x + kj - centreMatrice, 0), width  - 1);
                        int ny = Math.min(Math.max(y + ki - centreMatrice, 0), height - 1);

                        int color = image.getRGB(nx, ny);
                        int r = (color & 0xff0000) >> 16;
                        int g = (color & 0x00ff00) >> 8;
                        int b =  color & 0x0000ff;

                        sumR += matrice[ki][kj] * r;
                        sumG += matrice[ki][kj] * g;
                        sumB += matrice[ki][kj] * b;
                    }
                }

                int r = (int) Math.min(Math.max(Math.round(sumR), 0), 255);
                int g = (int) Math.min(Math.max(Math.round(sumG), 0), 255);
                int b = (int) Math.min(Math.max(Math.round(sumB), 0), 255);

                result.setRGB(x, y, new Color(r, g, b).getRGB());
            }
        }
        return result;
    }
}