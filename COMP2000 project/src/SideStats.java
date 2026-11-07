import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// The "Statistics" view: one row per train with how many passengers it has
// delivered, the average wait and the on-time %, plus a total row.
//
// Nothing in Passenger records these numbers, so SidePanel calls update() every
// frame and this class works them out by watching the trains:
//   delivered    - a passenger who was aboard last frame and is gone now got off
//   average wait - minutes a passenger spent sitting in a delayed or stopped
//                  train (waiting at a platform is not tracked)
//   on time %    - share of service time the train was running, not delayed
//                  or stopped
public class SideStats implements SideView {

    private final int leftX;
    private final int top;
    private final int rightX;

    private final Color body = new Color(220, 220, 220);
    private final Color headerRow = new Color(190, 190, 190);
    private final Color gridLine = new Color(170, 170, 170);

    // The numbers for one train.
    private static class Row {
        int delivered = 0;
        int aboard = 0;
        long serviceSeconds = 0;        // time the train was meant to be running
        long lateSeconds = 0;           // ...of which delayed or stopped
        long waitPassengerSeconds = 0;  // passengers x seconds in a late train
        List<Passenger> lastOnBoard = new ArrayList<>();

        int carried() {
            return delivered + aboard;
        }

        // Adds another row's numbers into this one (used for the ALL row).
        void add(Row other) {
            delivered += other.delivered;
            aboard += other.aboard;
            serviceSeconds += other.serviceSeconds;
            lateSeconds += other.lateSeconds;
            waitPassengerSeconds += other.waitPassengerSeconds;
        }

        double averageWaitMinutes() {
            if (carried() == 0) {
                return 0;
            }
            return waitPassengerSeconds / 60.0 / carried();
        }

        double onTimePercent() {
            if (serviceSeconds == 0) {
                return 100;
            }
            return 100.0 * (serviceSeconds - lateSeconds) / serviceSeconds;
        }
    }

    // Column x positions.
    private final int colTrain;
    private final int colDelivered;
    private final int colWait;
    private final int colOnTime;

    private final Map<String, Row> rows = new LinkedHashMap<>();
    private long lastSeconds = 0;

    public SideStats(int leftX, int top, int rightX) {
        this.leftX = leftX;
        this.top = top;
        this.rightX = rightX;
        this.colTrain = leftX + 40;
        this.colDelivered = leftX + 150;
        this.colWait = leftX + 300;
        this.colOnTime = leftX + 470;
    }

    @Override
    public int rightEdge() {
        return rightX;
    }

    // Called once per frame by SidePanel, whichever view is showing.
    public void update(Vehicles[] trains, Time time) {
        long passed = time.simSeconds - lastSeconds;
        lastSeconds = time.simSeconds;
        boolean counting = passed > 0 && time.isServiceHours();

        for (Vehicles train : trains) {
            Row row = rows.computeIfAbsent(train.getName(), name -> new Row());

            for (Passenger p : row.lastOnBoard) {
                if (!train.getPassengers().contains(p)) {
                    row.delivered++;
                }
            }
            row.lastOnBoard = new ArrayList<>(train.getPassengers());
            row.aboard = row.lastOnBoard.size();

            if (counting) {
                row.serviceSeconds += passed;
                if (train.isDelayed() || train.isStopped()) {
                    row.lateSeconds += passed;
                    row.waitPassengerSeconds += passed * row.aboard;
                }
            }
        }
    }

    @Override
    public void draw(Graphics g, int panelHeight, Vehicles[] trains, Time time) {
        int w = rightX - leftX;
        int h = panelHeight - top;

        g.setColor(body);
        g.fillRect(leftX, top, w, h);
        g.setColor(Color.black);
        g.drawRect(leftX, top, w, h);

        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.drawString("Statistics", leftX + 20, top + 34);

        int headerY = top + 78;
        g.setColor(headerRow);
        g.fillRect(leftX, headerY - 16, w, 24);
        g.setColor(Color.black);
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.drawString("TRAIN", colTrain, headerY);
        g.drawString("DELIVERED", colDelivered, headerY);
        g.drawString("AVERAGE WAIT", colWait, headerY);
        g.drawString("ON TIME", colOnTime, headerY);

        int rowY = headerY + 40;
        for (Vehicles train : trains) {
            Row row = rows.getOrDefault(train.getName(), new Row());

            g.setColor(SideView.lineColour(train.getName()));
            g.fillRect(colTrain - 16, rowY - 11, 11, 11);
            g.setColor(Color.black);
            g.drawRect(colTrain - 16, rowY - 11, 11, 11);

            g.setFont(new Font("SansSerif", Font.BOLD, 13));
            g.drawString(train.getName(), colTrain, rowY);
            g.setFont(new Font("SansSerif", Font.PLAIN, 13));
            drawNumbers(g, rowY, row);

            g.setColor(gridLine);
            g.drawLine(leftX + 12, rowY + 14, rightX - 12, rowY + 14);
            rowY += 46;
        }

        // Total row: collect() builds one empty Row, then adds every train's Row into it.
        Row total = rows.values().stream().collect(Row::new, Row::add, Row::add);

        g.setColor(Color.black);
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.drawString("ALL", colTrain, rowY);
        drawNumbers(g, rowY, total);

        g.setColor(new Color(60, 60, 60));
        g.setFont(new Font("SansSerif", Font.ITALIC, 12));
        g.drawString("Average wait = minutes a passenger sat in a delayed or stopped train.",
                leftX + 20, rowY + 44);
        g.drawString("On time = share of service time the train was running.",
                leftX + 20, rowY + 62);
    }

    // The three numbers of a row, in their columns (used for each train and for ALL).
    private void drawNumbers(Graphics g, int y, Row row) {
        g.drawString(String.valueOf(row.delivered), colDelivered, y);
        g.drawString(String.format("%.1f min", row.averageWaitMinutes()), colWait, y);
        g.drawString(String.format("%.0f%%", row.onTimePercent()), colOnTime, y);
    }
}
