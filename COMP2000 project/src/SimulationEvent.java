<<<<<<< HEAD
// Base type for anything that can be scheduled and later run against the
// trains. Ordered by the step it is due on, so an EventQueue always hands
// out the earliest due event first.]
import java.util.Comparator;
=======
import java.util.Arrays;
import java.util.PriorityQueue;

// anything that can be scheduled, ordered by the step it is due
>>>>>>> 1bdc7637ab398282885d1b89f012f0b25ac7e050
public abstract class SimulationEvent implements Comparable<SimulationEvent> {

    private final long step;

    protected SimulationEvent(long step){
        this.step = step;
    }

    public long getStep(){
        return step;
    }

    // runs the event on the trains
    public abstract void execute(Vehicles[] trains);

    @Override
    public int compareTo(SimulationEvent other){
        return Comparator.comparingLong(SimulationEvent::getStep).compare(this, other);
    }
<<<<<<< HEAD
    
=======

    // accident: delays one named train for a number of steps
    public static class AccidentEvent extends SimulationEvent {

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

    // holds events, the earliest one comes out first
    // the methods are synchronized so adding and removing stay safe even from two threads
    public static class EventQueue<T extends SimulationEvent> {

        private final PriorityQueue<T> events = new PriorityQueue<>();

        // puts an event in the queue
        public synchronized void add(T event){
            events.add(event);
        }

        // true if the earliest event is due by this step
        public synchronized boolean hasDueEvent(long step){
            return !events.isEmpty() && events.peek().getStep() <= step;
        }

        // takes out the earliest event
        public synchronized T nextEvent(){
            return events.poll();
        }
    }
>>>>>>> 1bdc7637ab398282885d1b89f012f0b25ac7e050
}
