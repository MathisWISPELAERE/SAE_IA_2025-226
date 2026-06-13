import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class KMeans implements AlgorithmeClustering {

    private int k;
    private int maxIterations;

    public KMeans(int k, int maxIterations) {
        this.k = k;
        this.maxIterations = maxIterations;
    }

    // FIX 1 : signature alignée sur l'interface AlgorithmeClustering
    @Override
    public List<Cluster> cluster(List<Pixel> pixels) {
        List<Cluster> clusters = new ArrayList<>();
        if (pixels == null || pixels.isEmpty()) return clusters;

        int nbObjets = pixels.size();

        // Conversion List<Pixel> → double[][] [R, G, B]
        double[][] donnees = new double[nbObjets][3];
        for (int i = 0; i < nbObjets; i++) {
            Color c = pixels.get(i).getCouleur();
            donnees[i][0] = c.getRed();
            donnees[i][1] = c.getGreen();
            donnees[i][2] = c.getBlue();
        }

        int nbCaracteristiques = donnees[0].length;

        // 1. Initialisation aléatoire des centroïdes
        double[][] centroids = new double[k][nbCaracteristiques];
        Random random = new Random();
        for (int i = 0; i < k; i++) {
            centroids[i] = donnees[random.nextInt(nbObjets)].clone();
        }

        int[] assignments = new int[nbObjets];
        boolean changed = true;
        int iterations = 0;

        // 2. Boucle principale
        while (changed && iterations < maxIterations) {
            changed = false;
            iterations++;

            // Étape A : Assigner chaque pixel au centroïde le plus proche
            for (int i = 0; i < nbObjets; i++) {
                int bestCluster = 0;
                double minDistance = Double.MAX_VALUE;

                for (int j = 0; j < k; j++) {
                    double dist = distanceEuclidienne(donnees[i], centroids[j]);
                    if (dist < minDistance) {
                        minDistance = dist;
                        bestCluster = j;
                    }
                }

                if (assignments[i] != bestCluster) {
                    assignments[i] = bestCluster;
                    changed = true;
                }
            }

            // Étape B : Recalculer les centroïdes
            double[][] newCentroids = new double[k][nbCaracteristiques];
            int[] counts = new int[k];

            for (int i = 0; i < nbObjets; i++) {
                int c = assignments[i];
                for (int dim = 0; dim < nbCaracteristiques; dim++) {
                    newCentroids[c][dim] += donnees[i][dim];
                }
                counts[c]++;
            }

            for (int j = 0; j < k; j++) {
                if (counts[j] > 0) {
                    for (int dim = 0; dim < nbCaracteristiques; dim++) {
                        // FIX 2 : diviser newCentroids (pas centroids) puis l'affecter
                        newCentroids[j][dim] /= counts[j];
                    }
                    centroids[j] = newCentroids[j];
                } else {
                    // Cluster vide : réinitialisation aléatoire
                    centroids[j] = donnees[random.nextInt(nbObjets)].clone();
                    changed = true;
                }
            }
        }

        // 3. Création des objets Cluster avec leurs centroïdes finaux
        for (int i = 0; i < k; i++) {
            Cluster c = new Cluster();
            c.setcouleurClusteur(centroids[i]);
            clusters.add(c);
        }

        // 4. Rattachement des pixels originaux à leur cluster
        // (on réutilise les objets Pixel existants, pas besoin d'en recréer)
        for (int i = 0; i < nbObjets; i++) {
            clusters.get(assignments[i]).ajouterPixel(pixels.get(i));
        }

        return clusters;
    }

    /**
     * Distance euclidienne au carré (suffisant pour comparer des distances).
     */
    private double distanceEuclidienne(double[] p1, double[] p2) {
        double sum = 0;
        for (int i = 0; i < p1.length; i++) {
            sum += (p1[i] - p2[i]) * (p1[i] - p2[i]);
        }
        return sum;
    }

    private int limiteRGB(int valeur) {
        return Math.max(0, Math.min(255, valeur));
    }
}