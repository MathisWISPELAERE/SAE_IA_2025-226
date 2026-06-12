import java.awt.Color;

public class NormeRGB implements NormeCouleurs {

    @Override
    public double distanceCouleur(Color c1, Color c2) {

        int r = c1.getRed() - c2.getRed();
        int g = c1.getGreen() - c2.getGreen();
        int b = c1.getBlue() - c2.getBlue();

        return r * r + g * g + b * b;
    }
}