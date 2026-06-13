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

        System.out.println("\n-- Test du Clustering K-Means --");

        // 1. Prendre l'image floutée comme base (ex: le résultat du flou gaussien)
        BufferedImage imagePourClustering = FlouGaussien.apply(original, 11, 3);
        int width = imagePourClustering.getWidth();
        int height = imagePourClustering.getHeight();

        // 2. Transformer l'image en tableau double[][] pour l'algorithme
        // La taille est : nombre d'objets (pixels) x nombre de caractéristiques (R, G, B)
        List<Pixel> listePixels = new ArrayList<>(); 

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = imagePourClustering.getRGB(x, y);
                Color couleur = new Color(rgb);
                
                // On crée un nouvel objet Pixel contenant sa position et sa couleur
                listePixels.add(new Pixel(x, y, couleur));
            }
        }

        // 3. Lancer l'algorithme KMeans
        int nombreDeBiomes = 2; // Vous pouvez ajuster ce nombre (k)
        int maxIterations = 100;
        KMeans kmeans = new KMeans(nombreDeBiomes, maxIterations);

        System.out.println("Calcul des clusters en cours...");
        // On passe bien notre liste de Pixels à la méthode cluster()
        List<Cluster> resultats = kmeans.cluster(listePixels); 
        System.out.println("Clustering terminé. Nombre de clusters trouvés : " + resultats.size());
        // 4. Visualiser les clusters : on remplace chaque pixel par la couleur moyenne de son cluster
        // Comme demandé dans le sujet : "remplacer chaque pixel de la carte par la couleur moyenne de son cluster."
        BufferedImage imageClusters = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);

        // Reconstruire l'image à partir des clusters
        // *Version plus simple pour l'affichage* : relancer l'assignation sur l'image
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int color = imagePourClustering.getRGB(x, y);
                Color c = new Color(color);
                
                // Trouver le cluster le plus proche pour ce pixel
                double minDst = Double.MAX_VALUE;
                Color bestColor = Color.BLACK;
                
                for(Cluster cluster : resultats) {
                    Color moy = cluster.getCouleurMoyenne();
                    // Distance euclidienne basique
                    double dst = Math.pow(c.getRed()-moy.getRed(), 2) + Math.pow(c.getGreen()-moy.getGreen(), 2) + Math.pow(c.getBlue()-moy.getBlue(), 2);
                    if(dst < minDst) {
                        minDst = dst;
                        bestColor = moy;
                    }
                }
                imageClusters.setRGB(x, y, bestColor.getRGB());
            }
        }

        // Sauvegarder le résultat
        File outputCluster = new File(dossier + "resultat_clustering_" + nombreDeBiomes + "_biomes.png");
        ImageIO.write(imageClusters, "PNG", outputCluster);
        System.out.println("Image des clusters sauvegardée : " + outputCluster.getPath());

            }

}