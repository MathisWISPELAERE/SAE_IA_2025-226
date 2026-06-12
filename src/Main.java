import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class Main {

    public static void main(String[] args) throws Exception {
        String dossier = "src/cartes/";
        // -------------------------------------------------------
        // 1. CHARGEMENT DE L'IMAGE
        // -------------------------------------------------------
        File f = new File(dossier+"p1.jpg");
        BufferedImage original = ImageIO.read(f);
        System.out.println("Image chargée : " + original.getWidth() + "x" + original.getHeight());

        // -------------------------------------------------------
        // 2. FLOU PAR MOYENNE
        // -------------------------------------------------------
        System.out.println("\n-- Flou par moyenne --");

        for (int size : new int[]{3}) {
            BufferedImage result = MeanBlur.apply(original, size);
            String path = dossier + "mean_blur_" + size + "x" + size + ".png";
            ImageIO.write(result, "PNG", new File(path));
            System.out.println("  Noyau " + size + "x" + size + " -> " + path);
        }

        // -------------------------------------------------------
        // 3. FLOU GAUSSIEN
        // -------------------------------------------------------
        System.out.println("\n-- Flou gaussien --");

        // Paires (taille, sigma) testées
        int[][] configs = {{11, 2},{11, 3}};
        for (int[] cfg : configs) {
            int size  = cfg[0];
            double sigma = cfg[1];
            BufferedImage result = FlouGaussien.apply(original, size, sigma);
            String path = dossier + "gaussian_blur_" + size + "x" + size + "_sigma" + (int)sigma + ".png";
            ImageIO.write(result, "PNG", new File(path));
            System.out.println("  Noyau " + size + "x" + size + " sigma=" + sigma + " -> " + path);
        }
        System.out.println("\nTerminé.");
    }

}