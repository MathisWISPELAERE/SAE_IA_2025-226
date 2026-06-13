import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * Interface graphique de visualisation et d'analyse des biomes.
 *
 * Fonctionnalités :
 *  - Liste les cartes disponibles dans src/cartes/
 *  - Applique KMeans (avec flou gaussien) et affiche la légende des biomes détectés
 *  - Bascule entre vue normale et vue par biomes (couleur moyenne du cluster)
 *  - En vue biome : clic sur une entrée de légende = filtre visuel (biome en couleur, reste N&B)
 *  - Checkbox "Afficher par écosystème" : lance HAC sur le plus grand cluster KMeans
 *    et colore chaque sous-cluster avec une couleur HSB vive (fond N&B)
 */
public class InterfaceCarte extends JFrame {

    private static final String DOSSIER_CARTES = "src/cartes/";

    // ---------- Paramètres KMeans ----------
    private static final int    K        = 10;
    private static final int    MAX_ITER = 50;

    // ---------- Paramètres flou gaussien ----------
    private static final int    FLOU_TAILLE = 11;
    private static final double FLOU_SIGMA  = 3.0;

    // ---------- Paramètres HAC ----------
    private static final double HAC_SEUIL = 20.0;

    // ---------- État applicatif ----------
    private BufferedImage imageOriginale;   // image brute chargée depuis le disque
    private BufferedImage imageParBiome;    // pixels recolorés avec la couleur moyenne de leur cluster KMeans
    private BufferedImage imageHAC;         // fond N&B + sous-clusters HAC en couleurs HSB vives
    private List<Cluster> clusters;         // résultat KMeans

    private boolean modeParBiome      = false;
    private boolean modeEcosysteme    = false;   // checkbox HAC
    private String  biomeFiltreActif  = null;    // null = tous affichés

    // ---------- Composants UI ----------
    private final JPanel        panelImage;
    private final JPanel        panelLegende;
    private final JScrollPane   scrollLegende;
    private final JList<String> listeCartes;
    private final JToggleButton btnBasculer;
    private final JCheckBox     chkEcosysteme;
    private final JLabel        labelStatus;

    // nomBiome → couleur moyenne (utilisée dans la légende)
    private final Map<String, Color> couleursBiomes = new LinkedHashMap<>();

    // ============================================================
    //  Constructeur
    // ============================================================
    public InterfaceCarte() {
        super("Visualisation des cartes - Détection de biomes");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1300, 850);
        setLocationRelativeTo(null);

        // ---------- Panneau gauche : liste des cartes ----------
        listeCartes = new JList<>(listerImages());
        listeCartes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listeCartes.setFont(new Font("SansSerif", Font.PLAIN, 13));
        JScrollPane scrollListe = new JScrollPane(listeCartes);
        scrollListe.setBorder(BorderFactory.createTitledBorder("Cartes disponibles"));
        scrollListe.setPreferredSize(new Dimension(200, 0));

        // ---------- Panneau central : dessin custom ----------
        panelImage = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                BufferedImage img = imageCourante();
                if (img == null) {
                    g.setColor(Color.GRAY);
                    g.drawString("Sélectionnez une carte à gauche", 40, 40);
                    return;
                }
                double ratioW = (double) getWidth()  / img.getWidth();
                double ratioH = (double) getHeight() / img.getHeight();
                double ratio  = Math.min(ratioW, ratioH);
                if (ratio > 1) ratio = 1;
                int w  = (int) (img.getWidth()  * ratio);
                int h  = (int) (img.getHeight() * ratio);
                int ox = (getWidth()  - w) / 2;
                int oy = (getHeight() - h) / 2;

