// Base type for anything that can be scheduled and later run against the
// trains. Ordered by the step it is due on, so an EventQueue always hands
// out the earliest due event first.]
import java.util.Comparator;
public abstract class SimulationEvent implements Comparable<SimulationEvent> {

    private final long step;

    protected SimulationEvent(long step){
        this.step = step;
    }

    public long getStep(){
        return step;
    }

    // Applies this event's effect to the running trains.
    public abstract void execute(Vehicles[] trains);

    @Override
    public int compareTo(SimulationEvent other){
        return Comparator.comparingLong(SimulationEvent::getStep).compare(this, other);
    }
    
}
