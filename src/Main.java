import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

public class Main {

    public static void main(String[] args) throws Exception {
        String dossier = "src/cartes/";

        // -------------------------------------------------------
        // 1. CHARGEMENT DE L'IMAGE
        // -------------------------------------------------------
        File f = new File(dossier + "p1.jpg");
        BufferedImage original = ImageIO.read(f);
        System.out.println("Image chargée : " + original.getWidth() + "x" + original.getHeight());

        // -------------------------------------------------------
        // 2. FLOU PAR MOYENNE
        // -------------------------------------------------------
        // System.out.println("\n-- Flou par moyenne --");
        // for (int size : new int[]{3}) {
        //     BufferedImage result = MeanBlur.apply(original, size);
        //     String path = dossier + "mean_blur_" + size + "x" + size + ".png";
        //     ImageIO.write(result, "PNG", new File(path));
        //     System.out.println("  Noyau " + size + "x" + size + " -> " + path);
        // }

        // -------------------------------------------------------
        // 3. FLOU GAUSSIEN
        // -------------------------------------------------------
        // System.out.println("\n-- Flou gaussien --");
        // int[][] configs = {{11, 2}, {11, 3}};
        // for (int[] cfg : configs) {
        //     int size = cfg[0];
        //     double sigma = cfg[1];
        //     BufferedImage result = FlouGaussien.apply(original, size, sigma);
        //     String path = dossier + "gaussian_blur_" + size + "x" + size + "_sigma" + (int) sigma + ".png";
        //     ImageIO.write(result, "PNG", new File(path));
        //     System.out.println("  Noyau " + size + "x" + size + " sigma=" + sigma + " -> " + path);
        // }

        // -------------------------------------------------------
        // 4. CLUSTERING K-MEANS
        // -------------------------------------------------------
        System.out.println("\n-- Test du Clustering K-Means --");

        BufferedImage imagePourClustering = FlouGaussien.apply(original, 11, 3);
        int width  = imagePourClustering.getWidth();
        int height = imagePourClustering.getHeight();

        List<Pixel> listePixels = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = imagePourClustering.getRGB(x, y);
                listePixels.add(new Pixel(x, y, new Color(rgb)));
            }
        }

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
        BufferedImage imageClusters = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int color = imagePourClustering.getRGB(x, y);
                Color c = new Color(color);

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
                imageClusters.setRGB(x, y, bestColor.getRGB());
            }
        }

        File outputCluster = new File(dossier + "resultat_clustering_" + nombreDeBiomes + "_biomes.png");
        ImageIO.write(imageClusters, "PNG", outputCluster);
        System.out.println("Image des clusters sauvegardée : " + outputCluster.getPath());

        // -------------------------------------------------------
        // 5. HAC SUR LE PLUS GRAND CLUSTER K-MEANS
        // -------------------------------------------------------
        System.out.println("\n-- Clustering HAC sur le plus grand cluster KMeans --");

        // Sélection du cluster le plus grand (le plus peuplé en pixels)
        Cluster plusPetitCLuster = resultats.get(0);
        for (Cluster cl : resultats) {
            if (cl.getNbPixels() < plusPetitCLuster.getNbPixels()) {
                plusPetitCLuster = cl;
            }
        }
        System.out.println("Cluster sélectionné : " + plusPetitCLuster.getNbPixels() + " pixels");

        // -------------------------------------------------------
        // 5b. APERÇU N&B : mise en évidence du cluster sélectionné avant HAC
        // -------------------------------------------------------
        // Les pixels du cluster sélectionné apparaissent en blanc (255),
        // le reste de l'image est converti en niveaux de gris sombres (assombris à 40 %)
        // afin de faire ressortir clairement la zone qui va être traitée par HAC.
        BufferedImage imageAvantHAC = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);

        // Construire un ensemble des positions du cluster pour un lookup O(1)
        java.util.Set<Long> pixelsCluster = new java.util.HashSet<>();
        for (Pixel p : plusPetitCLuster.getPixels()) {
            pixelsCluster.add((long) p.getY() * width + p.getX());
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color c = new Color(imagePourClustering.getRGB(x, y));
                int gris = (int) (0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue());
                if (pixelsCluster.contains((long) y * width + x)) {
                    // Pixel appartenant au cluster : blanc pur pour le faire ressortir
                    imageAvantHAC.setRGB(x, y, Color.WHITE.getRGB());
                } else {
                    // Reste de l'image : gris assombri (40 % de la luminance)
                    int grisSombre = (int) (gris * 0.4);
                    imageAvantHAC.setRGB(x, y, new Color(grisSombre, grisSombre, grisSombre).getRGB());
                }
            }
        }

        File outputAvantHAC = new File(dossier + "cluster_selectionne_avant_hac.png");
        ImageIO.write(imageAvantHAC, "PNG", outputAvantHAC);
        System.out.println("Aperçu du cluster sélectionné sauvegardé : " + outputAvantHAC.getPath());

        // Seuil de distance spatiale : ajuster selon la résolution de l'image
        // (ex: 20 pixels = les pixels doivent être proches géographiquement pour fusionner)
        double seuilDistanceSpatiale = 10.0;
        HACEcosystemes hac = new HACEcosystemes(seuilDistanceSpatiale);

        System.out.println("Application de HAC (seuil=" + seuilDistanceSpatiale + " px)...");
        List<Cluster> sousClusters = hac.cluster(plusPetitCLuster.getPixels());
        System.out.println("HAC terminé. Nombre de sous-clusters : " + sousClusters.size());
        for (int i = 0; i < sousClusters.size(); i++) {
            System.out.println("  HAC sous-cluster " + i + " -> " + sousClusters.get(i));
        }

        // -------------------------------------------------------
        // 6. IMAGE RÉSULTAT : FOND N&B + CLUSTERS HAC EN COULEURS VIVES
        // -------------------------------------------------------
        // - Les pixels hors du cluster KMeans sélectionné restent en niveaux de gris
        // - Chaque sous-cluster HAC reçoit une couleur vive distincte
        //   générée par répartition uniforme de la teinte (HSB : saturation=1, luminosité=1)
        BufferedImage imageHAC = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);

        // Étape A : remplir toute l'image en niveaux de gris (arrière-plan)
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color c = new Color(imagePourClustering.getRGB(x, y));
                int gris = (int) (0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue());
                imageHAC.setRGB(x, y, new Color(gris, gris, gris).getRGB());
            }
        }

        // Étape B : générer une palette de couleurs vives pour chaque sous-cluster HAC.
        // On utilise la méthode du nombre d'or (golden ratio ~0.618) :
        // en ajoutant 0.618 à chaque teinte et en prenant modulo 1, on obtient
        // une suite qui maximise l'écart entre chaque couleur consécutive,
        // garantissant que deux clusters voisins dans la liste n'ont jamais
        // une teinte proche — même avec beaucoup de clusters.
        int nbSousClusters = sousClusters.size();
        Color[] palette = new Color[nbSousClusters];
        final float GOLDEN_RATIO = 0.618033988749895f;
        float teinte = 0.0f;
        for (int i = 0; i < nbSousClusters; i++) {
            palette[i] = Color.getHSBColor(teinte, 0.9f, 0.95f);
            teinte = (teinte + GOLDEN_RATIO) % 1.0f;
        }

        // Étape C : colorier chaque pixel de chaque sous-cluster avec sa couleur vive
        for (int i = 0; i < nbSousClusters; i++) {
            for (Pixel p : sousClusters.get(i).getPixels()) {
                imageHAC.setRGB(p.getX(), p.getY(), palette[i].getRGB());
            }
        }

        File outputHAC = new File(dossier + "resultat_hac_couleurs.png");
        ImageIO.write(imageHAC, "PNG", outputHAC);
        System.out.println("Image HAC (couleurs vives) sauvegardée : " + outputHAC.getPath());
    }
}