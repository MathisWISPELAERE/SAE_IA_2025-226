import java.awt.Color;

/**
 * Distance euclidienne dans l'espace RGB :
 * d(c1,c2) = (R1-R2)² + (G1-G2)² + (B1-B2)²
 * (sans racine carrée, suffisant pour comparer des distances)
 */
public class NormeEuclidienne implements NormeCouleurs {

    @Override
    public double distanceCouleur(Color c1, Color c2) {
        int dR = c1.getRed()   - c2.getRed();
        int dG = c1.getGreen() - c2.getGreen();
        int dB = c1.getBlue()  - c2.getBlue();
        return dR * dR + dG * dG + dB * dB;
    }
}