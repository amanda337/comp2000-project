import java.awt.Color;
import java.awt.Graphics;

// A small flame icon drawn above a burning station: an orange flame with a
// yellow core. (centreX, bottomY) is the middle of its base; size is its height.
public class Flame {

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
