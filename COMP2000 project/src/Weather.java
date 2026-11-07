import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// one weather for the whole map, a new one starts when the time is up
public class Weather {

    public enum Type {
        SUNNY(new HazardRule("Sunny", 0.00, 0, 240)),
        CLOUDY(new HazardRule("Cloudy", 0.15, 3, 120)),
        RAIN(new HazardRule("Rain", 0.80, 8, 60)),
        FOG(new HazardRule("Foggy", 1.00, 0, 30));

        final HazardRule rule;

        Type(HazardRule rule) {
            this.rule = rule;
        }
    }

    private final Random random = new Random();
    private final Spell spell = new Spell();
    private Type type = Type.SUNNY;
    private boolean changed = false;
    private final List<Type> history = new ArrayList<>();

    // starts it, refuses a time over the limit
    public void begin(Type next, int minutes, long nowSeconds)
            throws DurationLimitException {
        spell.start(next.rule, minutes, nowSeconds);
        type = next;
        history.add(next);
        changed = true;
    }

    // picks a new weather when the current one is over
    public void update(long nowSeconds, int hourOfDay) {
        if (!spell.isOver(nowSeconds)) {
            return;
        }

        Type next = pickFor(hourOfDay);

        try {
            begin(next, next.rule.randomMinutes(random), nowSeconds);
        } catch (DurationLimitException ex) {
            System.out.println(ex.getMessage());
            spell.extend(nowSeconds);
        }
    }

    // picks the next weather from what suits the hour
    private Type pickFor(int hour) {
        List<Type> choices;

        if (hour < 6) {
            choices = Arrays.asList(Type.FOG, Type.FOG, Type.CLOUDY);
        } else if (hour < 9) {
            choices = Arrays.asList(Type.values());
        } else if (hour < 12) {
            choices = Arrays.asList(Type.SUNNY, Type.SUNNY, Type.CLOUDY);
        } else if (hour < 18) {
            choices = Arrays.asList(Type.SUNNY, Type.SUNNY, Type.RAIN);
        } else if (hour < 21) {
            choices = Arrays.asList(Type.values());
        } else {
            choices = Arrays.asList(Type.FOG, Type.CLOUDY);
        }

        // not the same weather again, and never rain straight to fog or fog to rain
        List<Type> different = choices.stream()
                .filter(t -> t != type)
                .filter(t -> !clashes(type, t))
                .collect(Collectors.toList());

        return GenericUtil.randomItem(different, random);
    }

    // rain and fog must not follow each other
    private static boolean clashes(Type from, Type to) {
        return (from == Type.RAIN && to == Type.FOG)
                || (from == Type.FOG && to == Type.RAIN);
    }

    // true once after each change, so delays are rolled once
    public boolean takeChange() {
        boolean result = changed;
        changed = false;
        return result;
    }

    // each train may be delayed, the chance depends on the weather
    public void rollDelays(Vehicles[] trains) {
        type.rule.delay(Arrays.stream(trains), random);
    }

    // fog stops every train
    public boolean stopsTrains() {
        return type == Type.FOG;
    }

    // no fire in rain or fog
    public boolean allowsFire() {
        return type == Type.SUNNY || type == Type.CLOUDY;
    }

    public String getLabel() {
        return type.rule.label;
    }

    // how many of each weather so far, e.g. Cloudy 2, Foggy 1
    public String summary() {
        Map<String, Long> counts = history.stream()
                .collect(Collectors.groupingBy(
                        t -> t.rule.label,
                        TreeMap::new,
                        Collectors.counting()));

        return counts.entrySet().stream()
                .map(entry -> entry.getKey() + " " + entry.getValue())
                .collect(Collectors.joining(", "));
    }

    // numbers for one weather or fire: delay chance, delay steps, longest time
    static class HazardRule {

        private static final int MIN_MINUTES = 15;

        final String label;
        final double delayChance;
        final int delaySteps;
        final int maxMinutes;

        HazardRule(
                String label,
                double delayChance,
                int delaySteps,
                int maxMinutes) {
            this.label = label;
            this.delayChance = delayChance;
            this.delaySteps = delaySteps;
            this.maxMinutes = maxMinutes;
        }

        // refuses a time below 1 or over the limit
        void check(int minutes) throws DurationLimitException {
            if (minutes < 1 || minutes > maxMinutes) {
                throw new DurationLimitException(label, minutes, maxMinutes);
            }
        }

        // random length from 15 minutes up to the limit, in steps of 5
        int randomMinutes(Random random) {
            return MIN_MINUTES
                    + 5 * random.nextInt(
                            (maxMinutes - MIN_MINUTES) / 5 + 1);
        }

        // each train in the stream is delayed with this rule's chance
        void delay(Stream<Vehicles> trains, Random random) {
            trains.filter(train ->
                            random.nextDouble() < delayChance)
                    .forEach(train -> train.delay(delaySteps));
        }
    }

    // remembers when the current weather or fire ends
    static class Spell {

        // the clock jumps about 5 minutes, so end one step early
        private static final long STEP_GUARD_SECONDS = 330;

        private long endSeconds = 0;

        void start(
                HazardRule rule,
                int minutes,
                long nowSeconds) throws DurationLimitException {
            rule.check(minutes);
            endSeconds = nowSeconds + minutes * 60L;
        }

        // back to not running, used when a fire is put out
        void reset() {
            endSeconds = 0;
        }

        // keeps going 15 more minutes
        void extend(long nowSeconds) {
            endSeconds = nowSeconds + 15 * 60L;
        }

        // true when the end time is near, so a spell never passes its limit
        boolean isOver(long nowSeconds) {
            return nowSeconds >= endSeconds - STEP_GUARD_SECONDS;
        }
    }

    // thrown when a weather or fire is asked to last longer than its limit
    static class DurationLimitException extends Exception {

        public DurationLimitException(String what, int minutes, int maxMinutes) {
            super(what + " must last 1 to " + maxMinutes + " minutes, not " + minutes);
        }
    }
}
