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

// Weather and Fire in one file. They are separate systems that share three
// small helpers, kept at the bottom: HazardRule (the numbers for one kind of
// weather or fire), Spell (how long the current one lasts) and the
// GenericUtil.randomItem pick. Time owns one Weather and one Fire.

// ---------------------------------------------------------------------------
// WEATHER: one weather for the whole map. Each spell lasts a random time up to
// a limit for that weather (sunny 4 h, cloudy 2 h, rain 1 h, fog 30 min), then
// a different weather follows, so no weather can run past its limit. The next
// weather suits the time of day, e.g. fog is likely at night. It follows the
// simulation clock, so the 1x/2x/4x speed speeds it up too.
// ---------------------------------------------------------------------------
public class Weather {

    // Rain and cloud only delay trains. Fog is different: it stops every train
    // while it lasts (see stopsTrains).
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

    // Starts a spell. The limit is checked in Spell.start, so a spell longer
    // than its weather allows is refused before anything changes.
    public void begin(Type next, int minutes, long nowSeconds) throws DurationLimitException {
        spell.start(next.rule, minutes, nowSeconds);
        type = next;
        history.add(next);
        changed = true;
    }

    // Called by Time as the clock moves. Nothing happens until the spell ends.
    public void update(long nowSeconds, int hourOfDay) {
        if (!spell.isOver(nowSeconds)) {
            return;
        }
        Type next = pickFor(hourOfDay);
        try {
            begin(next, next.rule.randomMinutes(random), nowSeconds);
        } catch (DurationLimitException ex) {
            // Cannot happen with randomMinutes, but if it ever does, keep the
            // current weather a little longer instead of crashing the clock.
            System.out.println(ex.getMessage());
            spell.extend(nowSeconds);
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
        return GenericUtil.randomItem(different, random);
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
        type.rule.delay(Arrays.stream(trains), random);
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
        return type.rule.label;
    }

    // How many spells of each weather so far, e.g. "Cloudy 1, Foggy 2".
    // groupingBy + counting, with a TreeMap so the names stay in order.
    public String summary() {
        Map<String, Long> counts = history.stream()
                .collect(Collectors.groupingBy(t -> t.rule.label, TreeMap::new, Collectors.counting()));
        return counts.entrySet().stream()
                .map(entry -> entry.getKey() + " " + entry.getValue())
                .collect(Collectors.joining(", "));
    }
}

// ---------------------------------------------------------------------------
// FIRE: starts at one station. It is separate from the weather, but follows
// one rule from it: no fire in rain or fog (Weather.allowsFire), and a burning
// fire is put out the moment rain or fog starts. A fire burns for a random time
// up to its limit (small and medium 1 h, large 30 min), then it is out.
//
// How far it spreads depends on its size:
//   small  - stays at one station and delays only one train
//   medium - each station next to the start has a 25% chance to catch fire,
//            up to 2 extra stations
//   large  - keeps spreading to neighbouring stations until 4 or 5 burn
// Only trains at or heading into a burning station are affected.
//
// When a fire starts:
//   - a fire is certain once 5 weather changes have passed without one
//     (and the weather allows it); otherwise each change has a 30% chance
//   - fires build up: an hour after a small fire started, the next fire is 50%
//     more likely to be medium; two hours after a medium fire started, it is
//     50% more likely to be large. After a large fire the build-up starts over.
//   - rain or fog puts the fire out and also ends the build-up
// ---------------------------------------------------------------------------
class Fire {

    // A small or medium fire delays nearby trains (with the rule's delayChance);
    // a large fire stops them completely until it is out.
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

    private static final double CHANCE_OF_FIRE = 0.30;   // per weather change, if allowed
    private static final double SPREAD_CHANCE = 0.25;    // medium fire, per neighbouring station
    private static final int MUST_FIRE_AFTER = 5;        // weather changes without a fire
    private static final double BUILD_UP = 1.5;          // 50% more likely
    private static final long ONE_HOUR = 3600;
    private static final long TWO_HOURS = 7200;

    private final Random random = new Random();
    private final Spell spell = new Spell();
    private Size size = Size.NONE;
    private final Set<Stops> burning = new LinkedHashSet<>(); // first one is where it started

    // The build-up: the size of the last fire and when it started.
    private Size lastSize = Size.NONE;
    private long lastStartSeconds = 0;
    private int changesWithoutFire = 0;

    // Near = the train is at a burning station or about to arrive at one.
    private final Predicate<Vehicles> nearFire = train ->
            burning.contains(train.getCurStop()) || burning.contains(train.getNextStop());

    // Starts a fire. Like Weather.begin, it refuses a time over the limit.
    public void begin(Size newSize, Stops where, int minutes, long nowSeconds)
            throws DurationLimitException {
        spell.start(newSize.rule, minutes, nowSeconds);
        size = newSize;
        setFlames(false);
        burning.clear();
        burning.add(where);
    }

