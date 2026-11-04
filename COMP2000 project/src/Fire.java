import java.util.Random;

// A fire at one station. It is separate from the weather, but follows one
// rule from it: no fire in a storm or fog (Weather.allowsFire). Each time the
// weather changes the old fire is put out and a new one may start. Only trains
// at or heading into the burning station are affected, not the whole network.
public class Fire {

    // delayChance / delaySteps: a small or medium fire delays nearby trains.
    // A large fire stops them completely until the fire is out.
    public enum Size {
        NONE("No fire", 0.0, 0),
        SMALL("Small fire", 0.20, 3),
        MEDIUM("Medium fire", 0.60, 8),
        LARGE("Large fire", 1.00, 0);

        final String label;
        final double delayChance;
        final int delaySteps;

        Size(String label, double delayChance, int delaySteps) {
            this.label = label;
            this.delayChance = delayChance;
            this.delaySteps = delaySteps;
        }
    }

    private static final double CHANCE_OF_FIRE = 0.30; // per weather change, if allowed

    private final Random random = new Random();
    private Size size = Size.NONE;
    private Stops place = null;

    // Called when the weather changes: the old fire ends, a new one may start.
    public void update(Weather weather, Vehicles[] trains) {
        size = Size.NONE;
        place = null;

        if (!weather.allowsFire() || random.nextDouble() >= CHANCE_OF_FIRE) {
            return;
        }

        Size[] sizes = {Size.SMALL, Size.MEDIUM, Size.LARGE};
        size = sizes[random.nextInt(sizes.length)];

        // Burns at a random station on a random train's line.
        Vehicles anyTrain = trains[random.nextInt(trains.length)];
        place = anyTrain.route.stations.get(random.nextInt(anyTrain.route.stations.size()));

        for (Vehicles train : trains) {
            if (isNear(train) && random.nextDouble() < size.delayChance) {
                train.delay(size.delaySteps);
            }
        }
    }

    // Near = the train is at the burning station or about to arrive at it.
    private boolean isNear(Vehicles train) {
        return place != null
                && (place.equals(train.getCurStop()) || place.equals(train.getNextStop()));
    }

    // Only a large fire stops trains, and only the ones near it.
    public boolean stops(Vehicles train) {
        return size == Size.LARGE && isNear(train);
    }

    public String getText() {
        if (place == null) {
            return "Fire: none";
        }
        return "Fire: " + size.label + " at " + place.getName();
    }
}
