// The project's two custom exceptions. Both are checked on purpose, so the
// code that can trigger them is forced to decide what to do.

// Thrown when the simulated clock leaves the allowed service window
// (the trains only run from 06:00 AM to midnight). advance() declares
// "throws SimulationTimeException" so the tick loop must handle it when
// service starts or ends.
public class SimulationTimeException extends Exception {

    public SimulationTimeException(String message) {
        super(message);
    }
}

// Thrown when a weather spell or a fire is asked to last longer than its limit
// (for example fog for 45 minutes when fog is capped at 30).
class DurationLimitException extends Exception {

    public DurationLimitException(String what, int minutes, int maxMinutes) {
        super(what + " must last 1 to " + maxMinutes + " minutes, not " + minutes);
    }
}
