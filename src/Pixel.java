import java.awt.Color;

/**
 * Représente un pixel de la carte avec sa position et sa couleur.
 */
public class Pixel {

    private final int   x;
    private final int   y;
    private final Color couleur;

    public Pixel(int x, int y, Color couleur) {
        this.x       = x;
        this.y       = y;
        this.couleur = couleur;
    }

    public int getX(){return x;}
    public int getY(){return y;}
    public Color getCouleur() {return couleur;}

    /** Retourne la couleur sous forme de tableau [R, G, B]. */
    public double[] getCouleurCommeTableau() {
        return new double[]{ couleur.getRed(), couleur.getGreen(), couleur.getBlue() };
    }

    /** Retourne la position sous forme de tableau [x, y]. */
    public double[] getPositionCommeTableau() {
        return new double[]{ x, y };
    }

    @Override
    public String toString() {
        return "Pixel[" + x + "," + y + "] RGB(" + couleur.getRed()   + "," + couleur.getGreen() + "," + couleur.getBlue()  + ")";
    }
}