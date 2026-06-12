import java.awt.image.BufferedImage;

public class FlouGaussien {

    public static double[][] createMatrice(int t, double sigma) {
        double[][] matrice = new double[t][t];
        int center = t / 2;
        double sum = 0;

        for (int i = 0; i < t; i++) {
            for (int j = 0; j < t; j++) {
                int x = j - center;
                int y = i - center;
                matrice[i][j] = Math.exp(-(x * x + y * y) / (2.0 * sigma * sigma));
                sum += matrice[i][j];
            }
        }
        for (int i = 0; i < t; i++)
            for (int j = 0; j < t; j++)
                matrice[i][j] /= sum;

        return matrice;
    }

    public static BufferedImage apply(BufferedImage image, int t, double sigma) {
        return Filtrer.apply(image, createMatrice(t, sigma));
    }
}