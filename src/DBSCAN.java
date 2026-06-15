import java.util.ArrayList;
import java.util.List;

public class DBSCAN implements AlgorithmeClustering {

    private final double eps;
    private final int minPts;

    private static final int UNVISITED = -1;
    private static final int NOISE = -2;

    public DBSCAN(double eps, int minPts) {
        this.eps = eps;
        this.minPts = minPts;
    }

    @Override
    public List<Cluster> cluster(List<Pixel> pixels) {
        int nbObjets = pixels.size();
        List<Cluster> listeDeClusters = new ArrayList<>();

        if (nbObjets == 0) return listeDeClusters;

        System.out.println("[DBSCAN] Analyse de " + nbObjets + " pixels...");

        double[][] donnees = new double[nbObjets][];
        for (int i = 0; i < nbObjets; i++) {
            donnees[i] = pixels.get(i).getPositionCommeTableau();
        }

        int[] labels = new int[nbObjets];
        for (int i = 0; i < nbObjets; i++) {
            labels[i] = UNVISITED;
        }

        int currentClusterId = 0;

        for (int i = 0; i < nbObjets; i++) {
            if (i % 1000 == 0 && i > 0) {
                int progression = (int) ((double) i / nbObjets * 100);
                System.out.print("\r[DBSCAN] Progression : " + progression + "% (" + i + "/" + nbObjets + " pixels)");
            }

            if (labels[i] != UNVISITED) {
                continue;
            }

            List<Integer> voisins = getVoisins(i, donnees);

            if (voisins.size() < minPts) {
                labels[i] = NOISE;
            } else {
                labels[i] = currentClusterId;
                etendreCluster(voisins, currentClusterId, labels, donnees);
                currentClusterId++;
            }
        }
        System.out.println("\r[DBSCAN] Progression : 100% | Calcul terminé !");

        for (int c = 0; c < currentClusterId; c++) {
            listeDeClusters.add(new Cluster());
        }

        for (int i = 0; i < nbObjets; i++) {
            int clusterId = labels[i];
            if (clusterId >= 0) {
                listeDeClusters.get(clusterId).ajouterPixel(pixels.get(i));
            }
        }

        for (Cluster c : listeDeClusters) {
            c.finaliser();
        }

        System.out.println("[DBSCAN] Total : " + listeDeClusters.size() + " clusters créés.");
        return listeDeClusters;
    }

    private void etendreCluster(List<Integer> voisins, int clusterId, int[] labels, double[][] donnees) {
        for (int i = 0; i < voisins.size(); i++) {
            int voisinId = voisins.get(i);

            if (labels[voisinId] == UNVISITED) {
                labels[voisinId] = clusterId;

                List<Integer> voisinsDuVoisin = getVoisins(voisinId, donnees);
                if (voisinsDuVoisin.size() >= minPts) {
                    for (int v : voisinsDuVoisin) {
                        if (!voisins.contains(v)) {
                            voisins.add(v);
                        }
                    }
                }
            } else if (labels[voisinId] == NOISE) {
                labels[voisinId] = clusterId;
            }
        }
    }

    private List<Integer> getVoisins(int indexPoint, double[][] donnees) {
        List<Integer> voisins = new ArrayList<>();
        double[] pointA = donnees[indexPoint];

        for (int i = 0; i < donnees.length; i++) {
            if (i != indexPoint && distanceEuclidienne(pointA, donnees[i]) <= eps) {
               voisins.add(i);
        }
        }
        return voisins;
    }

    private double distanceEuclidienne(double[] a, double[] b) {
        double somme = 0;
        for (int i = 0; i < a.length; i++) {
            somme += Math.pow(a[i] - b[i], 2);
        }
        return Math.sqrt(somme);
    }
}