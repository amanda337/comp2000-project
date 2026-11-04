import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Area;

// The small drawing helpers for trouble on the map, in one file:
// HazardStripe (a delayed train), StopSign (a stopped train) and Flame (a
// burning station). Each one only has a static draw method.

// Draws a yellow/red diagonal caution-stripe border around the outside of a
// box. Used to flag a delayed train, both on its side-panel card and on its
// map icon.
public class HazardStripe {

    public static void draw(Graphics g, int x, int y, int width, int height, int thickness) {
        Graphics2D g2d = (Graphics2D) g.create();

        Rectangle outer = new Rectangle(x - thickness, y - thickness,
                width + thickness * 2, height + thickness * 2);
        Rectangle inner = new Rectangle(x, y, width, height);

        Area frame = new Area(outer);
        frame.subtract(new Area(inner));
        g2d.clip(frame);

        int stripeWidth = 10;
        int slant = outer.height;
        boolean yellow = true;
        for (int sx = outer.x - slant; sx < outer.x + outer.width + slant; sx = sx + stripeWidth) {
            if (yellow) {
                g2d.setColor(Color.yellow);
            } else {
                g2d.setColor(Color.red);
            }
            int[] xs = {sx, sx + stripeWidth, sx + stripeWidth - slant, sx - slant};
            int[] ys = {outer.y, outer.y, outer.y + outer.height, outer.y + outer.height};
            g2d.fillPolygon(xs, ys, 4);
            yellow = !yellow;
        }

        g2d.dispose();
    }
}

// A small red stop sign (octagon with a white "STOP" label), drawn on a train
// that cannot move at all. A merely delayed train gets HazardStripe instead.
class StopSign {

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

// A small flame icon drawn above a burning station: an orange flame with a
// yellow core. (centreX, bottomY) is the middle of its base; size is its height.
class Flame {

    public static void draw(Graphics g, int centreX, int bottomY, int size) {
        int w = size * 2 / 3;
        int[] xs = {centreX, centreX + w / 2, centreX + w / 3, centreX, centreX - w / 3, centreX - w / 2};
        int[] ys = {bottomY - size, bottomY - size / 3, bottomY, bottomY - size / 4, bottomY, bottomY - size / 3};
        g.setColor(new Color(230, 70, 20));
        g.fillPolygon(xs, ys, 6);

        int[] cx = {centreX, centreX + w / 5, centreX, centreX - w / 5};
        int[] cy = {bottomY - size * 2 / 3, bottomY - size / 4, bottomY, bottomY - size / 4};
        g.setColor(new Color(255, 200, 40));
        g.fillPolygon(cx, cy, 4);
    }
}
