import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class Main {

    public static void main(String[] args) throws Exception {

        // -------------------------------------------------------
        // 1. CHARGEMENT DE L'IMAGE
        // -------------------------------------------------------
        File f = new File("src/p1.png");
        System.out.println("Chemin absolu cherché : " + f.getAbsolutePath());
        System.out.println("Fichier existe : " + f.exists());
        BufferedImage original = ImageIO.read(f);
        System.out.println("Image chargée : " + original.getWidth() + "x" + original.getHeight());

        // -------------------------------------------------------
        // 2. FLOU PAR MOYENNE
        // -------------------------------------------------------
        System.out.println("\n-- Flou par moyenne --");

        for (int size : new int[]{3, 5, 7}) {
            BufferedImage result = MeanBlur.apply(original, size);
            String path = "mean_blur_" + size + "x" + size + ".png";
            ImageIO.write(result, "PNG", new File(path));
            System.out.println("  Noyau " + size + "x" + size + " -> " + path);
        }

        // -------------------------------------------------------
        // 3. FLOU GAUSSIEN
        // -------------------------------------------------------
        System.out.println("\n-- Flou gaussien --");

        // Paires (taille, sigma) testées
        int[][] configs = {{3, 1}, {5, 1}, {5, 2}, {7, 1}, {7, 2}};
        for (int[] cfg : configs) {
            int size  = cfg[0];
            double sigma = cfg[1];
            BufferedImage result = FlouGaussien.apply(original, size, sigma);
            String path = "gaussian_blur_" + size + "x" + size
                          + "_sigma" + (int)sigma + ".png";
            ImageIO.write(result, "PNG", new File(path));
            System.out.println("  Noyau " + size + "x" + size
                               + " sigma=" + sigma + " -> " + path);
        }
        System.out.println("\nTerminé.");
    }

}