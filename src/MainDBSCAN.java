import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

public class MainDBSCAN {

    public static void main(String[] args) throws Exception {
        String dossier = "src/cartes/";

        // -------------------------------------------------------
        // 1. CHARGEMENT DE L'IMAGE
        // -------------------------------------------------------
        File f = new File(dossier + "p1.jpg");
        BufferedImage original = ImageIO.read(f);
        System.out.println("Image chargée : " + original.getWidth() + "x" + original.getHeight());

        // -------------------------------------------------------
        // 2. FLOU GAUSSIEN
        // -------------------------------------------------------
        System.out.println("\n-- Flou gaussien --");
        BufferedImage imagePourClustering = FlouGaussien.apply(original, 11, 3);
        int width  = imagePourClustering.getWidth();
        int height = imagePourClustering.getHeight();
        System.out.println("Flou appliqué (noyau 11x11, sigma=3)");

        // -------------------------------------------------------
        // 3. EXTRACTION DES PIXELS
        // -------------------------------------------------------
        List<Pixel> listePixels = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = imagePourClustering.getRGB(x, y);
                listePixels.add(new Pixel(x, y, new Color(rgb)));
            }
        }

        // -------------------------------------------------------
        // 4. CLUSTERING K-MEANS
        // -------------------------------------------------------
        System.out.println("\n-- Clustering K-Means --");
        int nombreDeBiomes = 10;
        int maxIterations  = 100;
        KMeans kmeans = new KMeans(nombreDeBiomes, maxIterations);

        System.out.println("Calcul des clusters en cours...");
        List<Cluster> resultats = kmeans.cluster(listePixels);
        System.out.println("Clustering terminé. Nombre de clusters trouvés : " + resultats.size());
        for (int i = 0; i < resultats.size(); i++) {
            System.out.println("  KMeans cluster " + i + " -> " + resultats.get(i));
        }

        // Visualisation KMeans : chaque pixel prend la couleur moyenne de son cluster
        BufferedImage imageKMeans = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color c = new Color(imagePourClustering.getRGB(x, y));
                double minDst = Double.MAX_VALUE;
                Color bestColor = Color.BLACK;
                for (Cluster cluster : resultats) {
                    Color moy = cluster.getCouleurMoyenne();
                    double dst = Math.pow(c.getRed()   - moy.getRed(),   2)
                               + Math.pow(c.getGreen() - moy.getGreen(), 2)
                               + Math.pow(c.getBlue()  - moy.getBlue(),  2);
                    if (dst < minDst) {
                        minDst = dst;
                        bestColor = moy;
                    }
                }
                imageKMeans.setRGB(x, y, bestColor.getRGB());
            }
        }

        File outputKMeans = new File(dossier + "dbscan_resultat_kmeans_" + nombreDeBiomes + "_biomes.png");
        ImageIO.write(imageKMeans, "PNG", outputKMeans);
        System.out.println("Image KMeans sauvegardée : " + outputKMeans.getPath());

        // -------------------------------------------------------
        // 5. SÉLECTION DU PLUS PETIT CLUSTER K-MEANS
        // -------------------------------------------------------
        System.out.println("\n-- Sélection du plus petit cluster KMeans --");
        Cluster plusPetitCluster = resultats.get(0);
        for (Cluster cl : resultats) {
            if (cl.getNbPixels() < plusPetitCluster.getNbPixels()) {
                plusPetitCluster = cl;
            }
        }
        System.out.println("Cluster sélectionné : " + plusPetitCluster.getNbPixels() + " pixels");

        // -------------------------------------------------------
        // 5b. APERÇU : mise en évidence du cluster sélectionné avant DBSCAN
        // -------------------------------------------------------
        BufferedImage imageAvantDBSCAN = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);

        java.util.Set<Long> pixelsCluster = new java.util.HashSet<>();
        for (Pixel p : plusPetitCluster.getPixels()) {
            pixelsCluster.add((long) p.getY() * width + p.getX());
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color c = new Color(imagePourClustering.getRGB(x, y));
                int gris = (int) (0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue());
                if (pixelsCluster.contains((long) y * width + x)) {
                    imageAvantDBSCAN.setRGB(x, y, Color.WHITE.getRGB());
                } else {
                    int grisSombre = (int) (gris * 0.4);
                    imageAvantDBSCAN.setRGB(x, y, new Color(grisSombre, grisSombre, grisSombre).getRGB());
                }
            }
        }

        File outputAvantDBSCAN = new File(dossier + "dbscan_cluster_selectionne.png");
        ImageIO.write(imageAvantDBSCAN, "PNG", outputAvantDBSCAN);
        System.out.println("Aperçu du cluster sélectionné sauvegardé : " + outputAvantDBSCAN.getPath());

        // -------------------------------------------------------
        // 6. DBSCAN SUR LE PLUS PETIT CLUSTER
        // -------------------------------------------------------
        System.out.println("\n-- DBSCAN sur le plus petit cluster KMeans --");

        // eps  : distance euclidienne maximale entre deux pixels voisins
        //        (dans l'espace [x, y, R, G, B] selon getPositionCommeTableau)
        //        À ajuster selon la nature des données de Pixel
        // minPts : nombre minimum de voisins pour qu'un point soit un core point
        double eps    = 2.0;
        int    minPts = 5;

        DBSCAN dbscan = new DBSCAN(eps, minPts);

        System.out.println("Application de DBSCAN (eps=" + eps + ", minPts=" + minPts + ")...");
        List<Cluster> sousClusters = dbscan.cluster(plusPetitCluster.getPixels());
        System.out.println("DBSCAN terminé. Nombre de sous-clusters : " + sousClusters.size());
        for (int i = 0; i < sousClusters.size(); i++) {
            System.out.println("  DBSCAN sous-cluster " + i + " -> " + sousClusters.get(i));
        }

        // -------------------------------------------------------
        // 7. IMAGE RÉSULTAT : FOND N&B + SOUS-CLUSTERS DBSCAN EN COULEURS VIVES
        // -------------------------------------------------------
        BufferedImage imageDBSCAN = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);

        // Étape A : fond en niveaux de gris
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color c = new Color(imagePourClustering.getRGB(x, y));
                int gris = (int) (0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue());
                imageDBSCAN.setRGB(x, y, new Color(gris, gris, gris).getRGB());
            }
        }

        // Étape B : palette de couleurs vives (golden ratio)
        int nbSousClusters = sousClusters.size();
        Color[] palette = new Color[nbSousClusters];
        final float GOLDEN_RATIO = 0.618033988749895f;
        float teinte = 0.0f;
        for (int i = 0; i < nbSousClusters; i++) {
            palette[i] = Color.getHSBColor(teinte, 0.9f, 0.95f);
            teinte = (teinte + GOLDEN_RATIO) % 1.0f;
        }

        // Étape C : colorier chaque pixel de chaque sous-cluster
        for (int i = 0; i < nbSousClusters; i++) {
            for (Pixel p : sousClusters.get(i).getPixels()) {
                imageDBSCAN.setRGB(p.getX(), p.getY(), palette[i].getRGB());
            }
        }

        File outputDBSCAN = new File(dossier + "dbscan_resultat_couleurs.png");
        ImageIO.write(imageDBSCAN, "PNG", outputDBSCAN);
        System.out.println("Image DBSCAN (couleurs vives) sauvegardée : " + outputDBSCAN.getPath());
    }
}