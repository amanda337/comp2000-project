import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

// The fixed UI: the top bar (Time|Day box, home and pause buttons) and one of
// two left views, SideTrain (list of trains) or SideTable (timetable).
// SidePanel is the context of a State pattern: `view` is the current state and
// does the drawing and the scrolling. Three chevron tabs on the view's right
// edge switch state; their positions never swap, only colour and width change.
public class SidePanel {

    int topHeight = 70;
    int leftWidth = 320;   // width of the SideTrain panel
    int tableWidth = 960;  // right edge of the SideTable panel

    int topButtonSize = 44; // shared by the pause and home buttons
    int pauseMargin = 20;
    int homeGap = 10;       // gap between the home button and the pause button

    // Tabs are snug by default and a little wider while the mouse hovers.
    int tabShortWidth = 100;
    int tabFullWidth = 135;
    int tabHeight = 40;
    int tabPoint = 14;   // length of the pointed tip
    int tabGap = 10;     // vertical gap between the two tabs

    boolean trainTabHover = false;
    boolean tableTabHover = false;
    boolean statsTabHover = false;
    boolean pauseHover = false;
    boolean homeHover = false;

    // Speed buttons (1x 2x 4x) sit left of the home button.
    int speedButtonWidth = 40;
    int speedHover = -1;   // index in Speed.OPTIONS under the mouse, or -1
    Time time;             // remembered from display() so a click can change its speed

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

    public int getLeftWidth() {
        return leftWidth;
    }

    // True if (x, y) is over the fixed UI rather than the draggable map.
    public boolean isOverPanel(int x, int y, int panelWidth, int panelHeight) {
        return y < topHeight || x < view.rightEdge() || isOverAnyTab(x, y);
    }

    public void display(Graphics g, int panelWidth, int panelHeight, Time time, Vehicles[] trains) {
        this.time = time;
        time.applyEvents(trains); // weather and fire effects on the trains
        sideStats.update(trains, time); // statistics are counted whichever view shows

        // Day-night tint over the map. The bars and panels are drawn after
        // it, so only the map and the trains are tinted.
        g.setColor(time.dayTint());
        g.fillRect(0, 0, panelWidth, panelHeight);

        drawTopBar(g, panelWidth, time);
        view.draw(g, panelHeight, trains, time);
        drawTabs(g);
    }

