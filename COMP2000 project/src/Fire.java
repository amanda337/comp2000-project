import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// a fire that starts at one station and can spread, never in rain or fog
public class Fire {

    public enum Size {
        NONE(new Weather.HazardRule("No fire", 0.0, 0, 0)),
        SMALL(new Weather.HazardRule("Small fire", 0.20, 3, 60)),
        MEDIUM(new Weather.HazardRule("Medium fire", 0.60, 8, 60)),
        LARGE(new Weather.HazardRule("Large fire", 1.00, 0, 30));

        final Weather.HazardRule rule;

        Size(Weather.HazardRule rule) {
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
    private final Weather.Spell spell = new Weather.Spell();
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
            throws Weather.DurationLimitException {
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
        } catch (Weather.DurationLimitException ex) {
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
