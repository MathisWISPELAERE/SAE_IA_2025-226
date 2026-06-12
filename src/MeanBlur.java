import java.awt.image.BufferedImage;

public class MeanBlur {

    public static double[][] createMatrice(int t) {
        double[][] matrice = new double[t][t];
        double value = 1.0 / (t * t);
        for (int i = 0; i < t; i++)
            for (int j = 0; j < t; j++)
                matrice[i][j] = value;
        return matrice;
    }

    public static BufferedImage apply(BufferedImage image, int t) {
        return Filtrer.apply(image, createMatrice(t));
    }
}