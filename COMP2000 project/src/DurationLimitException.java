// Thrown when a weather spell or a fire is asked to last longer than its limit
// (for example fog for 45 minutes when fog is capped at 30). It is checked, so
// the caller has to handle it, like SimulationTimeException.
public class DurationLimitException extends Exception {

    public DurationLimitException(String what, int minutes, int maxMinutes) {
        super(what + " must last 1 to " + maxMinutes + " minutes, not " + minutes);
    }
}
