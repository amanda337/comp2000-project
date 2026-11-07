import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

// the "Train current" view: a card for each train in a scrolling list
public class SideTrain implements SidePanel.SideView {

    private final int width;  // panel width
    private final int top;  // y where the panel starts, below the top bar

    int headerHeight = 56;  // title and clock line
    int cardHeight = 88;  // room for 4 lines of text
    int cardGap = 14;
    int cardSpacing = cardHeight + cardGap;

    int scrollbarWidth = 8;
    int scrollbarWidthHot = 12;
    int hoverMargin = 28;  // how close the mouse must be to show the bar

    int scrollY = 0;  // pixels scrolled down
    int contentHeight = 0;  // height of all the cards, set when drawing
    int viewHeight = 0;  // height of the visible list, set when drawing

    boolean pointerNearBar = false;
    boolean thumbHover = false;
    boolean thumbDragging = false;

    Color panelBody = new Color(225, 225, 225);  // light grey
    Color cardBackground = new Color(248, 248, 248);
    Color cardBorder = new Color(90, 90, 90);
    Color scrollTrack = new Color(80, 80, 80);
    Color thumbNormal = new Color(150, 150, 150);
    Color thumbHoverColour = new Color(195, 195, 195);
    Color thumbDragColour = new Color(230, 230, 230);

    public SideTrain(int width, int top) {
        this.width = width;
        this.top = top;
    }

    @Override
    public int rightEdge() {
        return width;
    }

    // title and clock, then the cards, then the scrollbar
    @Override
    public void draw(Graphics g, int panelHeight, Vehicles[] trains, Time time) {
        int fullHeight = panelHeight - top;

        g.setColor(panelBody);
        g.fillRect(0, top, width, fullHeight);

        g.setColor(Color.black);
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.drawString("Train current", 16, top + 26);
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.drawString("Clock: " + time.getClockText(), 16, top + 44);

        int listTop = top + headerHeight;
        contentHeight = trains.length * cardSpacing + 8;
        viewHeight = fullHeight - headerHeight;
        scrollY = clampScroll(scrollY);

        // clip so scrolled cards stay inside the list
        Graphics clipped = g.create();
        clipped.clipRect(0, listTop, width, viewHeight);
        int cardY = listTop + 8 - scrollY;
        for (Vehicles train : trains) {
            drawTrainCard(clipped, cardY, train, time);
            cardY += cardSpacing;
        }
        clipped.dispose();

        drawScrollbar(g, listTop);
    }

    // scrolling

    // furthest the list can scroll
    private int maxScroll() {
        return Math.max(0, contentHeight - viewHeight);
    }

    // keeps the scroll between 0 and the furthest
    private int clampScroll(int value) {
        if (value < 0) return 0;
        if (value > maxScroll()) return maxScroll();
        return value;
    }

    // thumb height matches how much of the list is visible
    private int thumbHeight() {
        if (contentHeight <= 0) return 30;
        int h = (int) ((long) viewHeight * viewHeight / contentHeight);
        return Math.max(30, Math.min(h, viewHeight));
    }

    // thumb position follows the scroll
    private int thumbY(int listTop) {
        if (maxScroll() == 0) return listTop;
        return listTop + (int) ((long) (viewHeight - thumbHeight()) * scrollY / maxScroll());
    }

    // scrolls the list, used by the mouse wheel
    @Override
    public void scrollBy(int amount) {
        scrollY = clampScroll(scrollY + amount);
    }

    // true if something changed, so the caller redraws
    @Override
    public boolean setPointer(int x, int y) {
        int listTop = top + headerHeight;
        boolean near = x >= 0 && x <= width + hoverMargin
                && y >= listTop && y <= listTop + viewHeight;
        int tX = width - currentWidth();
        int tY = thumbY(listTop);
        boolean overThumb = near && x >= tX - hoverMargin && y >= tY && y <= tY + thumbHeight();

        boolean changed = (near != pointerNearBar) || (overThumb != thumbHover);
        pointerNearBar = near;
        thumbHover = overThumb;
        return changed;
    }

    @Override
    public void setThumbDragging(boolean dragging) {
        thumbDragging = dragging;
    }