    // Called when the weather changes. Rain or fog puts any fire out; otherwise
    // a new fire may start if none is burning.
    public void maybeStart(Weather weather, Vehicles[] trains, long nowSeconds) {
        changesWithoutFire++;
        if (!weather.allowsFire()) {
            putOut();
            lastSize = Size.NONE; // rain or fog also ends the build-up
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

        // Burns at a random station on a random train's line.
        Vehicles anyTrain = GenericUtil.randomItem(Arrays.asList(trains), random);
        Stops where = GenericUtil.randomItem(anyTrain.route.stations, random);

        try {
            begin(newSize, where, newSize.rule.randomMinutes(random), nowSeconds);
        } catch (DurationLimitException ex) {
            System.out.println(ex.getMessage()); // no fire starts
            return;
        }

        changesWithoutFire = 0;
        lastStartSeconds = nowSeconds;
        lastSize = newSize;
        if (newSize == Size.LARGE) {
            lastSize = Size.NONE; // after a large fire the build-up starts over
        }

        spread(newSize, where, trains);
        setFlames(true);

        // A small fire delays one train at most; the others can delay several.
        long mostTrains = trains.length;
        if (newSize == Size.SMALL) {
            mostTrains = 1;
        }
        newSize.rule.delay(Arrays.stream(trains).filter(nearFire).limit(mostTrains), random);
    }

    // Small fires are the most common: 50% small, 30% medium, 20% large.
    // A recent small fire raises the chance of medium by 50% (30% -> 45%), and
    // a recent medium fire raises the chance of large by 50% (20% -> 30%).
    private Size pickSize(long nowSeconds) {
        double[] odds = {0.5, 0.3, 0.2}; // small, medium, large

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

    // Makes one chance 50% bigger and shrinks the other two so they still add up to 1.
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

    // Spreads the fire from its start to neighbouring stations.
    private void spread(Size newSize, Stops start, Vehicles[] trains) {
        if (newSize == Size.MEDIUM) {
            // Each neighbour of the start may catch fire, but at most 2 do.
            for (Stops next : neighboursOf(start, trains)) {
                if (burning.size() < 3 && random.nextDouble() < SPREAD_CHANCE) {
                    burning.add(next);
                }
            }
        } else if (newSize == Size.LARGE) {
            // Keep adding a station next to the fire until 4 or 5 are burning.
            int target = 4 + random.nextInt(2);
            while (burning.size() < target) {
                List<Stops> edge = burning.stream()
                        .flatMap(stop -> neighboursOf(stop, trains).stream())
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

    // The stations right before and after this one on any train's line, e.g.
    // Central touches Rosebury, Jubilee and Sherie Grove.
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

    // A fire never burns past its time limit.
    public void endIfOver(long nowSeconds) {
        if (size != Size.NONE && spell.isOver(nowSeconds)) {
            putOut();
        }
    }

    private void putOut() {
        size = Size.NONE;
        setFlames(false);
        burning.clear();
    }

    // Marks the burning stations so the map can draw a flame on each.
    private void setFlames(boolean on) {
        burning.forEach(stop -> stop.onFire = on);
    }

    // Only a large fire stops trains, and only the ones near it.
    public boolean stops(Vehicles train) {
        return size == Size.LARGE && nearFire.test(train);
    }

    public String getText() {
        // e.g. "Fire: Medium fire at Central +2" (2 more stations burning)
        return burning.stream().findFirst()
                .map(start -> "Fire: " + size.rule.label + " at " + start.getName() + more())
                .orElse("Fire: none");
    }

    private String more() {
        if (burning.size() > 1) {
            return " +" + (burning.size() - 1);
        }
        return "";
    }
}

// ---------------------------------------------------------------------------
// SHARED by Weather and Fire
// ---------------------------------------------------------------------------

// The numbers for one kind of weather or fire: its name, how likely it is to
// delay a train, for how many steps, and the longest it may last.
class HazardRule {

    private static final int MIN_MINUTES = 15;

    final String label;
    final double delayChance;
    final int delaySteps;
    final int maxMinutes;

    HazardRule(String label, double delayChance, int delaySteps, int maxMinutes) {
        this.label = label;
        this.delayChance = delayChance;
        this.delaySteps = delaySteps;
        this.maxMinutes = maxMinutes;
    }

    // Refuses a length outside 1..maxMinutes.
    void check(int minutes) throws DurationLimitException {
        if (minutes < 1 || minutes > maxMinutes) {
            throw new DurationLimitException(label, minutes, maxMinutes);
        }
    }

    // A random length from 15 minutes up to the limit, in 5-minute steps.
    int randomMinutes(Random random) {
        return MIN_MINUTES + 5 * random.nextInt((maxMinutes - MIN_MINUTES) / 5 + 1);
    }

    // Each train in the stream is delayed with this rule's chance. Weather
    // passes every train; Fire passes only the trains near the flames.
    void delay(Stream<Vehicles> trains, Random random) {
        trains.filter(train -> random.nextDouble() < delayChance)
                .forEach(train -> train.delay(delaySteps));
    }
}

// When the current weather or fire ends, and the check that it is not too long.
class Spell {

    // The clock jumps about 5 minutes per step (plus up to 15 seconds of
    // wobble). A spell ends at the first step within this many seconds of its
    // end time, so the jump can never carry it past its limit.
    private static final long STEP_GUARD_SECONDS = 330;

    private long endSeconds = 0; // simulated second when it ends

    void start(HazardRule rule, int minutes, long nowSeconds) throws DurationLimitException {
        rule.check(minutes);
        endSeconds = nowSeconds + minutes * 60L;
    }

    // Keeps going 15 more minutes (used only if a start was refused).
    void extend(long nowSeconds) {
        endSeconds = nowSeconds + 15 * 60L;
    }

    boolean isOver(long nowSeconds) {
        return nowSeconds >= endSeconds - STEP_GUARD_SECONDS;
    }
}
