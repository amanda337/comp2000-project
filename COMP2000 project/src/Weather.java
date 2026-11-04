import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.stream.Collectors;

// The weather for the whole map. Each spell lasts a random time up to a limit
// for that weather (sunny 4 h, cloudy 2 h, rain 1 h, fog 30 min), then a
// different weather follows, so no weather can run past its limit. The next
// weather is picked to suit the time of day, e.g. fog is likely at night.
// It follows the simulation clock, so the 1x/2x/4x speed speeds it up too.
public class Weather {

    // delayChance: chance a train is delayed when this weather begins (rain and
    // cloud only delay). Fog is different: it stops every train while it lasts.
    public enum Type {
        SUNNY("Sunny", 0.00, 0, 240),
        CLOUDY("Cloudy", 0.15, 3, 120),
        RAIN("Rain", 0.80, 8, 60),
        FOG("Foggy", 1.00, 0, 30);

        final String label;
        final double delayChance;
        final int delaySteps;
        final int maxMinutes;

        Type(String label, double delayChance, int delaySteps, int maxMinutes) {
            this.label = label;
            this.delayChance = delayChance;
            this.delaySteps = delaySteps;
            this.maxMinutes = maxMinutes;
        }
    }

    private static final int MIN_MINUTES = 15;

    // The clock jumps about 5 minutes per step (plus up to 15 seconds of
    // wobble). A spell ends at the first step within this many seconds of its
    // end time, so the jump can never carry it past its limit.
    public static final long STEP_GUARD_SECONDS = 330;

    private final Random random = new Random();
    private Type type = Type.SUNNY;
    private long endSeconds = 0;       // simulated second when the current spell ends
    private boolean changed = false;
    private final List<Type> history = new ArrayList<>();

    // A random length from 15 minutes up to the limit, in 5-minute steps.
    public static int randomMinutes(Random random, int maxMinutes) {
        return MIN_MINUTES + 5 * random.nextInt((maxMinutes - MIN_MINUTES) / 5 + 1);
    }

    // Starts a spell. The limit is checked here, so a spell longer than its
    // weather allows is refused before anything changes.
    public void begin(Type next, int minutes, long nowSeconds) throws DurationLimitException {
        if (minutes < 1 || minutes > next.maxMinutes) {
            throw new DurationLimitException(next.label, minutes, next.maxMinutes);
        }
        type = next;
        endSeconds = nowSeconds + minutes * 60L;
        history.add(next);
        changed = true;
    }

    // Called by Time as the clock moves. Nothing happens until the spell ends.
    public void update(long nowSeconds, int hourOfDay) {
        if (nowSeconds < endSeconds - STEP_GUARD_SECONDS) {
            return;
        }
        Type next = pickFor(hourOfDay);
        try {
            begin(next, randomMinutes(random, next.maxMinutes), nowSeconds);
        } catch (DurationLimitException ex) {
            // Cannot happen with randomMinutes, but if it ever does, keep the
            // current weather a little longer instead of crashing the clock.
            System.out.println(ex.getMessage());
            endSeconds = nowSeconds + MIN_MINUTES * 60L;
        }
    }

    // Time-of-day tendencies. Listing a type twice makes it twice as likely.
    // A spell never repeats the weather it follows, so no weather can chain
    // past its limit.
    private Type pickFor(int hour) {
        List<Type> choices;
        if (hour < 6) {
            choices = Arrays.asList(Type.FOG, Type.FOG, Type.CLOUDY);
        } else if (hour < 9) {
            choices = Arrays.asList(Type.values());              // morning: anything
        } else if (hour < 12) {
            choices = Arrays.asList(Type.SUNNY, Type.SUNNY, Type.CLOUDY);
        } else if (hour < 18) {
            choices = Arrays.asList(Type.SUNNY, Type.SUNNY, Type.RAIN);
        } else if (hour < 21) {
            choices = Arrays.asList(Type.values());              // evening: anything
        } else {
            choices = Arrays.asList(Type.FOG, Type.CLOUDY);
        }

        List<Type> different = choices.stream()
                .filter(t -> t != type)
                .filter(t -> !clashes(type, t))
                .collect(Collectors.toList());
        return different.get(random.nextInt(different.size()));
    }

    // Rain and fog never follow each other directly. After one of them the
    // weather must be sunny or cloudy first, in either order.
    private static boolean clashes(Type from, Type to) {
        return (from == Type.RAIN && to == Type.FOG)
                || (from == Type.FOG && to == Type.RAIN);
    }

    // True once after each weather change, so the effects are rolled once.
    public boolean takeChange() {
        boolean result = changed;
        changed = false;
        return result;
    }

    // Rolls the delay chance for each train. A delayed train shows the red and
    // yellow stripe, the same as an accident delay.
    public void rollDelays(Vehicles[] trains) {
        Arrays.stream(trains)
                .filter(train -> random.nextDouble() < type.delayChance)
                .forEach(train -> train.delay(type.delaySteps));
    }

    // Fog is a complete stop: no train moves until it lifts.
    public boolean stopsTrains() {
        return type == Type.FOG;
    }

    // No fire can start, or keep burning, in rain or fog.
    public boolean allowsFire() {
        return type == Type.SUNNY || type == Type.CLOUDY;
    }

    public String getLabel() {
        return type.label;
    }

    // How many spells of each weather so far, e.g. "Cloudy 1, Foggy 2".
    // groupingBy + counting, with a TreeMap so the names stay in order.
    public String summary() {
        Map<String, Long> counts = history.stream()
                .collect(Collectors.groupingBy(t -> t.label, TreeMap::new, Collectors.counting()));
        return counts.entrySet().stream()
                .map(entry -> entry.getKey() + " " + entry.getValue())
                .collect(Collectors.joining(", "));
    }
}
