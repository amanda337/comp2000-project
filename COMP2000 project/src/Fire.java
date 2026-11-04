import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

// A fire starting at one station. It is separate from the weather, but follows
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
public class Fire {

    // A small or medium fire delays nearby trains (with delayChance); a large
    // fire stops them completely until it is out.
    public enum Size {
        NONE("No fire", 0.0, 0, 0),
        SMALL("Small fire", 0.20, 3, 60),
        MEDIUM("Medium fire", 0.60, 8, 60),
        LARGE("Large fire", 1.00, 0, 30);

        final String label;
        final double delayChance;
        final int delaySteps;
        final int maxMinutes;

        Size(String label, double delayChance, int delaySteps, int maxMinutes) {
            this.label = label;
            this.delayChance = delayChance;
            this.delaySteps = delaySteps;
            this.maxMinutes = maxMinutes;
        }
    }

    private static final double CHANCE_OF_FIRE = 0.30;   // per weather change, if allowed
    private static final double SPREAD_CHANCE = 0.25;    // medium fire, per neighbouring station
    private static final int MUST_FIRE_AFTER = 5;        // weather changes without a fire
    private static final double BUILD_UP = 1.5;          // 50% more likely
    private static final long ONE_HOUR = 3600;
    private static final long TWO_HOURS = 7200;

    private final Random random = new Random();
    private Size size = Size.NONE;
    private final Set<Stops> burning = new LinkedHashSet<>(); // first one is where it started
    private long endSeconds = 0;

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
        if (minutes < 1 || minutes > newSize.maxMinutes) {
            throw new DurationLimitException(newSize.label, minutes, newSize.maxMinutes);
        }
        size = newSize;
        setFlames(false);
        burning.clear();
        burning.add(where);
        endSeconds = nowSeconds + minutes * 60L;
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
        Vehicles anyTrain = trains[random.nextInt(trains.length)];
        Stops where = anyTrain.route.stations.get(random.nextInt(anyTrain.route.stations.size()));

        try {
            begin(newSize, where, Weather.randomMinutes(random, newSize.maxMinutes), nowSeconds);
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
        Arrays.stream(trains)
                .filter(nearFire)
                .limit(mostTrains)
                .filter(train -> random.nextDouble() < size.delayChance)
                .forEach(train -> train.delay(size.delaySteps));
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
                burning.add(edge.get(random.nextInt(edge.size())));
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
        if (size != Size.NONE && nowSeconds >= endSeconds - Weather.STEP_GUARD_SECONDS) {
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
                .map(start -> "Fire: " + size.label + " at " + start.getName() + more())
                .orElse("Fire: none");
    }

    private String more() {
        if (burning.size() > 1) {
            return " +" + (burning.size() - 1);
        }
        return "";
    }
}
