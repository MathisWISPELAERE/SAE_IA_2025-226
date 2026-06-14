import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainDBSCAN {
    public static void main(String[] args) {
        try {
            String cheminImage = "./src/cartes/p1.jpg";
            System.out.println("Chargement de l'image : " + cheminImage);
            File fichierEntree = new File(cheminImage);
            BufferedImage imageOriginale = ImageIO.read(fichierEntree);

            int largeur = imageOriginale.getWidth();
            int hauteur = imageOriginale.getHeight();

            System.out.println("Application du flou par moyenne (t=5)...");
            BufferedImage imageFloutee = MeanBlur.apply(imageOriginale, 5);

            System.out.println("Sélection des pixels du biome de test (pixels à dominante bleue)...");
            List<Pixel> pixelsDuBiome = new ArrayList<>();
            for (int y = 0; y < hauteur; y++) {
                for (int x = 0; x < largeur; x++) {
                    Color couleur = new Color(imageFloutee.getRGB(x, y));
                    if (couleur.getBlue() > couleur.getRed() && couleur.getBlue() > couleur.getGreen()) {
                        pixelsDuBiome.add(new Pixel(x, y, couleur));
                    }
                }
            }
            System.out.println(pixelsDuBiome.size() + " pixels appartiennent à ce biome.");

            System.out.println("Lancement du clustering DBSCAN (écosystèmes, étape 3)...");
            AlgorithmeClustering dbscan = new DBSCAN(1.5, 4);
            List<Cluster> ecosystemes = dbscan.cluster(pixelsDuBiome);

            System.out.println("Nombre d'écosystèmes détectés : " + ecosystemes.size());

            System.out.println("Génération de l'image de rendu...");
            BufferedImage imageRendu = new BufferedImage(largeur, hauteur, BufferedImage.TYPE_INT_RGB);

            int pourcentageEclaircissement = 75;
            for (int y = 0; y < hauteur; y++) {
                for (int x = 0; x < largeur; x++) {
                    Color c = new Color(imageFloutee.getRGB(x, y));
                    int r = eclaircir(c.getRed(), pourcentageEclaircissement);
                    int g = eclaircir(c.getGreen(), pourcentageEclaircissement);
                    int b = eclaircir(c.getBlue(), pourcentageEclaircissement);
                    imageRendu.setRGB(x, y, new Color(r, g, b).getRGB());
                }
            }

            Color[] palette = {
                    Color.RED, Color.GREEN, Color.YELLOW, Color.MAGENTA,
                    Color.CYAN, Color.ORANGE, Color.PINK, new Color(128, 0, 128)
            };

            for (int i = 0; i < ecosystemes.size(); i++) {
                Color couleurEcosysteme = palette[i % palette.length];
                for (Pixel p : ecosystemes.get(i).getPixels()) {
                    imageRendu.setRGB(p.getX(), p.getY(), couleurEcosysteme.getRGB());
                }
            }

            File fichierSortie = new File("./src/cartes/resultat_ecosystemes1.jpg");
            ImageIO.write(imageRendu, "jpg", fichierSortie);

            System.out.println("Terminé ! Le résultat a été sauvegardé ici : " + fichierSortie.getAbsolutePath());

        } catch (Exception e) {
            System.err.println("Erreur lors de l'exécution du traitement :");
            e.printStackTrace();
        }
    }

    private static int eclaircir(int valeur, int pourcentage) {
        return Math.round(valeur + pourcentage / 100f * (255 - valeur));
    }
}