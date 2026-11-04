import java.util.Random;

// The weather for the whole map. It changes every PERIOD_HOURS of simulated
// time (not every tick) and the new weather is picked from what suits the time
// of day, e.g. fog is likely at night and storms in the afternoon. Because it
// follows the simulation clock, the 1x/2x/4x speed speeds it up too.
public class Weather {

    // delayChance: chance that a train is delayed when this weather begins.
    // delaySteps: how many steps a delayed train waits. Fog is different: it
    // stops every train until it clears (see stopsTrains).
    public enum Type {
        SUNNY("Sunny", 0.00, 0),
        CLOUDY("Cloudy", 0.15, 3),
        STORM("Storm", 0.80, 8),
        FOG("Foggy", 1.00, 0);

        final String label;
        final double delayChance;
        final int delaySteps;

        Type(String label, double delayChance, int delaySteps) {
            this.label = label;
            this.delayChance = delayChance;
            this.delaySteps = delaySteps;
        }
    }

    public static final int PERIOD_HOURS = 3;
    public static final int PERIOD_SECONDS = PERIOD_HOURS * 3600;

    private final Random random = new Random();
    private Type type = Type.SUNNY;
    private long period = -1;      // which 3-hour block of simulated time we are in
    private boolean changed = false;

    // Called by Time as the clock moves. Only a new 3-hour block picks new weather.
    public void update(long newPeriod) {
        if (newPeriod != period) {
            period = newPeriod;
            type = pickFor((int) ((newPeriod * PERIOD_HOURS) % 24));
            changed = true;
        }
    }

    // Time-of-day tendencies. Listing a type twice makes it twice as likely.
    private Type pickFor(int hour) {
        Type[] choices;
        if (hour < 6) {
            choices = new Type[] {Type.FOG, Type.FOG, Type.CLOUDY};
        } else if (hour < 9) {
            choices = Type.values();                         // morning: anything
        } else if (hour < 12) {
            choices = new Type[] {Type.SUNNY, Type.SUNNY, Type.CLOUDY};
        } else if (hour < 18) {
            choices = new Type[] {Type.SUNNY, Type.SUNNY, Type.STORM};
        } else if (hour < 21) {
            choices = Type.values();                         // evening: anything
        } else {
            choices = new Type[] {Type.FOG, Type.CLOUDY};
        }
        return choices[random.nextInt(choices.length)];
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
        for (Vehicles train : trains) {
            if (random.nextDouble() < type.delayChance) {
                train.delay(type.delaySteps);
            }
        }
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
}