    private void drawTopBar(Graphics g, int panelWidth, Time time) {
        g.setColor(darkBar);
        g.fillRect(0, 0, panelWidth, topHeight);
        g.setColor(accentLine);
        g.fillRect(0, topHeight - 3, panelWidth, 3);

        // Big box: clock on top, date underneath.
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

        // Weather and fire, stacked next to the clock box.
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

    // Speed buttons

    // Left edge of the i-th speed button: they end one gap before the home button.
    private int speedX(int panelWidth, int i) {
        int count = Speed.OPTIONS.length;
        return homeX(panelWidth) - homeGap - (count - i) * speedButtonWidth;
    }

    // Index of the speed button at (x, y), or -1 if none.
    private int speedIndexAt(int mouseX, int mouseY, int panelWidth) {
        for (int i = 0; i < Speed.OPTIONS.length; i++) {
            if (inRect(mouseX, mouseY, speedX(panelWidth, i), buttonY(), speedButtonWidth, topButtonSize)) {
                return i;
            }
        }
        return -1;
    }

    // Three side-by-side boxes; the one matching the current speed is dark.
    private void drawSpeedButtons(Graphics g, int panelWidth, int current) {
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        for (int i = 0; i < Speed.OPTIONS.length; i++) {
            int x = speedX(panelWidth, i);
            int y = buttonY();
            boolean selected = Speed.OPTIONS[i] == current;

            g.setColor(pick(selected, accentLine, pick(i == speedHover, buttonHoverFill, buttonFill)));
            g.fillRect(x, y, speedButtonWidth, topButtonSize);
            g.setColor(Color.black);
            g.drawRect(x, y, speedButtonWidth, topButtonSize);

            g.setColor(pick(selected, Color.white, Color.black));
            g.drawString(Speed.label(Speed.OPTIONS[i]), x + 10, y + topButtonSize / 2 + 5);
        }
    }

    // Top-bar pause and home buttons

    private int pauseX(int panelWidth) {
        return panelWidth - topButtonSize - pauseMargin;
    }

    private int homeX(int panelWidth) {
        return pauseX(panelWidth) - topButtonSize - homeGap;
    }

    // Both buttons are vertically centred in the bar.
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

    private Color pick(boolean on, Color yes, Color no) {
        if (on) {
            return yes;
        }
        return no;
    }

    // The square every top-bar button starts from.
    private void drawButtonBox(Graphics g, int x, int y, boolean hover) {
        g.setColor(pick(hover, buttonHoverFill, buttonFill));
        g.fillRect(x, y, topButtonSize, topButtonSize);
        g.setColor(Color.black);
        g.drawRect(x, y, topButtonSize, topButtonSize);
    }

    // House icon: a roof triangle over a body, with a door cut-out.
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

    // View-switch tabs

    private int trainTabY() {
        return topHeight + 16;
    }

    private int statsTabY() {
        return tableTabY() + tabHeight + tabGap;
    }

    private int tableTabY() {
        return trainTabY() + tabHeight + tabGap;
    }

    private int tabWidth(boolean hover) {
        if (hover) {
            return tabFullWidth;
        }
        return tabShortWidth;
    }

    private void drawTabs(Graphics g) {
        int x = view.rightEdge();
        drawTab(g, x, trainTabY(), "Train current", view == sideTrain, trainTabHover);
        drawTab(g, x, tableTabY(), "Train table", view == sideTable, tableTabHover);
        drawTab(g, x, statsTabY(), "Statistics", view == sideStats, statsTabHover);
    }

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

    // The hit area is as wide as a tab ever gets, so hovering near a short tab
    // grows it and is easy to click once grown.
    private boolean inTab(int mouseX, int mouseY, int tabY) {
        return inRect(mouseX, mouseY, view.rightEdge(), tabY, tabFullWidth + tabPoint, tabHeight);
    }

    private boolean isOverAnyTab(int mouseX, int mouseY) {
        return inTab(mouseX, mouseY, trainTabY()) || inTab(mouseX, mouseY, tableTabY())
                || inTab(mouseX, mouseY, statsTabY());
    }

    // Each tab always switches to its own view. A click on a speed button is
    // handled here too (Panel already calls this for every click and repaints
    // when it returns true), so Panel needs no change for the speed buttons.
    public boolean handleTabClick(int mouseX, int mouseY, int panelWidth) {
        int speedIndex = speedIndexAt(mouseX, mouseY, panelWidth);
        if (speedIndex >= 0 && time != null) {
            time.setSpeed(Speed.OPTIONS[speedIndex]);
            return true;
        }
        if (inTab(mouseX, mouseY, trainTabY())) {
            view = sideTrain;
            return true;
        }
        if (inTab(mouseX, mouseY, tableTabY())) {
            view = sideTable;
            return true;
        }
        if (inTab(mouseX, mouseY, statsTabY())) {
            view = sideStats;
            return true;
        }
        return false;
    }

    // Pointer and scroll calls go straight to the current view.

    public boolean setPointer(int x, int y) {
        boolean changed = view.setPointer(x, y);

        boolean trainHover = inTab(x, y, trainTabY());
        boolean tableHover = inTab(x, y, tableTabY());
        boolean statsHover = inTab(x, y, statsTabY());
        if (trainHover != trainTabHover || tableHover != tableTabHover || statsHover != statsTabHover) {
            changed = true;
        }
        trainTabHover = trainHover;
        tableTabHover = tableHover;
        statsTabHover = statsHover;
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

    private boolean inRect(int px, int py, int x, int y, int w, int h) {
        return px >= x && px <= x + w && py >= y && py <= y + h;
    }
}
