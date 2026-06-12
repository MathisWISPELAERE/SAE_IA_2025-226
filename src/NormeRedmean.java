import java.awt.Color;

/**
 * Métrique "redmean" :
 * ΔC = sqrt( (2 + r̄/256)ΔR² + 4ΔG² + (2 + (255-r̄)/256)ΔB² )
 */
public class NormeRedmean implements NormeCouleurs {

    @Override
    public double distanceCouleur(Color c1, Color c2) {
        double rBar = 0.5 * (c1.getRed() + c2.getRed());

        double dR = c1.getRed()   - c2.getRed();
        double dG = c1.getGreen() - c2.getGreen();
        double dB = c1.getBlue()  - c2.getBlue();

        return Math.sqrt(
            (2.0 + rBar / 256.0)         * dR * dR
          + 4.0                           * dG * dG
          + (2.0 + (255.0 - rBar) / 256.0) * dB * dB
        );
    }
}