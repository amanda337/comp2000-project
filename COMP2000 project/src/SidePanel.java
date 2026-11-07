import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.util.List;
import java.util.Optional;
import java.util.Map;

// the fixed part of the screen: top bar (clock, speed, home, pause) and one of three left views.
// view is the current state (State pattern) and does the drawing and scrolling,
// the three tabs next to it switch the view
public class SidePanel {

    int topHeight = 70;
    int leftWidth = 320;  // width of the train list
    int tableWidth = 960;  // right edge of the table

    int topButtonSize = 44;  // pause and home buttons
    int pauseMargin = 20;
    int homeGap = 10;  // gap between the home and pause buttons

    // tabs are narrower until the mouse is over them
    int tabShortWidth = 100;
    int tabFullWidth = 135;
    int tabHeight = 40;
    int tabPoint = 14;  // length of the pointed end
    int tabGap = 10;  // gap between tabs

    boolean pauseHover = false;
    boolean homeHover = false;

    // speed buttons (1x 2x 4x) sit left of the home button
    int speedButtonWidth = 40;
    int speedHover = -1;  // index in Speed.OPTIONS under the mouse, or -1
    Time time;  // kept from display() so a click can change the speed

    Color darkBar = new Color(45, 45, 45);
    Color accentLine = new Color(30, 144, 255);
    Color boxFill = new Color(245, 245, 245);
    Color buttonFill = new Color(245, 245, 245);
    Color buttonHoverFill = new Color(150, 200, 255);
    Color tabActiveFill = new Color(225, 225, 225);
    Color tabInactiveFill = new Color(90, 90, 90);

    SideTrain sideTrain = new SideTrain(leftWidth, topHeight);
    SideTable sideTable = new SideTable(0, topHeight, tableWidth);
    SideStats sideStats = new SideStats(0, topHeight, 640);
    SideView view = sideTrain;

    // one tab: its view, its place in the column (0 is the top) and if the mouse is over it.
    // a new view only needs one more line in the tabs list
    private static class Tab {
        final int row;
        final String label;
        final SideView view;
        boolean hover = false;

        Tab(int row, String label, SideView view) {
            this.row = row;
            this.label = label;
            this.view = view;
        }

        // true if the hover state changed
        boolean setHover(boolean now) {
            boolean changed = now != hover;
            hover = now;
            return changed;
        }
    }

    private final List<Tab> tabs = List.of(
            new Tab(0, "Train current", sideTrain),
            new Tab(1, "Train table", sideTable),
            new Tab(2, "Statistics", sideStats));

    public int getLeftWidth() {
        return leftWidth;
    }

    // true if the point is on the fixed part, not on the map
    public boolean isOverPanel(int x, int y, int panelWidth, int panelHeight) {
        return y < topHeight || x < view.rightEdge() || isOverAnyTab(x, y);
    }

    // draws the tint, top bar, current view and tabs
    public void display(Graphics g, int panelWidth, int panelHeight, Time time, Vehicles[] trains) {
        this.time = time;
        time.applyEvents(trains);  // weather and fire effects on the trains
        sideStats.update(trains, time);  // statistics count whichever view shows

        // day and night tint over the map, the panels are drawn after it
        g.setColor(time.dayTint());
        g.fillRect(0, 0, panelWidth, panelHeight);

        drawTopBar(g, panelWidth, time);
        view.draw(g, panelHeight, trains, time);
        drawTabs(g);
    }

    // clock box, weather and fire text, speed, home and pause buttons
    private void drawTopBar(Graphics g, int panelWidth, Time time) {
        g.setColor(darkBar);
        g.fillRect(0, 0, panelWidth, topHeight);
        g.setColor(accentLine);
        g.fillRect(0, topHeight - 3, panelWidth, 3);

        // clock box: time on top, date below
        int boxX = 12;
        int boxY = 8;
        int boxW = 300;
        int boxH = topHeight - 16;
        g.setColor(boxFill);
        g.fillRect(boxX, boxY, boxW, boxH);
        g.setColor(Color.black);
        g.drawRect(boxX, boxY, boxW, boxH);

        g.setFont(new Font("Times New Roman", Font.BOLD, 24));
        g.drawString(time.getClockText(), boxX + 14, boxY + 26);
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.drawString(time.getDateText(), boxX + 14, boxY + 45);

        // weather and fire text next to the clock box
        int infoX = boxX + boxW + 24;
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(Color.white);
        g.drawString("Weather: " + time.getWeather().getLabel(), infoX, boxY + 20);
        g.setColor(new Color(255, 170, 60));
        g.drawString(time.getFire().getText(), infoX, boxY + 42);

        if (!time.isServiceHours()) {
            g.setColor(new Color(255, 120, 120));
            g.setFont(new Font("SansSerif", Font.BOLD, 16));
            g.drawString("SERVICE ENDED - RESUMES 6:00 AM", infoX + 290, boxY + 34);
        }

        drawSpeedButtons(g, panelWidth, time.getSpeed().getMultiplier());
        drawHomeButton(g, panelWidth);
        drawPauseButton(g, panelWidth, time.isRunning());
    }

