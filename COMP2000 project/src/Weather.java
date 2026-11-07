
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Weather and Fire in one file.

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

    public void begin(Type next, int minutes, long nowSeconds)
            throws DurationLimitException {
        spell.start(next.rule, minutes, nowSeconds);
        type = next;
        history.add(next);
        changed = true;
    }

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

        // A spell never repeats the weather it follows, and rain and fog never
        // follow each other.
        List<Type> different = choices.stream()
                .filter(t -> t != type)
                .filter(t -> !clashes(type, t))
                .collect(Collectors.toList());

        return GenericUtil.randomItem(different, random);
    }

    private static boolean clashes(Type from, Type to) {
        return (from == Type.RAIN && to == Type.FOG)
                || (from == Type.FOG && to == Type.RAIN);
    }

    public boolean takeChange() {
        boolean result = changed;
        changed = false;
        return result;
    }

    public void rollDelays(Vehicles[] trains) {
        type.rule.delay(Arrays.stream(trains), random);
    }

    public boolean stopsTrains() {
        return type == Type.FOG;
    }

    public boolean allowsFire() {
        return type == Type.SUNNY || type == Type.CLOUDY;
    }

    public String getLabel() {
        return type.rule.label;
    }

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

    private final Predicate<Vehicles> nearFire = train ->
            burning.contains(train.getCurStop())
                    || burning.contains(train.getNextStop());

    public void begin(Size newSize, Stops where, int minutes, long nowSeconds)
            throws DurationLimitException {
        spell.start(newSize.rule, minutes, nowSeconds);
        size = newSize;
        setFlames(false);
        burning.clear();
        burning.add(where);
    }

    public void maybeStart(
            Weather weather,
            Vehicles[] trains,
            long nowSeconds) {

        changesWithoutFire++;

        if (!weather.allowsFire()) {
            putOut();
            lastSize = Size.NONE; // rain or fog ends the build-up completely
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

        long mostTrains = trains.length;

        if (newSize == Size.SMALL) {
            mostTrains = 1;
        }

        newSize.rule.delay(
                Arrays.stream(trains).filter(nearFire).limit(mostTrains),
                random);
    }

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

    private void spread(Size newSize, Stops start, Vehicles[] trains) {
        if (newSize == Size.MEDIUM) {
            for (Stops next : neighboursOf(start, trains)) {
                if (burning.size() < 3
                        && random.nextDouble() < SPREAD_CHANCE) {
                    burning.add(next);
                }
            }
        } else if (newSize == Size.LARGE) {
            // Keep adding a station next to the fire until 4 or 5 are burning.
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

    private List<Stops> neighboursOf(Stops stop, Vehicles[] trains) {
        List<Stops> result = new ArrayList<>();

        for (Vehicles train : trains) {
            List<Stops> line = train.route.stations;
            int i = line.indexOf(stop);

            if (i > 0) {
                result.add(line.get(i - 1));
            }

            if (i >= 0 && i < line.size() - 1) {
                result.add(line.get(i + 1));
            }
        }

        return result.stream().distinct().collect(Collectors.toList());
    }

    public void endIfOver(long nowSeconds) {
        if (size != Size.NONE && spell.isOver(nowSeconds)) {
            putOut();
        }
    }

    private void putOut() {
        size = Size.NONE;
        setFlames(false);
        burning.clear();
        spell.reset();
    }

    private void setFlames(boolean on) {
        burning.forEach(stop -> stop.onFire = on);
    }

    public boolean stops(Vehicles train) {
        return size == Size.LARGE && nearFire.test(train);
    }

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

    void check(int minutes) throws DurationLimitException {
        if (minutes < 1 || minutes > maxMinutes) {
            throw new DurationLimitException(label, minutes, maxMinutes);
        }
    }

    int randomMinutes(Random random) {
        return MIN_MINUTES
                + 5 * random.nextInt(
                        (maxMinutes - MIN_MINUTES) / 5 + 1);
    }

    void delay(Stream<Vehicles> trains, Random random) {
        trains.filter(train ->
                        random.nextDouble() < delayChance)
                .forEach(train -> train.delay(delaySteps));
    }
}

class Spell {

    private static final long STEP_GUARD_SECONDS = 330;

    private long endSeconds = 0;

    void start(
            HazardRule rule,
            int minutes,
            long nowSeconds) throws DurationLimitException {
        rule.check(minutes);
        endSeconds = nowSeconds + minutes * 60L;
    }

    // Back to "not running", used when a fire is put out.
    void reset() {
        endSeconds = 0;
    }

    void extend(long nowSeconds) {
        endSeconds = nowSeconds + 15 * 60L;
    }

    boolean isOver(long nowSeconds) {
        return nowSeconds >= endSeconds - STEP_GUARD_SECONDS;
    }
}
