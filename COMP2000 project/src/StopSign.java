import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;

// A small red stop sign (octagon with a white "STOP" label), drawn on a train
// that cannot move at all. A merely delayed train gets HazardStripe instead.
public class StopSign {

    // (centreX, centreY) is the middle of the sign; size is its width and height.
    public static void draw(Graphics g, int centreX, int centreY, int size) {
        double radius = size / 2.0;
        int[] xs = new int[8];
        int[] ys = new int[8];
        for (int i = 0; i < 8; i++) {
            // Eight corners, turned 22.5 degrees so the flat sides face up and down.
            double angle = Math.toRadians(22.5 + i * 45);
            xs[i] = (int) Math.round(centreX + radius * Math.cos(angle));
            ys[i] = (int) Math.round(centreY + radius * Math.sin(angle));
        }

        g.setColor(new Color(200, 20, 20));
        g.fillPolygon(xs, ys, 8);
        g.setColor(Color.white);
        g.drawPolygon(xs, ys, 8);

        g.setFont(new Font("SansSerif", Font.BOLD, Math.max(7, size / 3)));
        FontMetrics fm = g.getFontMetrics();
        g.drawString("STOP", centreX - fm.stringWidth("STOP") / 2,
                centreY + fm.getAscent() / 2 - 1);
    }
}
