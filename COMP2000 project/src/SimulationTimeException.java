// thrown when service opens or closes (trains run 06:00 AM to 03:00 AM)
public class SimulationTimeException extends Exception {

    public SimulationTimeException(String message) {
        super(message);
    }
}