    // speed buttons

    // left edge of each speed button, they end just before the home button
    private int speedX(int panelWidth, int i) {
        int count = Time.Speed.OPTIONS.length;
        return homeX(panelWidth) - homeGap - (count - i) * speedButtonWidth;
    }

    // index of the speed button at this point, -1 if none
    private int speedIndexAt(int mouseX, int mouseY, int panelWidth) {
        for (int i = 0; i < Time.Speed.OPTIONS.length; i++) {
            if (inRect(mouseX, mouseY, speedX(panelWidth, i), buttonY(), speedButtonWidth, topButtonSize)) {
                return i;
            }
        }
        return -1;
    }

    // three boxes side by side, the current speed is blue
    private void drawSpeedButtons(Graphics g, int panelWidth, int current) {
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        for (int i = 0; i < Time.Speed.OPTIONS.length; i++) {
            int x = speedX(panelWidth, i);
            int y = buttonY();
            boolean selected = Time.Speed.OPTIONS[i] == current;

            g.setColor(pick(selected, accentLine, pick(i == speedHover, buttonHoverFill, buttonFill)));
            g.fillRect(x, y, speedButtonWidth, topButtonSize);
            g.setColor(Color.black);
            g.drawRect(x, y, speedButtonWidth, topButtonSize);

            g.setColor(pick(selected, Color.white, Color.black));
            g.drawString(Time.Speed.label(Time.Speed.OPTIONS[i]), x + 10, y + topButtonSize / 2 + 5);
        }
    }

    // pause and home buttons

    private int pauseX(int panelWidth) {
        return panelWidth - topButtonSize - pauseMargin;
    }

    private int homeX(int panelWidth) {
        return pauseX(panelWidth) - topButtonSize - homeGap;
    }

    // both buttons are centred in the bar
    private int buttonY() {
        return (topHeight - topButtonSize) / 2;
    }

    public boolean isPauseClicked(int mouseX, int mouseY, int panelWidth) {
        return inRect(mouseX, mouseY, pauseX(panelWidth), buttonY(), topButtonSize, topButtonSize);
    }

    public boolean isHomeClicked(int mouseX, int mouseY, int panelWidth) {
        return inRect(mouseX, mouseY, homeX(panelWidth), buttonY(), topButtonSize, topButtonSize);
    }

    public boolean setButtonHover(int mouseX, int mouseY, int panelWidth) {
        boolean nowPauseHover = isPauseClicked(mouseX, mouseY, panelWidth);
        boolean nowHomeHover = isHomeClicked(mouseX, mouseY, panelWidth);
        int nowSpeedHover = speedIndexAt(mouseX, mouseY, panelWidth);
        boolean changed = (nowPauseHover != pauseHover) || (nowHomeHover != homeHover)
                || (nowSpeedHover != speedHover);
        pauseHover = nowPauseHover;
        homeHover = nowHomeHover;
        speedHover = nowSpeedHover;
        return changed;
    }

    // one of two colours
    private Color pick(boolean on, Color yes, Color no) {
        if (on) {
            return yes;
        }
        return no;
    }

    // the square box every button starts from
    private void drawButtonBox(Graphics g, int x, int y, boolean hover) {
        g.setColor(pick(hover, buttonHoverFill, buttonFill));
        g.fillRect(x, y, topButtonSize, topButtonSize);
        g.setColor(Color.black);
        g.drawRect(x, y, topButtonSize, topButtonSize);
    }

    // house icon: roof, body and door
    private void drawHomeButton(Graphics g, int panelWidth) {
        int x = homeX(panelWidth);
        int y = buttonY();
        drawButtonBox(g, x, y, homeHover);

        int[] roofX = {x + 6, x + topButtonSize / 2, x + topButtonSize - 6};
        int[] roofY = {y + 22, y + 8, y + 22};
        g.setColor(new Color(60, 60, 60));
        g.fillPolygon(roofX, roofY, 3);
        g.fillRect(x + 11, y + 22, topButtonSize - 22, 14);

        g.setColor(pick(homeHover, buttonHoverFill, buttonFill));
        g.fillRect(x + topButtonSize / 2 - 3, y + 27, 6, 9); // door
    }

    // pause bars while running, play triangle while paused
    private void drawPauseButton(Graphics g, int panelWidth, boolean running) {
        int x = pauseX(panelWidth);
        int y = buttonY();
        drawButtonBox(g, x, y, pauseHover);

        int barW = topButtonSize / 6;
        if (running) {
            g.fillRect(x + topButtonSize / 5, y + 10, barW, topButtonSize - 20);
            g.fillRect(x + topButtonSize - topButtonSize / 5 - barW, y + 10, barW, topButtonSize - 20);
        } else {
            int[] xs = {x + 16, x + 16, x + topButtonSize - 14};
            int[] ys = {y + 10, y + topButtonSize - 10, y + topButtonSize / 2};
            g.fillPolygon(xs, ys, 3);
        }
    }

