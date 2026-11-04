import java.util.Arrays;
import java.util.PriorityQueue;

// The event system in one file: the base type (SimulationEvent), the one
// event we have (AccidentEvent), and the queue that holds them (EventQueue).

// Base type for anything that can be scheduled and later run against the
// trains. Ordered by the step it is due on, so an EventQueue always hands
// out the earliest due event first.
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
        return Long.compare(step, other.step);
    }
}

// A scheduled accident: once its step is reached, it stops one named train
// at its current stop for a number of steps.
class AccidentEvent extends SimulationEvent {

    private final String trainName;
    private final int delaySteps;

    public AccidentEvent(long step, String trainName, int delaySteps) {
        super(step);
        this.trainName = trainName;
        this.delaySteps = delaySteps;
    }

    @Override
    public void execute(Vehicles[] trains) {
        Arrays.stream(trains)
                .filter(train -> train.getName().equals(trainName))
                .forEach(train -> train.delay(delaySteps));
    }
}

// A queue of simulation events that always returns the earliest-due one
// first. "T extends SimulationEvent" means only real events can be added,
// while still letting different event subclasses share one queue.
//
// A PriorityQueue is not thread-safe. Panel adds the accidents on whichever
// thread builds the window while the Swing timer thread removes them, so every
// method locks the queue (synchronized). hasDueEvent() followed by
// nextEvent() is still two separate calls, which is safe only because a
// single thread (the timer) drains the queue.
class EventQueue<T extends SimulationEvent> {

    private final PriorityQueue<T> events = new PriorityQueue<>();

    public synchronized void add(T event){
        events.add(event);
    }

    // True if the earliest queued event is due by the given step.
    public synchronized boolean hasDueEvent(long step){
        return !events.isEmpty() && events.peek().getStep() <= step;
    }

    public synchronized T nextEvent(){
        return events.poll();
    }
}
