import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class Cluster {

    private final List<Pixel> pixels;
    private double[] couleurClusteur; // {R, G, B}

    // Centroïde spatial (position moyenne des pixels) — utilisé par HAC
    private double centroideX;
    private double centroideY;

    // Nom du biome détecté à partir de la couleur moyenne du cluster
    private String biome;

    // -------------------------------------------------------------------------
    // Table de référence des biomes (couleurs RGB extraites de l'image)
    // -------------------------------------------------------------------------
    private static final Object[][] BIOMES_REFERENCE = {
        { "Tundra",          new Color( 71,  70,  61) },
        { "Taïga",           new Color( 43,  50,  35) },
        { "Forêt tempérée",  new Color( 59,  66,  43) },
        { "Forêt tropicale", new Color( 46,  64,  34) },
        { "Savane",          new Color( 84, 106,  70) },
        { "Prairie",         new Color(104,  95,  82) },
        { "Désert",          new Color(152, 140, 120) },
        { "Glacier",         new Color(200, 200, 200) },
        { "Eau peu profonde",new Color( 49,  83, 100) },
        { "Eau profonde",    new Color( 12,  31,  47) },
    };

    // -------------------------------------------------------------------------
    // Constructeurs
    // -------------------------------------------------------------------------
    public Cluster() {
        this.pixels = new ArrayList<>();
    }

    public Cluster(List<Pixel> c) {
        this.pixels = new ArrayList<>(c);
        calculerCouleurMoyenne();
        calculerCentroideSpatial();
        detecterBiome();
    }

    // -------------------------------------------------------------------------
    // Ajout / fusion
    // -------------------------------------------------------------------------
    public void ajouterPixel(Pixel p) {
        pixels.add(p);
    }

    /**
     * Fusionne tous les pixels d'un autre cluster dans celui-ci,
     * puis recalcule le centroïde spatial.
     */
    public void fusionner(Cluster autre) {
        pixels.addAll(autre.pixels);
        calculerCentroideSpatial();
    }

    // -------------------------------------------------------------------------
    // Calculs internes
    // -------------------------------------------------------------------------
    private void calculerCouleurMoyenne() {
        if (pixels.isEmpty()) { couleurClusteur = new double[]{0, 0, 0}; return; }
        double sumR = 0, sumG = 0, sumB = 0;
        for (Pixel p : pixels) {
            sumR += p.getCouleur().getRed();
            sumG += p.getCouleur().getGreen();
            sumB += p.getCouleur().getBlue();
        }
        couleurClusteur = new double[]{ sumR / pixels.size(), sumG / pixels.size(), sumB / pixels.size() };
    }

    public void calculerCentroideSpatial() {
        if (pixels.isEmpty()) { centroideX = 0; centroideY = 0; return; }
        double sx = 0, sy = 0;
        for (Pixel p : pixels) { sx += p.getX(); sy += p.getY(); }
        centroideX = sx / pixels.size();
        centroideY = sy / pixels.size();
    }

    /**
     * Détermine le biome le plus proche en comparant la couleur moyenne
     * du cluster à chaque couleur de référence (distance euclidienne RGB).
     * Appelé automatiquement après chaque recalcul de couleur moyenne.
     */
    public void detecterBiome() {
        if (couleurClusteur == null) { biome = "Inconnu"; return; }

        double distMin = Double.MAX_VALUE;
        String biomeLePlusProche = "Inconnu";

        for (Object[] ref : BIOMES_REFERENCE) {
            String nomBiome   = (String) ref[0];
            Color  couleurRef = (Color)  ref[1];

            double dR = couleurClusteur[0] - couleurRef.getRed();
            double dG = couleurClusteur[1] - couleurRef.getGreen();
            double dB = couleurClusteur[2] - couleurRef.getBlue();
            double dist = dR * dR + dG * dG + dB * dB;

            if (dist < distMin) {
                distMin = dist;
                biomeLePlusProche = nomBiome;
            }
        }
        biome = biomeLePlusProche;
    }

    // -------------------------------------------------------------------------
    // Getters / setters
    // -------------------------------------------------------------------------
    public double distanceSpatiale(Cluster autre) {
        double dx = this.centroideX - autre.centroideX;
        double dy = this.centroideY - autre.centroideY;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public void setcouleurClusteur(double[] couleurClusteur) {
        this.couleurClusteur = couleurClusteur;
        detecterBiome(); // recalculer le biome dès que la couleur change (utilisé par KMeans)
    }

    public List<Pixel> getPixels()          { return pixels;          }
    public double[]    getcouleurClusteur() { return couleurClusteur; }
    public int         getNbPixels()        { return pixels.size();   }
    public double      getCentroideX()      { return centroideX;      }
    public double      getCentroideY()      { return centroideY;      }
    public String      getBiome()           { return biome;           }

    public Color getCouleurMoyenne() {
        return new Color(
            (int) Math.round(couleurClusteur[0]),
            (int) Math.round(couleurClusteur[1]),
            (int) Math.round(couleurClusteur[2])
        );
    }

    public void finaliser() {
        calculerCouleurMoyenne();
        calculerCentroideSpatial();
        detecterBiome();
        // recalcule a son appel plutot qu'à chaque étape de la construction du cluster 
    }

    @Override
    public String toString() {
        return "Cluster[" + getNbPixels() + " px, biome=" + biome
             + ", RGB=(" + Math.round(couleurClusteur[0]) + ","
             + Math.round(couleurClusteur[1]) + ","
             + Math.round(couleurClusteur[2]) + ")]";
    }
}