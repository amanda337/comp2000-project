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

// title screen with a background picture and two buttons, START and INSTRUCTION
public class Home extends Frame {

    int width = 1280;
    int height = 720;

    int buttonWidth = 200;
    int buttonHeight = 50;
    int buttonGap = 16;
    int buttonMargin = 40;

    Color background = new Color(238, 238, 238);
    Color buttonFill = new Color(120, 120, 120);

    Image backgroundImage;

    // builds the window and opens the next screen when a button is clicked
    public Home() {
        setTitle("Transit Sim - Home");
        setSize(width, height);
        // load the picture first so the first paint is not blank
        backgroundImage = Assets.load("home.png");
        setVisible(true);
        // read the next screens' pictures in the background so they open fast
        Assets.preload("1.png", "ins.png");

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
                if (isOverButton(e.getX(), e.getY(), startY())) {
                    dispose();
                    new Panel();
                } else if (isOverButton(e.getX(), e.getY(), instructionY())) {
                    dispose();
                    new Instruction();
                }
            }
        });
    }

    // both buttons share one x, bottom right
    private int buttonX() {
        return width - buttonMargin - buttonWidth;
    }

    private int startY() {
        return height - buttonMargin - buttonHeight * 2 - buttonGap;
    }

    private int instructionY() {
        return height - buttonMargin - buttonHeight;
    }

    // true if the mouse is on the button at this y
    private boolean isOverButton(int mouseX, int mouseY, int buttonY) {
        int x = buttonX();
        return mouseX >= x && mouseX <= x + buttonWidth
                && mouseY >= buttonY && mouseY <= buttonY + buttonHeight;
    }

    // background, title and the two buttons
    public void paint(Graphics g) {
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, width, height, this);
        } else {
            g.setColor(background);
            g.fillRect(0, 0, width, height);
        }
        g.setColor(Color.black);
        g.drawRect(0, 0, width - 1, height - 1);

        drawTitle(g);
        drawButton(g, startY(), "START");
        drawButton(g, instructionY(), "INSTRUCTION");
    }

    // dark strip behind the title so it is easy to read, as wide as the text
    private void drawTitle(Graphics g) {
        String title = "Train Simulation";
        g.setFont(new Font("SansSerif", Font.BOLD, 42));
        FontMetrics fm = g.getFontMetrics();
        int stripWidth = fm.stringWidth(title) + 60;

        g.setColor(new Color(0, 0, 0, 130));
        g.fillRect(0, 30, stripWidth, 70);

        g.setColor(Color.white);
        g.drawString(title, 30, 80);
    }

    // grey box with the label in the middle
    private void drawButton(Graphics g, int y, String label) {
        int x = buttonX();
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

    // the program start from here, the window is built on the event thread
    // (the thread that also handles the clicks, the timer and the painting)
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(Home::new);
    }
}
