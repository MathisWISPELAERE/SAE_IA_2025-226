import java.util.*;

/**
 * HAC avec Simple Linkage (lien minimum) :
 *   dist(A∪B, C) = min( dist(A,C), dist(B,C) )
 *
 * Optimisations conservées :
 *  1. Pré-groupement en grille (TAILLE_CELLULE × TAILLE_CELLULE px)
 *     → réduit drastiquement le nombre de clusters initiaux
 *  2. Mise à jour de la matrice en O(n) après chaque fusion
 *     → on prend simplement le minimum des deux lignes fusionnées,
 *        sans recalculer de centroïde ni de distance pixel à pixel
 */
public class HACEcosystemes implements AlgorithmeClustering {

    private final double seuilDistance;

    // Taille d'une cellule de pré-groupement (en pixels).
    // Augmenter pour accélérer, diminuer pour plus de précision.
    private static final int TAILLE_CELLULE = 5;

    public HACEcosystemes(double seuilDistance) {
        this.seuilDistance = seuilDistance;
    }

    @Override
    public List<Cluster> cluster(List<Pixel> pixels) {
        if (pixels == null || pixels.isEmpty()) return new ArrayList<>();

        // Étape 1 : pré-groupement en cellules de grille
        Map<String, List<Pixel>> grille = new HashMap<>();
        for (Pixel p : pixels) {
            String cle = (p.getX() / TAILLE_CELLULE) + "_" + (p.getY() / TAILLE_CELLULE);
            grille.computeIfAbsent(cle, k -> new ArrayList<>()).add(p);
        }

        // Un Cluster par cellule non vide
        List<Cluster> clusters = new ArrayList<>();
        for (List<Pixel> groupe : grille.values()) {
            clusters.add(new Cluster(groupe));
        }

        System.out.println("  HAC (simple linkage) : " + pixels.size() + " pixels → "
                + clusters.size() + " cellules initiales");

        // Étape 2 : précalcul de la matrice des distances (distance spatiale entre centroïdes)
        // Pour le simple linkage, on initialise avec la distance minimale entre les deux cellules.
        // Comme chaque cellule est un carré de TAILLE_CELLULE px, la distance spatiale
        // entre centroïdes de cellules adjacentes est une bonne approximation du min-pixel.
        int n = clusters.size();
        double[][] distances = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                double dist = clusters.get(i).distanceSpatiale(clusters.get(j));
                distances[i][j] = dist;
                distances[j][i] = dist;
            }
        }

        // Étape 3 : boucle HAC — simple linkage
        // On travaille sur des indices actifs pour éviter de décaler le tableau.
        List<Integer> actifs = new ArrayList<>();
        for (int i = 0; i < n; i++) actifs.add(i);

        while (actifs.size() > 1) {

            // Trouver la paire (idxA, idxB) de distance minimale parmi les actifs
            int idxA = -1, idxB = -1;
            double distMin = Double.MAX_VALUE;

            for (int i = 0; i < actifs.size(); i++) {
                for (int j = i + 1; j < actifs.size(); j++) {
                    double dist = distances[actifs.get(i)][actifs.get(j)];
                    if (dist < distMin) {
                        distMin = dist;
                        idxA = actifs.get(i);
                        idxB = actifs.get(j);
                    }
                }
            }

            // Arrêt si la distance minimale dépasse le seuil
            if (distMin > seuilDistance) break;

            // Fusion : A absorbe B
            clusters.get(idxA).fusionner(clusters.get(idxB));
            actifs.remove(Integer.valueOf(idxB));

            // Mise à jour simple linkage : dist(A∪B, C) = min(dist(A,C), dist(B,C))
            // On met à jour la ligne/colonne de A dans la matrice.
            for (int idx : actifs) {
                if (idx == idxA) continue;
                double nouvelleDist = Math.min(distances[idxA][idx], distances[idxB][idx]);
                distances[idxA][idx] = nouvelleDist;
                distances[idx][idxA] = nouvelleDist;
            }
        }

        // Étape 4 : retourner uniquement les clusters encore actifs
        List<Cluster> resultat = new ArrayList<>();
        for (int idx : actifs) resultat.add(clusters.get(idx));
        return resultat;
    }
}