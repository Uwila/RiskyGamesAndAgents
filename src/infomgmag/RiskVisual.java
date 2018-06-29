package infomgmag;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.text.DefaultCaret;

public class RiskVisual extends JFrame {
    private static final long serialVersionUID = 1L;

    Risk risk;
    private boolean visible;

    // Define screen size
    int width = 1920, height = 1080;
    int gameWidth = 1600, gameHeight = height;
    int logWidth = width - gameWidth - 20;

    JPanel gamePanel;
    JPanel infoPanel;
    JTextArea infoArea;
    JTextArea logArea;
    JScrollPane logPanel;

    // Define territory radii
    int continentRadius = 50;
    int playerRadius = continentRadius * 3 / 4;
    int attackMarkerTargetRadius = playerRadius * 1 / 2;
    int attackMarkerCrossResolution = playerRadius * 1 / 2;

    boolean drawNames = false;
    long targetFrameDuration;

    Image map;

    public RiskVisual(Risk risk, boolean visible) {
        this.risk = risk;
        this.visible = visible;
        this.targetFrameDuration = 20;

        if (visible) {
            try {
                map = ImageIO.read(new File("src/map.jpg"));
            } catch (Exception e) {
                System.out.println("Could not read image");
                e.printStackTrace();
            }

            gamePanel = new JPanel(new BorderLayout(0, 0));
            gamePanel.setPreferredSize(new Dimension(gameWidth, gameHeight));
            gamePanel.setVisible(true);
            this.add(gamePanel, BorderLayout.WEST);

            infoPanel = new JPanel(new BorderLayout(0, 0));
            infoPanel.setSize(width - gameWidth, height);
            this.add(infoPanel, BorderLayout.EAST);

            logArea = new JTextArea();
            logArea.setSize(logWidth, height * 3 / 4);
            logArea.setEditable(false);
            logArea.setLineWrap(true);
            logArea.setWrapStyleWord(true);

            infoArea = new JTextArea();
            infoArea.setSize(logWidth, height);
            infoArea.setEditable(false);
            infoArea.setLineWrap(true);
            infoArea.setWrapStyleWord(true);
            infoArea.append("Here should be player info \n hahah");
            infoPanel.add(infoArea, BorderLayout.NORTH);

            logPanel = new JScrollPane(logArea);
            logPanel.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
            logPanel.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            logPanel.setPreferredSize(new Dimension(width - gameWidth, height * 1 / 4));
            DefaultCaret caret = (DefaultCaret) logArea.getCaret();
            caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
            infoPanel.add(logPanel, BorderLayout.SOUTH);

            // Standard window building code
            this.setDefaultCloseOperation(EXIT_ON_CLOSE);
            this.setSize(width, height);
            this.pack();
            this.setVisible(true);
            this.setExtendedState(JFrame.MAXIMIZED_BOTH);
            GraphicsDevice d = GraphicsEnvironment
                    .getLocalGraphicsEnvironment().getDefaultScreenDevice();
            d.setFullScreenWindow(this);

            this.buffer = new BufferedImage(gameWidth, gameHeight, BufferedImage.TYPE_INT_ARGB);

            this.lastFrameTime = System.currentTimeMillis();
        }
    }

    BufferedImage buffer;
    Graphics2D g;

    private int rasterX(double x) {
        return (int) (x * gameWidth);
    }

    private int rasterY(double y) {
        return gameHeight - (int) (y * gameHeight);
    }

    public void log(String s) {
        s += "\n";
        if (visible)
            logArea.append(s);
        else
            System.out.print(s);
    }

    public void update() {
        if (!visible)
            return;
        createBuffer();
        drawMap(g);
        drawBuffer();
    }

    public void update(CombatMove cm) {
        if (!visible)
            return;
        createBuffer();
        drawMap(g);
        g.setColor(Color.RED);
        drawArrow(cm.getAttackingTerritory(), cm.getDefendingTerritory());
        drawBuffer();
    }

    public void updateWithReinforcement(Territory t, int units) {
        if (!visible)
            return;
        createBuffer();
        drawMap(g);
        drawReinforcements(t,units);
        drawBuffer();
    }

    public void updateWithFortification(Territory from, Territory to, int units) {
        if (!visible)
            return;
        createBuffer();
        drawMap(g);
        g.setColor(Color.BLUE);
        drawArrow(from, to);
        drawReinforcements(to, units);
        drawBuffer();
    }

    private void drawReinforcements(Territory t, int units) {
        int centerX = rasterX(t.x);
        int centerY = rasterY(t.y);
        g.setColor(Color.WHITE);
        g.fillOval(centerX + continentRadius/2, centerY - continentRadius/2, continentRadius, continentRadius);
        String nr = "+" + units;
        g.setColor(Color.BLACK);
        g.drawString(nr, centerX + continentRadius / 2, centerY + 10);
    }

    public void createBuffer() {
        // One draws to buffer, then buffer to screen, to prevent flickering
        g = buffer.createGraphics();
        g.setStroke(new BasicStroke(5));

        Font currentFont = g.getFont();
        Font newFont = currentFont.deriveFont(currentFont.getSize() * 2F);
        g.setFont(newFont);

        // Background
        g.setColor(Color.WHITE);
        if (map == null)
            g.fillRect(0, 0, gameWidth, gameHeight);
        else
            g.drawImage(map, 0, 0, gameWidth, gameHeight, null);
    }

    long frameDuration = 1000;
    long lastFrameTime = targetFrameDuration;

