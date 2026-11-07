// the two custom exceptions, both checked

// thrown when service opens or closes (trains run 06:00 AM to midnight)
public class SimulationTimeException extends Exception {

    public SimulationTimeException(String message) {
        super(message);
    }
}

// thrown when a weather or fire is asked to last longer than its limit
class DurationLimitException extends Exception {

    public DurationLimitException(String what, int minutes, int maxMinutes) {
        super(what + " must last 1 to " + maxMinutes + " minutes, not " + minutes);
    }
}
