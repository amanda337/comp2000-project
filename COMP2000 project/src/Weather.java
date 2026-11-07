
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// weather and fire share this file with HazardRule and Spell (shared helpers)

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
}

// a fire that starts at one station and can spread, never in rain or fog
class Fire {

    public enum Size {
        NONE(new HazardRule("No fire", 0.0, 0, 0)),
        SMALL(new HazardRule("Small fire", 0.20, 3, 60)),
        MEDIUM(new HazardRule("Medium fire", 0.60, 8, 60)),
        LARGE(new HazardRule("Large fire", 1.00, 0, 30));

        final HazardRule rule;

        Size(HazardRule rule) {
            this.rule = rule;
        }
    }

    private static final double CHANCE_OF_FIRE = 0.30;
    private static final double SPREAD_CHANCE = 0.25;
    private static final int MUST_FIRE_AFTER = 5;
    private static final double BUILD_UP = 1.5;
    private static final long ONE_HOUR = 3600;
    private static final long TWO_HOURS = 7200;

    private final Random random = new Random();
    private final Spell spell = new Spell();
    private Size size = Size.NONE;

    private final Set<Stops> burning = new LinkedHashSet<>();

    private Size lastSize = Size.NONE;
    private long lastStartSeconds = 0;
    private int changesWithoutFire = 0;

    // trains already checked at this fire, and if a small fire has used its one delay
    private final Set<Vehicles> passed = new HashSet<>();
    private boolean smallFireDelayed = false;

    private final Predicate<Vehicles> nearFire = train ->
            burning.contains(train.getCurStop())
                    || burning.contains(train.getNextStop());

    // starts it, refuses a time over the limit
    public void begin(Size newSize, Stops where, int minutes, long nowSeconds)
            throws DurationLimitException {
        spell.start(newSize.rule, minutes, nowSeconds);
        size = newSize;
        setFlames(false);
        burning.clear();
        burning.add(where);
    }

    // called on each weather change: rain or fog puts the fire out, otherwise a new one may start
    public void maybeStart(
            Weather weather,
            Vehicles[] trains,
            long nowSeconds) {

        changesWithoutFire++;

        if (!weather.allowsFire()) {
            putOut();
            lastSize = Size.NONE;  // rain or fog also ends the build-up
            lastStartSeconds = 0;
            return;
        }

        if (size != Size.NONE) {
            return;
        }

        boolean mustHappen = changesWithoutFire >= MUST_FIRE_AFTER;

        if (!mustHappen && random.nextDouble() >= CHANCE_OF_FIRE) {
            return;
        }

        Size newSize = pickSize(nowSeconds);

        Vehicles anyTrain =
                GenericUtil.randomItem(Arrays.asList(trains), random);

        Stops where =
                GenericUtil.randomItem(anyTrain.route.stations, random);

        try {
            begin(newSize, where,
                    newSize.rule.randomMinutes(random), nowSeconds);
        } catch (DurationLimitException ex) {
            System.out.println(ex.getMessage());
            return;
        }

        changesWithoutFire = 0;
        lastStartSeconds = nowSeconds;
        lastSize = newSize;

        if (newSize == Size.LARGE) {
            lastSize = Size.NONE;
        }

        spread(newSize, where, trains);
        setFlames(true);

        passed.clear();
        smallFireDelayed = false;
    }

    // called every frame: a train that reaches a burning stop gets one chance of a delay,
    // a small fire delays one train only, a large fire stops trains instead
    public void affectPassingTrains(Vehicles[] trains) {
        if (size == Size.NONE || size == Size.LARGE) {
            return;
        }

        List<Vehicles> near = Arrays.stream(trains)
                .filter(nearFire)
                .collect(Collectors.toList());
        passed.retainAll(near);  // forget trains that moved on, they can be hit again later

        near.stream()
                .filter(passed::add)  // only trains that just arrived
                .filter(train -> size != Size.SMALL || !smallFireDelayed)
                .filter(train -> random.nextDouble() < size.rule.delayChance)
                .forEach(train -> {
                    train.delay(size.rule.delaySteps);
                    if (size == Size.SMALL) {
                        smallFireDelayed = true;
                    }
                });
    }

