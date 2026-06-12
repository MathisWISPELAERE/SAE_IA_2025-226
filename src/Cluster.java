import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class Cluster {

    private final List<Pixel> pixels;
    private double[] couleurClusteur; //{R,G,B}

    public Cluster() {
        this.pixels = new ArrayList<>();
    }

    public void ajouterPixel(Pixel p) {
        pixels.add(p);
    }

    public void setcouleurClusteur(double[] couleurClusteur) {
        this.couleurClusteur = couleurClusteur;
    }

    public List<Pixel> getPixels()   { return pixels;         }
    public double[]    getcouleurClusteur(){ return couleurClusteur;      }
    public int         getNbPixels() { return pixels.size();  }

    public Color getCouleurMoyenne() {
        return new Color((int) Math.round(couleurClusteur[0]), (int) Math.round(couleurClusteur[1]), (int) Math.round(couleurClusteur[2]));
    }
}