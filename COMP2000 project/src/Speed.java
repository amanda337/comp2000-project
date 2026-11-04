// How fast the simulation runs: 1x, 2x or 4x. The clock and the trains both
// move on the Swing timer in Time, so one number controls both. 2x simply
// fires the timer twice as often (half the delay between ticks).
public class Speed {

    // Delay between ticks at 1x, in milliseconds (5 ticks per real second).
    public static final int BASE_DELAY = 200;

    // The speeds the buttons offer, left to right.
    public static final int[] OPTIONS = {1, 2, 4};

    private int multiplier = 1;

    public int getMultiplier() {
        return multiplier;
    }

    // Ignores any value that is not one of OPTIONS, so a bad call cannot
    // leave the timer with a zero or negative delay.
    public void set(int newMultiplier) {
        for (int option : OPTIONS) {
            if (option == newMultiplier) {
                multiplier = newMultiplier;
            }
        }
    }

    // Timer delay for the current speed: 200 ms, 100 ms or 50 ms.
    public int timerDelay() {
        return BASE_DELAY / multiplier;
    }

    // Button text, e.g. "2x".
    public static String label(int multiplier) {
        return multiplier + "x";
    }
}
