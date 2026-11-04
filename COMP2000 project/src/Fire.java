import java.util.Arrays;
import java.util.Optional;
import java.util.Random;
import java.util.function.Predicate;

// A fire at one station. It is separate from the weather, but follows one
// rule from it: no fire in rain or fog (Weather.allowsFire). Each fire burns
// for a random time up to its limit (small and medium 1 h, large 30 min), then
// it is out. Only trains at or heading into the burning station are affected.
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

    private static final double CHANCE_OF_FIRE = 0.30; // per weather change, if allowed

    private final Random random = new Random();
    private Size size = Size.NONE;
    private Stops place = null;
    private long endSeconds = 0;

    // Near = the train is at the burning station or about to arrive at it.
    private final Predicate<Vehicles> nearFire = train -> place != null
            && (place.equals(train.getCurStop()) || place.equals(train.getNextStop()));

    // Starts a fire. Like Weather.begin, it refuses a time over the limit.
    public void begin(Size newSize, Stops where, int minutes, long nowSeconds)
            throws DurationLimitException {
        if (minutes < 1 || minutes > newSize.maxMinutes) {
            throw new DurationLimitException(newSize.label, minutes, newSize.maxMinutes);
        }
        size = newSize;
        place = where;
        endSeconds = nowSeconds + minutes * 60L;
    }

    // Called when the weather changes. Rain or fog puts any fire out; otherwise
    // a new fire may start if none is burning.
    public void maybeStart(Weather weather, Vehicles[] trains, long nowSeconds) {
        if (!weather.allowsFire()) {
            putOut();
            return;
        }
        if (size != Size.NONE || random.nextDouble() >= CHANCE_OF_FIRE) {
            return;
        }

        Size[] sizes = {Size.SMALL, Size.MEDIUM, Size.LARGE};
        Size newSize = sizes[random.nextInt(sizes.length)];

        // Burns at a random station on a random train's line.
        Vehicles anyTrain = trains[random.nextInt(trains.length)];
        Stops where = anyTrain.route.stations.get(random.nextInt(anyTrain.route.stations.size()));

        try {
            begin(newSize, where, Weather.randomMinutes(random, newSize.maxMinutes), nowSeconds);
        } catch (DurationLimitException ex) {
            System.out.println(ex.getMessage()); // no fire starts
            return;
        }

        Arrays.stream(trains)
                .filter(nearFire)
                .filter(train -> random.nextDouble() < size.delayChance)
                .forEach(train -> train.delay(size.delaySteps));
    }

    // A fire never burns past its time limit.
    public void endIfOver(long nowSeconds) {
        if (size != Size.NONE && nowSeconds >= endSeconds - Weather.STEP_GUARD_SECONDS) {
            putOut();
        }
    }

    private void putOut() {
        size = Size.NONE;
        place = null;
    }

    // Only a large fire stops trains, and only the ones near it.
    public boolean stops(Vehicles train) {
        return size == Size.LARGE && nearFire.test(train);
    }

    public String getText() {
        return Optional.ofNullable(place)
                .map(station -> "Fire: " + size.label + " at " + station.getName())
                .orElse("Fire: none");
    }
}
