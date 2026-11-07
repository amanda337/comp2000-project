import java.awt.event.ActionListener;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Random;
import javax.swing.Timer;

public class Time {

    int ticks = 0;          // 1 real second = 5 ticks (still drives the train step)
    long simSeconds = 0;    // simulated seconds elapsed since START, grows forever

    // The simulated clock counts up from this fixed moment (1 Sep 2029, 6am).
    // It is NOT the real wall-clock time; it only moves while the sim runs.
    private static final LocalDateTime START = LocalDateTime.of(2029, 9, 1, 6, 0, 0);

    // Trains run 06:00 AM through to midnight each day; the clock itself
    // keeps going overnight so service resumes on its own at 06:00 AM.
    private static final LocalTime SERVICE_START = LocalTime.of(6, 0);

    // Base speed: each real second pushes the clock 5 simulated minutes forward,
    // so it steps 06:00:00 -> 06:05:00 -> 06:10:00 ... like the old MM:SS clock did.
    private static final int STEP_SECONDS = 5 * 60;

    private static final DateTimeFormatter CLOCK_FORMAT =
            DateTimeFormatter.ofPattern("hh:mm:ss a", Locale.ENGLISH); // e.g. 06:05:07 AM
    private static final DateTimeFormatter SHORT_CLOCK_FORMAT =
            DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH); // e.g. 06:05 AM
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH); // Monday, 1 September 2029

    private final Random random = new Random();

    Timer timer;
    private final Speed speed = new Speed();

    public Time(ActionListener onTick) {
        timer = new Timer(Speed.BASE_DELAY, onTick); // 200ms at 1x, 5 times a second
        updateWeather();
    }

    // Weather and fire follow the simulated clock, so the 1x/2x/4x speed
    // changes them too. Each weather spell and fire has its own time limit.
    private final Weather weather = new Weather();
    private final Fire fire = new Fire();

    public Weather getWeather() {
        return weather;
    }

    public Fire getFire() {
        return fire;
    }

    private void updateWeather() {
        weather.update(simSeconds, simNow().getHour());
    }

    // Applies weather and fire to the trains. Called every repaint, but the
    // rolls only happen once per weather change. A fog or a large fire near a
    // train stops it (stop sign); it moves again when that clears.
    public void applyEvents(Vehicles[] trains) {
        if (weather.takeChange()) {
            weather.rollDelays(trains);
            fire.maybeStart(weather, trains, simSeconds); // no fire in rain or fog
        }
        fire.endIfOver(simSeconds);
        fire.affectPassingTrains(trains); // delay a train that reaches a burning station
        for (Vehicles train : trains) {
            train.setStopped(weather.stopsTrains() || fire.stops(train));
        }
    }

    public Speed getSpeed() {
        return speed;
    }

    // Changes the speed (1, 2 or 4). Works while running or paused: setDelay
    // just changes the gap between ticks, so the clock and the trains, which
    // both move on this timer, speed up together.
    public void setSpeed(int multiplier) {
        speed.set(multiplier);
        timer.setDelay(speed.timerDelay());
    }

    // Sky tint for the time of day, as {red, green, blue, alpha}. Each row of
    // TINTS is a key hour; the colour fades smoothly between neighbouring rows.
    // Alpha 0 is clear daylight, higher alpha is darker night.
    private static final int[][] TINTS = {
        {0, 10, 20, 80, 120},   // midnight: deep blue
        {5, 10, 20, 80, 120},
        {6, 255, 150, 60, 50},  // dawn: warm orange
        {8, 255, 220, 150, 0},  // morning: clear
        {17, 255, 220, 150, 0},
        {18, 255, 120, 40, 60}, // dusk: orange
        {20, 30, 30, 100, 100}, // evening: blue
        {24, 10, 20, 80, 120}   // back to midnight
    };

    public java.awt.Color dayTint() {
        LocalTime now = simNow().toLocalTime();
        double hour = now.getHour() + now.getMinute() / 60.0;
        for (int i = 1; i < TINTS.length; i++) {
            if (hour <= TINTS[i][0]) {
                int[] from = TINTS[i - 1];
                int[] to = TINTS[i];
                double t = (hour - from[0]) / (to[0] - from[0]);
                return new java.awt.Color(
                        blend(from[1], to[1], t), blend(from[2], to[2], t),
                        blend(from[3], to[3], t), blend(from[4], to[4], t));
            }
        }
        return new java.awt.Color(0, 0, 0, 0);
    }

    private static int blend(int a, int b, double t) {
        return (int) Math.round(a + (b - a) * t);
    }

    // Advances the simulated clock by one tick. Throws when the clock crosses
    // into or out of service hours (06:00 AM - midnight), so the tick loop can
    // start or stop moving the trains at exactly the right moment.
    public void advance() throws SimulationTimeException {
        ticks = ticks + 1;
        if (ticks % 5 == 0) {
            boolean wasServiceHours = isServiceHours();

            // One real second finished: jump the clock ~5 minutes, plus a small
            // random wobble so the seconds field looks realistic instead of :00.
            int wobble = random.nextInt(31) - 15; // -15..+15 seconds
            simSeconds = simSeconds + STEP_SECONDS + wobble;
            updateWeather();

            boolean nowServiceHours = isServiceHours();
            if (wasServiceHours && !nowServiceHours) {
                throw new SimulationTimeException(
                        "Service ended at " + getClockText() + " - trains stop until 06:00 AM.");
            }
            if (!wasServiceHours && nowServiceHours) {
                throw new SimulationTimeException(
                        "Service resumed at " + getClockText() + " - trains are running again.");
            }
        }
    }

    public boolean isOnSecond() {
        return ticks % 5 == 0;
    }

    // True between 06:00 AM and 12:00 PM on any simulated day.
    public boolean isServiceHours() {
        LocalTime timeOfDay = simNow().toLocalTime();
        return !timeOfDay.isBefore(SERVICE_START);
    }

    // Simulated date-time = start + the seconds accumulated in advance().
    private LocalDateTime simNow() {
        return START.plusSeconds(simSeconds);
    }

    // 12-hour clock with AM/PM, e.g. "06:05:07 AM".
    public String getClockText() {
        return simNow().format(CLOCK_FORMAT);
    }

    // Weekday, day, month and year, e.g. "Monday, 1 September 2029".
    public String getDateText() {
        return simNow().format(DATE_FORMAT);
    }

    // The simulated clock a given number of minutes ahead, e.g. "06:08 AM".
    // Used by the timetable to estimate stop times.
    public String clockAt(int minutesAhead) {
        return simNow().plusMinutes(minutesAhead).format(SHORT_CLOCK_FORMAT);
    }

    // Space (or clicking the button) toggles between running and paused.
    // Works outside service hours too, so the clock can be fast-forwarded
    // through the night to the next morning.
    public void toggle() {
        if (timer.isRunning()) {
            timer.stop();
        } else {
            timer.start();
        }
    }

    public boolean isRunning() {
        return timer.isRunning();
    }
}

// How fast the simulation runs: 1x, 2x or 4x. The clock and the trains both
// move on the Swing timer in Time, so one number controls both. 2x simply
// fires the timer twice as often (half the delay between ticks).
class Speed {

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