    // small fires are most common, a recent small or medium fire makes a bigger one more likely
    private Size pickSize(long nowSeconds) {
        double[] odds = {0.5, 0.3, 0.2};

        long sinceLast = nowSeconds - lastStartSeconds;

        if (lastSize == Size.SMALL && sinceLast >= ONE_HOUR) {
            odds = raise(odds, 1);
        }

        if (lastSize == Size.MEDIUM && sinceLast >= TWO_HOURS) {
            odds = raise(odds, 2);
        }

        double roll = random.nextDouble();

        if (roll < odds[0]) {
            return Size.SMALL;
        }

        if (roll < odds[0] + odds[1]) {
            return Size.MEDIUM;
        }

        return Size.LARGE;
    }

    // makes one chance 50% bigger and shrinks the others to fit
    private double[] raise(double[] odds, int which) {
        double raised = odds[which] * BUILD_UP;
        double shrink = (1 - raised) / (1 - odds[which]);

        double[] result = new double[odds.length];

        for (int i = 0; i < odds.length; i++) {
            if (i == which) {
                result[i] = raised;
            } else {
                result[i] = odds[i] * shrink;
            }
        }

        return result;
    }

    // medium fire reaches 2 more stops at most, large keeps going until 4 or 5 burn
    private void spread(Size newSize, Stops start, Vehicles[] trains) {
        if (newSize == Size.MEDIUM) {
            for (Stops next : neighboursOf(start, trains)) {
                if (burning.size() < 3
                        && random.nextDouble() < SPREAD_CHANCE) {
                    burning.add(next);
                }
            }
        } else if (newSize == Size.LARGE) {
            // add a stop next to the fire until 4 or 5 burn
            int target = 4 + random.nextInt(2);

            while (burning.size() < target) {
                List<Stops> edge = burning.stream()
                        .flatMap(stop ->
                                neighboursOf(stop, trains).stream())
                        .filter(stop -> !burning.contains(stop))
                        .distinct()
                        .collect(Collectors.toList());

                if (edge.isEmpty()) {
                    break;
                }

                burning.add(GenericUtil.randomItem(edge, random));
            }
        }
    }

    // stops before and after this one on any line, e.g. Central touches Rosebury and Jubilee
    private List<Stops> neighboursOf(Stops stop, Vehicles[] trains) {
        return Arrays.stream(trains)
                .map(train -> train.route.stations)
                .flatMap(line -> neighboursOnLine(line, stop))
                .distinct()
                .collect(Collectors.toList());
    }

    // the stop before and after on one line, none if it is not on the line
    private Stream<Stops> neighboursOnLine(List<Stops> line, Stops stop) {
        int i = line.indexOf(stop);
        if (i < 0) {
            return Stream.empty();
        }
        return Stream.of(i - 1, i + 1)
                .filter(index -> index >= 0 && index < line.size())
                .map(line::get);
    }

    // a fire never burns longer than its limit
    public void endIfOver(long nowSeconds) {
        if (size != Size.NONE && spell.isOver(nowSeconds)) {
            putOut();
        }
    }

    // clears the fire and its timer
    private void putOut() {
        size = Size.NONE;
        setFlames(false);
        burning.clear();
        passed.clear();
        spell.reset();
    }

    // marks the burning stops so the map can draw them
    private void setFlames(boolean on) {
        burning.forEach(stop -> stop.onFire = on);
    }

    // a large fire stops trains that are at or next to a burning stop
    public boolean stops(Vehicles train) {
        return size == Size.LARGE && nearFire.test(train);
    }

    // text for the top bar, e.g. Fire: Medium fire at Central +2
    public String getText() {
        return burning.stream()
                .findFirst()
                .map(start -> "Fire: " + size.rule.label
                        + " at " + start.getName() + more())
                .orElse("Fire: none");
    }

    private String more() {
        if (burning.size() > 1) {
            return " +" + (burning.size() - 1);
        }
        return "";
    }
}

// numbers for one weather or fire: delay chance, delay steps, longest time
class HazardRule {

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
class Spell {

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