    // true when the mouse is on the scrollbar
    @Override
    public boolean isOverScrollbar(int x, int y) {
        if (maxScroll() == 0) return false;
        int listTop = top + headerHeight;
        return x >= width - scrollbarWidthHot - 4 && x <= width + 4
                && y >= listTop && y <= listTop + viewHeight;
    }

    // moves the list while the thumb is dragged
    @Override
    public void dragScrollTo(int mouseY) {
        int travel = viewHeight - thumbHeight();
        if (travel <= 0) {
            scrollY = 0;
            return;
        }
        int listTop = top + headerHeight;
        int rel = mouseY - listTop - thumbHeight() / 2;
        scrollY = clampScroll((int) ((long) rel * maxScroll() / travel));
    }

    // the bar only shows when the cards do not all fit and the mouse is near
    private boolean scrollbarShowing() {
        return maxScroll() > 0 && (pointerNearBar || thumbHover || thumbDragging);
    }

    // the bar is wider while the thumb is hovered or dragged
    private int currentWidth() {
        if (thumbHover || thumbDragging) {
            return scrollbarWidthHot;
        }
        return scrollbarWidth;
    }

    // track and thumb
    private void drawScrollbar(Graphics g, int listTop) {
        if (!scrollbarShowing()) return;
        int barW = currentWidth();
        int trackX = width - barW;

        g.setColor(scrollTrack);
        g.fillRect(trackX, listTop, barW, viewHeight);

        int thumbH = thumbHeight();
        int ty = thumbY(listTop);
        if (thumbDragging) {
            g.setColor(thumbDragColour);
        } else if (thumbHover) {
            g.setColor(thumbHoverColour);
        } else {
            g.setColor(thumbNormal);
        }
        g.fillRect(trackX, ty, barW, thumbH);
        g.setColor(Color.black);
        g.drawRect(trackX, ty, barW - 1, thumbH - 1);
    }

    // one card for a train, same style as the table rows

    private void drawTrainCard(Graphics g, int cardY, Vehicles train, Time time) {
        int cardX = 10;
        int cardWidth = width - 20;

        if (train.isDelayed()) {
            HazardStripe.draw(g, cardX, cardY, cardWidth, cardHeight, 6);
        }

        g.setColor(cardBackground);
        g.fillRoundRect(cardX, cardY, cardWidth, cardHeight, 10, 10);
        g.setColor(cardBorder);
        g.drawRoundRect(cardX, cardY, cardWidth, cardHeight, 10, 10);

        // stripe in the line colour
        g.setColor(SidePanel.SideView.lineColour(train.getName()));
        g.fillRoundRect(cardX, cardY, 8, cardHeight, 10, 10);
        g.fillRect(cardX + 4, cardY, 4, cardHeight);  // square off the right edge

        // label and value pairs
        GenericUtil.Pair<String, String> current = new GenericUtil.Pair<>("Current", train.getCurStop().getName());
        GenericUtil.Pair<String, String> upcoming = new GenericUtil.Pair<>("Next", SidePanel.SideView.nextStopName(train));

        String title = train.getName() + " - " + train.route.name;
        if (train.isStopped()) {
            HazardStripe.StopSign.draw(g, cardX + cardWidth - 26, cardY + 24, 32);  // drawn first, it changes the colour
            g.setColor(new Color(200, 30, 30));
            title = title + " (STOPPED)";
        } else if (train.isDelayed()) {
            g.setColor(new Color(200, 30, 30));
            title = title + " (DELAYED)";
        } else {
            g.setColor(Color.black);
        }
        int textX = cardX + 18;
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.drawString(title, textX, cardY + 20);

        g.setColor(Color.black);
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.drawString(current.getFirst() + ": " + current.getSecond()
                + "   Time: " + time.clockAt(0), textX, cardY + 40);
        g.drawString(upcoming.getFirst() + ": " + upcoming.getSecond()
                + "   ETA: " + time.clockAt(MINUTES_BETWEEN_STOPS), textX, cardY + 58);
        g.drawString("Passengers on board: " + train.getPassengers().size(), textX, cardY + 76);
    }
}
