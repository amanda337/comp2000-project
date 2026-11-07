import java.awt.event.ActionListener;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Random;
import javax.swing.Timer;

public class Time {

    int ticks = 0;  // 5 ticks = 1 real second
    long simSeconds = 0;  // simulated seconds since START

    // the clock starts at this moment (1 Sep 2029, 6am), not the real time
    private static final LocalDateTime START = LocalDateTime.of(2029, 9, 1, 6, 0, 0);

    // trains run from 06:00 AM to 03:00 AM, then all stop for 3 hours until 06:00 AM
    // (the clock keeps going while they are stopped)
    private static final LocalTime SERVICE_START = LocalTime.of(6, 0);
    private static final LocalTime SERVICE_END = LocalTime.of(3, 0);

    // each real second moves the clock 5 simulated minutes
    private static final int STEP_SECONDS = 5 * 60;

    private static final DateTimeFormatter CLOCK_FORMAT =
            DateTimeFormatter.ofPattern("hh:mm:ss a", Locale.ENGLISH);  // e.g. 06:05:07 AM
    private static final DateTimeFormatter SHORT_CLOCK_FORMAT =
            DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);  // e.g. 06:05 AM
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH);  // e.g. Monday, 1 September 2029

    private final Random random = new Random();

    Timer timer;
    private final Speed speed = new Speed();

    public Time(ActionListener onTick) {
        timer = new Timer(Speed.BASE_DELAY, onTick);  // 200 ms at 1x
        updateWeather();
    }

    // weather and fire use the simulated clock, so the speed buttons change them too
    private final Weather weather = new Weather();
    private final Fire fire = new Fire();

    public Weather getWeather() {
        return weather;
    }

    public Fire getFire() {
        return fire;
    }

    // checks if the weather needs to change
    private void updateWeather() {
        weather.update(simSeconds, simNow().getHour());
    }

    // applies weather and fire to the trains, called every repaint
    // the rolls happen once per weather change, fog or a large fire stops a train
    public void applyEvents(Vehicles[] trains) {
        if (weather.takeChange()) {
            weather.rollDelays(trains);
            fire.maybeStart(weather, trains, simSeconds);  // no fire in rain or fog
        }
        fire.endIfOver(simSeconds);
        fire.affectPassingTrains(trains);  // delay a train that reaches a fire
        // every train is stopped when service has ended, as well as in fog or near a large fire
        for (Vehicles train : trains) {
            train.setStopped(!isServiceHours() || weather.stopsTrains() || fire.stops(train));
        }
    }

    public Speed getSpeed() {
        return speed;
    }

    // changes the speed, the clock and trains both use this timer
    public void setSpeed(int multiplier) {
        speed.set(multiplier);
        timer.setDelay(speed.timerDelay());
    }

    // map tint for each key hour: red, green, blue, alpha (0 is clear daylight)
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

    // colour between the two nearest key hours
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

    // a number part of the way from a to b
    private static int blend(int a, int b, double t) {
        return (int) Math.round(a + (b - a) * t);
    }

    // moves the clock one tick, throws when service starts or ends
    public void advance() throws SimulationTimeException {
        ticks = ticks + 1;
        if (ticks % 5 == 0) {
            boolean wasServiceHours = isServiceHours();

            // every second the clock jumps 5 minutes, with a small wobble in the seconds
            int wobble = random.nextInt(31) - 15; // -15..+15 seconds
            simSeconds = simSeconds + STEP_SECONDS + wobble;
            updateWeather();

            boolean nowServiceHours = isServiceHours();
            if (wasServiceHours && !nowServiceHours) {
                throw new SimulationTimeException(
                        "Service ended at " + getClockText() + " - all trains stop until 06:00 AM.");
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

    // true from 06:00 AM until 03:00 AM the next day
    public boolean isServiceHours() {
        LocalTime timeOfDay = simNow().toLocalTime();
        return !timeOfDay.isBefore(SERVICE_START) || timeOfDay.isBefore(SERVICE_END);
    }

    // start plus the seconds that have passed
    private LocalDateTime simNow() {
        return START.plusSeconds(simSeconds);
    }

    // clock text, e.g. 06:05:07 AM
    public String getClockText() {
        return simNow().format(CLOCK_FORMAT);
    }

    // date text, e.g. Monday, 1 September 2029
    public String getDateText() {
        return simNow().format(DATE_FORMAT);
    }

    // clock text some minutes ahead, used by the timetable
    public String clockAt(int minutesAhead) {
        return simNow().plusMinutes(minutesAhead).format(SHORT_CLOCK_FORMAT);
    }

    // starts or pauses the clock, also works at night to skip to the morning
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

    // simulation speed: 1x, 2x or 4x, 2x means the timer fires twice as often
    public static class Speed {

        // delay between ticks at 1x, in milliseconds
        public static final int BASE_DELAY = 200;

        // the speeds the buttons offer
        public static final int[] OPTIONS = {1, 2, 4};

        private int multiplier = 1;

        public int getMultiplier() {
            return multiplier;
        }

        // ignores a value that is not in OPTIONS
        public void set(int newMultiplier) {
            for (int option : OPTIONS) {
                if (option == newMultiplier) {
                    multiplier = newMultiplier;
                }
            }
        }

        // 200, 100 or 50 ms
        public int timerDelay() {
            return BASE_DELAY / multiplier;
        }

        // button text, e.g. 2x
        public static String label(int multiplier) {
            return multiplier + "x";
        }
    }
}
