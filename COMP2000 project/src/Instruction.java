import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.Set;

// help screen with the goal and the controls, BACK and NEXT buttons
public class Instruction extends Frame {

    int width = 1280;
    int height = 720;

    int cardTop = 100;
    int cardBottom = 130;  // leaves room for the buttons

    int buttonWidth = 160;
    int buttonHeight = 50;
    int buttonMargin = 40;

    Color background = new Color(238, 238, 238);
    Color cardFill = new Color(255, 255, 255, 235);
    Color buttonFill = new Color(120, 120, 120);

    Image backgroundImage;

    private static final Set<String> HEADINGS =
            Set.of("GOAL", "CONTROLS IN THE SIMULATION", "BUTTONS ON THIS SCREEN");

    String[] lines = {
        "GOAL",
        "Four trains (T1-T4) each run their own coloured line through a shared",
        "network of stations. Passengers appear, board a train at their start",
        "stop, and get off at their destination. Press space, or the play",
        "button, to watch the network run from 06:00 AM until 03:00 AM. All trains",
        "then stop for 3 hours and start again automatically at 6am.",
        "Weather and fires can delay or stop trains along the way.",
        "",
        "CONTROLS IN THE SIMULATION",
        "Space, or the play button (top right): start, pause and resume the clock.",
        "House button (top right): return to this app's home screen.",
        "The 1x, 2x and 4x buttons (next to the house button): change the speed.",
        "Drag anywhere on the map: pan the network. The side panel stays put.",
        "Mouse wheel, or drag its scrollbar, over the left panel: scroll the train list.",
        "The three tabs on the right of the side panel: switch between the train",
        "list (\"Train current\"), the timetable (\"Train table\") and the statistics.",
        "A yellow/red striped border marks a delayed train. A stop sign marks a",
        "stopped train (fog or a large fire). A red ring and flame mark a fire.",
        "",
        "BUTTONS ON THIS SCREEN",
        "BACK returns to the home screen. NEXT starts the simulation."
    };

    // builds the window and handles the button clicks
    public Instruction() {
        setTitle("Transit Sim - Instructions");
        setSize(width, height);
        // load the picture first so the first paint is not blank
        backgroundImage = Assets.load("ins.png");
        setVisible(true);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                System.exit(0);
            }

            public void windowOpened(WindowEvent we) {
                Insets insets = getInsets();
                setSize(width + insets.left + insets.right, height + insets.top + insets.bottom);
                repaint();
            }
        });

        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (isOverButton(e.getX(), e.getY(), backX())) {
                    dispose();
                    new Home();
                } else if (isOverButton(e.getX(), e.getY(), nextX())) {
                    dispose();
                    new Panel();
                }
            }
        });
    }

    // both buttons sit at the bottom
    private int buttonY() {
        return height - buttonMargin - buttonHeight;
    }

    private int backX() {
        return buttonMargin;
    }

    private int nextX() {
        return width - buttonMargin - buttonWidth;
    }

    // true if the mouse is on the button at this x
    private boolean isOverButton(int mouseX, int mouseY, int buttonX) {
        int y = buttonY();
        return mouseX >= buttonX && mouseX <= buttonX + buttonWidth
                && mouseY >= y && mouseY <= y + buttonHeight;
    }

    // background, heading, card and the two buttons
    public void paint(Graphics g) {
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, width, height, this);
        } else {
            g.setColor(background);
            g.fillRect(0, 0, width, height);
        }
        g.setColor(Color.black);
        g.drawRect(0, 0, width - 1, height - 1);

        g.setFont(new Font("SansSerif", Font.BOLD, 32));
        g.setColor(Color.WHITE);
        g.drawString("Instructions", 380, 80);

        drawCard(g);
        drawButton(g, backX(), "BACK");
        drawButton(g, nextX(), "NEXT");
    }

    // the card is as wide as its longest line and sits in the middle
    private void drawCard(Graphics g) {
        int cardWidth = measureCardWidth(g);
        int cardHeight = height - cardTop - cardBottom;
        int cardX = (width - cardWidth) / 2;

        g.setColor(cardFill);
        g.fillRect(cardX, cardTop, cardWidth, cardHeight);
        g.setColor(Color.black);
        g.drawRect(cardX, cardTop, cardWidth, cardHeight);

        int textX = cardX + 30;
        int y = cardTop + 40;
        for (String line : lines) {
            if (isHeading(line)) {
                g.setFont(new Font("SansSerif", Font.BOLD, 18));
                g.setColor(new Color(40, 40, 40));
            } else {
                g.setFont(new Font("SansSerif", Font.PLAIN, 15));
                g.setColor(Color.black);
            }
            g.drawString(line, textX, y);
            y = y + 22;  // line spacing
        }
    }

    // longest line in its own font, plus padding
    private int measureCardWidth(Graphics g) {
        FontMetrics plainFm = g.getFontMetrics(new Font("SansSerif", Font.PLAIN, 15));
        FontMetrics boldFm = g.getFontMetrics(new Font("SansSerif", Font.BOLD, 18));
        return Arrays.stream(lines)
                .mapToInt(line -> {
                    if (isHeading(line)) {
                        return boldFm.stringWidth(line);
                    }
                    return plainFm.stringWidth(line);
                })
                .max()
                .orElse(0) + 60;
    }

    // heading lines are drawn bold
    private boolean isHeading(String line) {
        return HEADINGS.contains(line);
    }

    // grey box with the label in the middle
    private void drawButton(Graphics g, int x, String label) {
        int y = buttonY();
        g.setColor(buttonFill);
        g.fillRect(x, y, buttonWidth, buttonHeight);
        g.setColor(Color.black);
        g.drawRect(x, y, buttonWidth, buttonHeight);

        g.setColor(Color.white);
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        FontMetrics fm = g.getFontMetrics();
        int textX = x + (buttonWidth - fm.stringWidth(label)) / 2;
        int textY = y + (buttonHeight + fm.getAscent()) / 2 - 3;
        g.drawString(label, textX, textY);
    }

    // the program can start from here, built on the event thread like Home
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(Instruction::new);
    }
}