    // tabs that switch the view

    private int tabY(Tab tab) {
        return topHeight + 16 + tab.row * (tabHeight + tabGap);
    }

    private int tabWidth(boolean hover) {
        if (hover) {
            return tabFullWidth;
        }
        return tabShortWidth;
    }

    // draws every tab next to the current view
    private void drawTabs(Graphics g) {
        int x = view.rightEdge();
        tabs.forEach(tab -> drawTab(g, x, tabY(tab), tab.label, view == tab.view, tab.hover));
    }

    // pointed tab, light when its view is the current one
    private void drawTab(Graphics g, int x, int y, String label, boolean active, boolean hover) {
        int w = tabWidth(hover);
        int[] xs = {x, x + w, x + w + tabPoint, x + w, x};
        int[] ys = {y, y, y + tabHeight / 2, y + tabHeight, y + tabHeight};

        g.setColor(pick(active, tabActiveFill, tabInactiveFill));
        g.fillPolygon(xs, ys, 5);
        g.setColor(Color.black);
        g.drawPolygon(xs, ys, 5);

        g.setColor(pick(active, Color.black, Color.white));
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.drawString(label, x + 10, y + tabHeight / 2 + 5);
    }

    // the click area is as wide as a tab gets, so a narrow tab is still easy to hit
    private boolean inTab(int mouseX, int mouseY, int tabY) {
        return inRect(mouseX, mouseY, view.rightEdge(), tabY, tabFullWidth + tabPoint, tabHeight);
    }

    // the tab under the mouse, empty if none
    private Optional<Tab> tabAt(int mouseX, int mouseY) {
        return tabs.stream()
                .filter(tab -> inTab(mouseX, mouseY, tabY(tab)))
                .findFirst();
    }

    private boolean isOverAnyTab(int mouseX, int mouseY) {
        return tabAt(mouseX, mouseY).isPresent();
    }

    // a click on a tab switches the view, a click on a speed button changes the speed
    // (Panel already calls this on every click, so Panel needs no extra code)
    public boolean handleTabClick(int mouseX, int mouseY, int panelWidth) {
        int speedIndex = speedIndexAt(mouseX, mouseY, panelWidth);
        if (speedIndex >= 0 && time != null) {
            time.setSpeed(Time.Speed.OPTIONS[speedIndex]);
            return true;
        }
        Optional<Tab> clicked = tabAt(mouseX, mouseY);
        clicked.ifPresent(tab -> view = tab.view);
        return clicked.isPresent();
    }

    // pointer and scroll calls go to the current view

    // updates hover states, true if anything changed
    public boolean setPointer(int x, int y) {
        boolean changed = view.setPointer(x, y);

        for (Tab tab : tabs) {
            if (tab.setHover(inTab(x, y, tabY(tab)))) {
                changed = true;
            }
        }
        return changed;
    }

    public boolean isOverScrollbar(int x, int y) {
        return view.isOverScrollbar(x, y);
    }

    public void dragScrollTo(int y) {
        view.dragScrollTo(y);
    }

    public void scrollBy(int amount) {
        view.scrollBy(amount);
    }

    public void setThumbDragging(boolean dragging) {
        view.setThumbDragging(dragging);
    }

    // true if the point is inside the rectangle
    private boolean inRect(int px, int py, int x, int y, int w, int h) {
        return px >= x && px <= x + w && py >= y && py <= y + h;
    }

    // the left views (State pattern), SidePanel passes draw and mouse calls to the current one
    public interface SideView {

        int MINUTES_BETWEEN_STOPS = 3;

        Map<String, Color> LINE_COLOURS = Map.of(
                "T1", new Color(255, 0, 0),  // red line
                "T2", new Color(0, 0, 255),  // blue line
                "T3", new Color(191, 0, 255),  // purple line
                "T4", new Color(0, 160, 0));  // green line

        void draw(Graphics g, int panelHeight, Vehicles[] trains, Time time);

        // right edge of the view, the tabs sit just outside it
        int rightEdge();

        // only the train list scrolls, the other views do nothing here
        default boolean setPointer(int x, int y) {
            return false;
        }

        default boolean isOverScrollbar(int x, int y) {
            return false;
        }

        default void dragScrollTo(int y) {
        }

        default void scrollBy(int amount) {
        }

        default void setThumbDragging(boolean dragging) {
        }

        // the colour a train shares with its line on the map
        static Color lineColour(String trainName) {
            return LINE_COLOURS.getOrDefault(trainName, Color.gray);
        }

        // "-" when there is no next stop
        static String nextStopName(Vehicles train) {
            return Optional.ofNullable(train.getNextStop()).map(Stops::getName).orElse("-");
        }
    }
}