    public void drawBuffer() {
        frameDuration = System.currentTimeMillis() - lastFrameTime;
        if (frameDuration < targetFrameDuration)
            try {
                Thread.sleep(targetFrameDuration - frameDuration);
            } catch (InterruptedException e) {
                System.exit(0);
            }
        lastFrameTime = System.currentTimeMillis();
        gamePanel.getGraphics().drawImage(buffer, 0, 0, null);

        updateGameInfo();
    }

    private void updateGameInfo() {
        String info = new String();

        info += "Turn " + risk.getTurn() + "\n";

        info += "Current state:\n\n";
        info += "Value golden cavalry: " + risk.getBoard().getGoldenCavalry() + "\n\n";

        for (Player p : risk.getActivePlayers()) {
            info += p.getName();
            if (risk.getCurrentPlayer() == p)
                info += " (current)";
            info += "\nHand:\n";

            info += "Infantry: " + p.hand.getInfantry() + "\n";
            info += "Cavalry: " + p.hand.getCavalry() + "\n";
            info += "Artillery: " + p.hand.getArtillery() + "\n";
            info += "Wildcards: " + p.hand.getWildcards() + "\n";

            info += "\n";
        }

        info+= "Defeated players:\n";
        for(Player p : risk.getDefeatedPlayers()){
            info+= p.name + "\n";
        }

        this.infoArea.setText(info);
    }

    public void drawArrow(Territory from, Territory to) {
        int attackingX = rasterX(from.x);
        int attackingY = rasterY(from.y);
        int defendingX = rasterX(to.x);
        int defendingY = rasterY(to.y);

        if ((from.getName() == "Alaska" && to.getName() == "Kamchatka")
                || (from.getName() == "Kamchatka" && to.getName() == "Alaska")) {

            int alaskaX, alaskaY, kamchatkaX, kamchatkaY;

            if (from.getName() == "Alaska") {
                alaskaX = attackingX;
                alaskaY = attackingY;
                kamchatkaX = defendingX;
                kamchatkaY = defendingY;
            } else {
                alaskaX = defendingX;
                alaskaY = defendingY;
                kamchatkaX = attackingX;
                kamchatkaY = attackingY;
            }

            g.drawLine(alaskaX, alaskaY, 0, alaskaY);
            g.drawLine(kamchatkaX, kamchatkaY, gameWidth, kamchatkaY);
        } else
            g.drawLine(attackingX, attackingY, defendingX, defendingY);

        g.fillOval(defendingX - attackMarkerTargetRadius / 2, defendingY - attackMarkerTargetRadius / 2,
                attackMarkerTargetRadius, attackMarkerTargetRadius);
        g.drawLine(attackingX - attackMarkerCrossResolution / 2, attackingY - attackMarkerCrossResolution / 2,
                attackingX + attackMarkerCrossResolution / 2, attackingY + attackMarkerCrossResolution / 2);
        g.drawLine(attackingX - attackMarkerCrossResolution / 2, attackingY + attackMarkerCrossResolution / 2,
                attackingX + attackMarkerCrossResolution / 2, attackingY - attackMarkerCrossResolution / 2);
    }

    private void drawMap(Graphics2D g) {
        for (Continent c : risk.getBoard().getContinents())
            for (Territory t : c.getTerritories()) {
                int centerX = (int) (t.x * gameWidth);
                int centerY = gameHeight - (int) (t.y * gameHeight);
                // Draw edges
                for (Territory adjacent : t.getAdjacentTerritories()) {
                    g.setColor(Color.BLACK);
                    if (t.getName() == "Alaska" && adjacent.getName() == "Kamchatka")
                        g.drawLine(centerX, centerY, 0, centerY);
                    else if (t.getName() == "Kamchatka" && adjacent.getName() == "Alaska")
                        g.drawLine(centerX, centerY, gameWidth, centerY);
                    else
                        g.drawLine(centerX, centerY, (int) (adjacent.x * gameWidth),
                                gameHeight - (int) (adjacent.y * gameHeight));
                }
            }

        Color continentColor;
        for (Continent c : risk.getBoard().getContinents()) {
            continentColor = c.getColor();
            for (Territory t : c.getTerritories()) {
                int centerX = (int) (t.x * gameWidth);
                int centerY = gameHeight - (int) (t.y * gameHeight);

                // Draw continent
                g.setColor(continentColor);
                g.fillOval(centerX - continentRadius / 2, centerY - continentRadius / 2, continentRadius,
                        continentRadius);
                // Draw player
                g.setColor(t.getOwner().getColor());
                g.fillOval(centerX - playerRadius / 2, centerY - playerRadius / 2, playerRadius, playerRadius);
                // Draw territory names
                if (drawNames) {
                    g.setColor(Color.BLACK);
                    g.drawString(t.getName(), centerX, centerY);
                }
                // Draw unit counts
                g.setColor(Color.WHITE);
                String nr = Integer.toString(t.getUnits());
                int width = g.getFontMetrics().stringWidth(nr);
                g.drawString(nr, centerX - (int)(width / 2), centerY + 10);
            }
        }

        int offset = 10;
        for (Player p : risk.getActivePlayers()) {
            g.setColor(p.getColor());
            g.fillRect(0, offset + 10, 20, 20);
            g.setColor(Color.WHITE);
            g.drawString(p.getName(), 30, offset += 25);
        }
    }

    public void setTargetFrameDuration(long targetFrameDuration) {
        this.targetFrameDuration = targetFrameDuration;
    }
}