                // En vue biome (hors mode écosystème) avec un filtre actif → image filtrée N&B
                if (modeParBiome && !modeEcosysteme && biomeFiltreActif != null) {
                    g.drawImage(construireImageFiltre(biomeFiltreActif,
                                img.getWidth(), img.getHeight()), ox, oy, w, h, this);
                } else {
                    g.drawImage(img, ox, oy, w, h, this);
                }
            }
        };
        panelImage.setBackground(new Color(40, 40, 40));

        // ---------- Contrôles du panneau droit ----------
        btnBasculer = new JToggleButton("🗺  Vue normale");
        btnBasculer.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnBasculer.setEnabled(false);
        btnBasculer.addActionListener(e -> basculerMode());

        chkEcosysteme = new JCheckBox("Afficher par écosystème");
        chkEcosysteme.setFont(new Font("SansSerif", Font.PLAIN, 12));
        chkEcosysteme.setEnabled(false);   // activée uniquement en vue biome
        chkEcosysteme.addActionListener(e -> basculerEcosysteme());

        labelStatus = new JLabel("  Aucune carte chargée");
        labelStatus.setFont(new Font("SansSerif", Font.ITALIC, 11));
        labelStatus.setForeground(Color.DARK_GRAY);

        // ---------- Panneau droit ----------
        panelLegende = new JPanel();
        panelLegende.setLayout(new BoxLayout(panelLegende, BoxLayout.Y_AXIS));
        panelLegende.setBorder(new EmptyBorder(6, 6, 6, 6));

        scrollLegende = new JScrollPane(panelLegende);
        scrollLegende.setBorder(null);

        // Regroupement des contrôles en haut du panneau droit
        JPanel panelControles = new JPanel();
        panelControles.setLayout(new BoxLayout(panelControles, BoxLayout.Y_AXIS));
        panelControles.add(btnBasculer);
        panelControles.add(Box.createVerticalStrut(4));
        panelControles.add(chkEcosysteme);

        JPanel panelDroit = new JPanel(new BorderLayout(0, 6));
        panelDroit.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Biomes"),
            new EmptyBorder(4, 4, 4, 4)
        ));
        panelDroit.setPreferredSize(new Dimension(230, 0));
        panelDroit.add(panelControles,  BorderLayout.NORTH);
        panelDroit.add(scrollLegende,   BorderLayout.CENTER);
        panelDroit.add(labelStatus,     BorderLayout.SOUTH);

        // ---------- Assemblage ----------
        listeCartes.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String nom = listeCartes.getSelectedValue();
                if (nom != null) chargerCarte(nom);
            }
        });

        add(scrollListe, BorderLayout.WEST);
        add(panelImage,  BorderLayout.CENTER);
        add(panelDroit,  BorderLayout.EAST);
    }

    // ============================================================
    //  Chargement + analyse
    // ============================================================

    private void chargerCarte(String nomFichier) {
        // Réinitialisation complète
        imageOriginale   = null;
        imageParBiome    = null;
        imageHAC         = null;
        clusters         = null;
        modeParBiome     = false;
        modeEcosysteme   = false;
        biomeFiltreActif = null;
        btnBasculer.setSelected(false);
        btnBasculer.setText("🗺  Vue normale");
        btnBasculer.setEnabled(false);
        chkEcosysteme.setSelected(false);
        chkEcosysteme.setEnabled(false);
        couleursBiomes.clear();
        panelLegende.removeAll();
        labelStatus.setText("  Chargement…");
        panelImage.repaint();

        File fichier = new File(DOSSIER_CARTES + nomFichier);
        if (!fichier.exists()) { labelStatus.setText("  Fichier introuvable"); return; }

        SwingWorker<Void, String> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                publish("  Chargement de l'image…");
                imageOriginale = ImageIO.read(fichier);

                publish("  Flou gaussien (" + FLOU_TAILLE + "x" + FLOU_TAILLE + ", σ=" + FLOU_SIGMA + ")…");
                BufferedImage imageFloutee = FlouGaussien.apply(imageOriginale, FLOU_TAILLE, FLOU_SIGMA);

                publish("  Analyse KMeans (k=" + K + ")…");
                clusters = new KMeans(K, MAX_ITER).cluster(extrairePixels(imageFloutee));

                publish("  Construction des vues…");
                construireVueBiome();
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                labelStatus.setText(chunks.get(chunks.size() - 1));
            }

            @Override
            protected void done() {
                try {
                    get();
                    construireLegendeBiomes();
                    btnBasculer.setEnabled(true);
                    labelStatus.setText("  " + clusters.size() + " biomes détectés");
                    panelImage.repaint();
                } catch (Exception ex) {
                    labelStatus.setText("  Erreur : " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        };
        worker.execute();
    }

    /** Extrait tous les pixels d'une BufferedImage sous forme de List<Pixel>. */
    private List<Pixel> extrairePixels(BufferedImage img) {
        List<Pixel> liste = new ArrayList<>(img.getWidth() * img.getHeight());
        for (int y = 0; y < img.getHeight(); y++)
            for (int x = 0; x < img.getWidth(); x++)
                liste.add(new Pixel(x, y, new Color(img.getRGB(x, y))));
        return liste;
    }

    /** Recolore chaque pixel avec la couleur moyenne de son cluster KMeans. */
    private void construireVueBiome() {
        int w = imageOriginale.getWidth(), h = imageOriginale.getHeight();
        imageParBiome = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (Cluster cl : clusters) {
            int rgb = cl.getCouleurMoyenne().getRGB();
            for (Pixel p : cl.getPixels())
                imageParBiome.setRGB(p.getX(), p.getY(), rgb);
        }
    }

    /**
     * Lance HAC sur le cluster correspondant au biome sélectionné dans la légende
     * (biomeFiltreActif), ou sur le plus petit cluster si aucun biome n'est sélectionné.
     * Construit imageHAC : fond N&B + sous-clusters en couleurs HSB vives.
     */
    private void construireVueEcosysteme() {
        int w = imageOriginale.getWidth(), h = imageOriginale.getHeight();

        // Déterminer les pixels cibles
        List<Pixel> pixelsCibles;
        if (biomeFiltreActif != null) {
            // Fusionner tous les clusters du biome sélectionné
            pixelsCibles = new ArrayList<>();
            for (Cluster cl : clusters)
                if (biomeFiltreActif.equals(cl.getBiome()))
                    pixelsCibles.addAll(cl.getPixels());
            System.out.println("  HAC sur biome '" + biomeFiltreActif + "' : " + pixelsCibles.size() + " pixels");
        } else {
            // Aucun biome sélectionné : prendre le plus petit cluster
            Cluster cible = clusters.get(0);
            for (Cluster cl : clusters)
                if (cl.getNbPixels() < cible.getNbPixels()) cible = cl;
            pixelsCibles = cible.getPixels();
            System.out.println("  HAC sur le plus petit cluster (" + cible.getBiome() + ") : " + pixelsCibles.size() + " pixels");
        }

        // Lancer HAC
        List<Cluster> sousClusters = new HACEcosystemes(HAC_SEUIL).cluster(pixelsCibles);
        System.out.println("  HAC terminé : " + sousClusters.size() + " sous-clusters");

        // Étape A : fond en niveaux de gris (luminance de l'image originale)
        imageHAC = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                Color c = new Color(imageOriginale.getRGB(x, y));
                int gris = (int) (0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue());
                imageHAC.setRGB(x, y, new Color(gris, gris, gris).getRGB());
            }
        }

        // Étape B : palette HSB vive, une couleur par sous-cluster
        int nb = sousClusters.size();
        Color[] palette = new Color[nb];
        for (int i = 0; i < nb; i++)
            palette[i] = Color.getHSBColor((float) i / nb, 1.0f, 1.0f);

        // Étape C : colorier les pixels de chaque sous-cluster
        for (int i = 0; i < nb; i++) {
            int rgb = palette[i].getRGB();
            for (Pixel p : sousClusters.get(i).getPixels())
                imageHAC.setRGB(p.getX(), p.getY(), rgb);
        }
    }

    /**
     * Construit une image où seul le biome {@code biomeCible} est affiché
     * en couleur ; tous les autres pixels sont convertis en niveaux de gris.
     */
    private BufferedImage construireImageFiltre(String biomeCible, int w, int h) {
        BufferedImage img    = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        BufferedImage source = imageParBiome != null ? imageParBiome : imageOriginale;

        // Fond N&B basé sur l'image originale
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                Color c = new Color(imageOriginale.getRGB(x, y));
                int gris = (int) (0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue());
                img.setRGB(x, y, new Color(gris, gris, gris).getRGB());
            }
        }

        // Pixels du biome cible en couleur
        for (Cluster cl : clusters) {
            if (!biomeCible.equals(cl.getBiome())) continue;
            for (Pixel p : cl.getPixels())
                img.setRGB(p.getX(), p.getY(), source.getRGB(p.getX(), p.getY()));
        }
        return img;
    }

    // ============================================================
    //  Légende
    // ============================================================

    private void construireLegendeBiomes() {
        couleursBiomes.clear();
        panelLegende.removeAll();

        // Regrouper par nom de biome, garder la couleur du plus grand cluster
        Map<String, Integer> tailleMax  = new LinkedHashMap<>();
        Map<String, Color>   couleursMap = new LinkedHashMap<>();
        for (Cluster cl : clusters) {
            String biome = cl.getBiome() != null ? cl.getBiome() : "Inconnu";
            if (!tailleMax.containsKey(biome) || cl.getNbPixels() > tailleMax.get(biome)) {
                tailleMax.put(biome, cl.getNbPixels());
                couleursMap.put(biome, cl.getCouleurMoyenne());
            }
        }

        // Trier par taille décroissante
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(tailleMax.entrySet());
        entries.sort((a, b) -> b.getValue() - a.getValue());

        for (Map.Entry<String, Integer> entry : entries) {
            String biome  = entry.getKey();
            Color  couleur = couleursMap.get(biome);
            couleursBiomes.put(biome, couleur);
            panelLegende.add(creerEntreeLegende(biome, couleur));
            panelLegende.add(Box.createVerticalStrut(4));
        }

        panelLegende.revalidate();
        panelLegende.repaint();
    }

    private JPanel creerEntreeLegende(String biome, Color couleur) {
        JPanel ligne = new JPanel(new BorderLayout(8, 0));
        ligne.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        ligne.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true),
            new EmptyBorder(4, 8, 4, 8)
        ));
        ligne.setBackground(UIManager.getColor("Panel.background"));
        ligne.putClientProperty("biome", biome);

        JPanel pastille = new JPanel();
        pastille.setBackground(couleur);
        pastille.setPreferredSize(new Dimension(22, 22));
        pastille.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));

        JLabel label = new JLabel(biome);
        label.setFont(new Font("SansSerif", Font.PLAIN, 13));

        ligne.add(pastille, BorderLayout.WEST);
        ligne.add(label,    BorderLayout.CENTER);
        ligne.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        ligne.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (!modeParBiome) return;
                biomeFiltreActif = biome.equals(biomeFiltreActif) ? null : biome;
                mettreAJourStylesLegende();
                if (modeEcosysteme) {
                    // Relancer HAC sur le nouveau biome sélectionné
                    basculerEcosysteme();
                } else {
                    panelImage.repaint();
                }
            }
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (modeParBiome && !biome.equals(biomeFiltreActif))
                    ligne.setBackground(new Color(230, 240, 255));
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (!biome.equals(biomeFiltreActif))
                    ligne.setBackground(UIManager.getColor("Panel.background"));
            }
        });

        return ligne;
    }

    private void mettreAJourStylesLegende() {
        for (Component comp : panelLegende.getComponents()) {
            if (!(comp instanceof JPanel)) continue;
            JPanel ligne = (JPanel) comp;
            String biome = (String) ligne.getClientProperty("biome");
            if (biome == null) continue;

            if (!modeParBiome) {
                ligne.setBackground(UIManager.getColor("Panel.background"));
                ligne.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true),
                    new EmptyBorder(4, 8, 4, 8)
                ));
            } else if (biome.equals(biomeFiltreActif)) {
                ligne.setBackground(new Color(195, 220, 255));
                ligne.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(60, 120, 220), 2, true),
                    new EmptyBorder(3, 7, 3, 7)
                ));
            } else {
                ligne.setBackground(UIManager.getColor("Panel.background"));
                ligne.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true),
                    new EmptyBorder(4, 8, 4, 8)
                ));
            }
        }
        panelLegende.repaint();
    }

    // ============================================================
    //  Bascules
    // ============================================================

    private void basculerMode() {
        modeParBiome     = btnBasculer.isSelected();
        modeEcosysteme   = false;
        biomeFiltreActif = null;
        chkEcosysteme.setSelected(false);

        // La checkbox est disponible uniquement en vue biome
        chkEcosysteme.setEnabled(modeParBiome);

        btnBasculer.setText(modeParBiome ? "🎨 Vue biomes" : "🗺  Vue normale");
        mettreAJourStylesLegende();
        panelImage.repaint();
    }

    private void basculerEcosysteme() {
        modeEcosysteme   = chkEcosysteme.isSelected();

        if (modeEcosysteme) {
            // Toujours relancer HAC : le biome cible peut avoir changé
            imageHAC = null;
            labelStatus.setText("  Calcul HAC en cours…");
            chkEcosysteme.setEnabled(false);
            btnBasculer.setEnabled(false);

            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    construireVueEcosysteme();
                    return null;
                }
                @Override
                protected void done() {
                    chkEcosysteme.setEnabled(true);
                    btnBasculer.setEnabled(true);
                    labelStatus.setText("  Vue écosystèmes prête");
                    mettreAJourStylesLegende();
                    panelImage.repaint();
                }
            };
            worker.execute();
            return;
        }

        mettreAJourStylesLegende();
        panelImage.repaint();
    }

    // ============================================================
    //  Helpers
    // ============================================================

    /** Retourne l'image à dessiner selon le mode courant. */
    private BufferedImage imageCourante() {
        if (modeParBiome && modeEcosysteme && imageHAC != null) return imageHAC;
        if (modeParBiome && imageParBiome != null) return imageParBiome;
        return imageOriginale;
    }

    private String[] listerImages() {
        File dossier = new File(DOSSIER_CARTES);
        File[] fichiers = dossier.listFiles((d, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg");
        });
        if (fichiers == null || fichiers.length == 0)
            return new String[]{"(dossier introuvable : " + DOSSIER_CARTES + ")"};
        Arrays.sort(fichiers, Comparator.comparing(File::getName));
        String[] noms = new String[fichiers.length];
        for (int i = 0; i < fichiers.length; i++) noms[i] = fichiers[i].getName();
        return noms;
    }

    // ============================================================
    //  Point d'entrée
    // ============================================================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new InterfaceCarte().setVisible(true));
    }
}