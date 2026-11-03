import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.util.Arrays;
import java.util.IntSummaryStatistics;

import java.util.Map;
import java.util.TreeMap;

import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;

// The "Train table" view: a wide panel that opens over the map with one row per
// train (line, current stop, next stop, rough ETA), then a footer naming the
// busiest train and a running/delayed status board. Times come from Time.
public class SideTable implements SideView {

    private final int leftX;   // x where the table starts
    private final int top;     // y where the panel starts (below the top bar)
    private final int rightX;  // x of the panel's right edge

    Color body = new Color(220, 220, 220);
    Color headerRow = new Color(190, 190, 190);
    Color gridLine = new Color(170, 170, 170);

    // Column x positions (absolute).
    private final int colTrain;
    private final int colLine;
    private final int colCurrent;
    private final int colTime;
    private final int colNext;
    private final int colEta;

    public SideTable(int leftX, int top, int rightX) {
        this.leftX = leftX;
        this.top = top;
        this.rightX = rightX;

        this.colTrain = leftX + 24;
        this.colLine = leftX + 110;
        this.colCurrent = leftX + 250;
        this.colTime = leftX + 470;
        this.colNext = leftX + 590;
        this.colEta = leftX + 820;
    }

    @Override
    public int rightEdge() {
        return rightX;
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
        g.drawString("Train Time Table", leftX + 20, top + 34);

        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.drawString("Estimated times, about " + MINUTES_BETWEEN_STOPS
                + " min between stops. Clock: " + time.getClockText(), leftX + 20, top + 54);

        // Header row.
        int headerY = top + 78;
        g.setColor(headerRow);
        g.fillRect(leftX, headerY - 16, w, 24);
        g.setColor(Color.black);
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.drawString("TRAIN", colTrain, headerY);
        g.drawString("LINE", colLine, headerY);
        g.drawString("CURRENT STOP", colCurrent, headerY);
        g.drawString("TIME", colTime, headerY);
        g.drawString("NEXT STOP", colNext, headerY);
        g.drawString("ETA", colEta, headerY);

        // One row per train.
        int rowY = headerY + 40;
        int rowStep = 46;
        for (Vehicles train : trains) {
            drawRow(g, rowY, train, time);
            g.setColor(gridLine);
            g.drawLine(leftX + 12, rowY + 14, rightX - 12, rowY + 14);
            rowY += rowStep;
        }

        drawBusiestFooter(g, leftX + 20, rowY + 10, trains);
        drawFooterLine(g, leftX + 20, rowY + 30, statusLine(trains));
        drawFooterLine(g, leftX + 20, rowY + 50, aboardLine(trains));
    }

    private void drawRow(Graphics g, int y, Vehicles train, Time time) {
        // Line-colour swatch next to the train name.
        g.setColor(SideView.lineColour(train.getName()));
        g.fillRect(colTrain - 16, y - 11, 11, 11);
        g.setColor(Color.black);
        g.drawRect(colTrain - 16, y - 11, 11, 11);

        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.drawString(train.getName(), colTrain, y);

        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.drawString(train.route.name, colLine, y);
        g.drawString(train.getCurStop().getName(), colCurrent, y);
        g.drawString(time.clockAt(0), colTime, y);
        g.drawString(SideView.nextStopName(train), colNext, y);
        g.drawString(time.clockAt(MINUTES_BETWEEN_STOPS), colEta, y);
    }

    // Two small stream pipelines: fold the passenger counts with the bounded
    // generic GenericUtil.max (the int counts autobox to Integer), then find
    // the first train with that count. An empty array just draws nothing.
    private void drawBusiestFooter(Graphics g, int x, int y, Vehicles[] trains) {
        int most = Arrays.stream(trains)
                .map(train -> train.getPassengers().size())
                .reduce(0, GenericUtil::max);

        // Arrays.stream(trains)
        //         .filter(train -> train.getPassengers().size() == most)
        //         .findFirst()
        //         .ifPresent(busiest -> {
        //             g.setColor(new Color(60, 60, 60));
        //             g.setFont(new Font("SansSerif", Font.ITALIC, 12));
        //             g.drawString("Busiest right now: " + busiest.getName()
        //                     + " (" + most + " aboard)", x, y + 14);
        //         });
        Arrays.stream(trains)
                .filter(Objects::nonNull)
                .filter(t -> t.getPassengers() != null)
                .collect(Collectors.maxBy(Comparator.comparingInt(t -> t.getPassengers().size())))
                .ifPresent(busiest -> {
                int count = busiest.getPassengers().size();
                g.setColor(new Color(60, 60, 60));
                g.setFont(new Font("SansSerif", Font.ITALIC, 12));
                g.drawString("Busiest right now: " + busiest.getName() 
                        + " (" + count + " aboard)", x, y + 14);
            });
    }

    private static String status(Vehicles train) {
        if (train.isDelayed()) {
            return "Delayed";
        }
        return "Running";
    }

    // Groups the trains by status with a collector. Each status maps to the
    // names of its trains, kept in alphabetical status order by the TreeMap,
    // e.g. "Delayed: T3     Running: T1, T2, T4".
    String statusLine(Vehicles[] trains) {
        Map<String, String> namesByStatus = Arrays.stream(trains)
                .collect(Collectors.groupingBy(
                        SideTable::status,
                        TreeMap::new,
                        Collectors.mapping(Vehicles::getName, Collectors.joining(", "))));

        return namesByStatus.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining("     "));
    }

    // One summarizing collector gives the total and the average in one pass.
    String aboardLine(Vehicles[] trains) {
        IntSummaryStatistics stats = Arrays.stream(trains)
                .collect(Collectors.summarizingInt(train -> train.getPassengers().size()));
        return String.format("Passengers aboard: %d total, %.1f average per train",
                stats.getSum(), stats.getAverage());
    }

    private void drawFooterLine(Graphics g, int x, int y, String text) {
        g.setColor(new Color(60, 60, 60));
        g.setFont(new Font("SansSerif", Font.ITALIC, 12));
        g.drawString(text, x, y + 14);
    }
}
