import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
 
public class Passenger {
    // a commuter with a random start and end stop on one line
 
    private enum State { Waitting, Riding, Arrived }
 
    private static final Random random = new Random();
    private static int counter = 0;
 
    private final String name;
    private final Stops.Routes route;
    private final Stops start;
    private final Stops end;
    private State state = State.Waitting;
 
    public Passenger(String name, Stops.Routes route)
    {
        this.name = name;
        this.route = route;
 
        List<Stops> stations = route.stations;
 
        // two different random indexes in one stream: distinct() guarantees
        // start != end, and limit() stops it (one index if the line has one stop)
        List<Integer> picks = random.ints(0, stations.size())
                .distinct()
                .limit(Math.min(2, stations.size()))
                .boxed()
                .collect(Collectors.toList());
 
        this.start = stations.get(picks.get(0));
        this.end = stations.get(picks.get(picks.size() - 1));
    }
 
    // adds 10-20 new passengers, each on a random one of the given routes
    public static void Spawnbatch(List<Passenger> into, List<Stops.Routes> routes, Time time)
    {
        if (routes.isEmpty())
        {
            return;
        }

        int count = 10;
        if ( time.isPeakHour() == true)
        {
            count += 10;
        }
        else 
        {
            count = 10;
        }
        count = count + random.nextInt(11);   // 10..20 inclusive
        System.out.println(count);
 
        into.addAll(IntStream.range(0, count)
                .mapToObj(i -> new Passenger("pass" + (++counter),
                        routes.get(random.nextInt(routes.size()))))
                .collect(Collectors.toList()));
    }
 
    public String GetName()
    {
        return name;
    }
 
    public boolean hasArrived()
    {
        return state == State.Arrived;
    }
 
    public void CheckBoarding(Stops curStop, ArrayList<Passenger> onBoard, Stops.Routes trainRoute)
    {
        if (curStop == null)
        {
            return;
        }
        if (state == State.Waitting && trainRoute == route && curStop.equals(start))
        {
            onBoard.add(this);
            state = State.Riding;
        }
        else if (state == State.Riding && onBoard.contains(this) && curStop.equals(end))
        {
            onBoard.remove(this);
            state = State.Arrived;
        }
    }
}