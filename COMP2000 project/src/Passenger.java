import java.util.ArrayList;
import java.util.List;
//import java.util.function.Consumer;  // only used by the unused code below
import java.util.Random;

public class Passenger {
    // a commuter with a random start and end stop on one line

    private enum State {Waitting, Riding, Arrived }

    private static final Random random = new Random();
    private static int counter =0;

    private final String name;
    private final Stops.Routes route; 
    private final Stops start;
    private final Stops end;
    private State state = State.Waitting;

    public Passenger(String name, Stops.Routes route)
    {
        this.name = name;
        this.route = route;

        int n = route.stations.size();
        int s = random.nextInt(n);
        int e =s;
        if(n > 1)
        {
            e = random.nextInt(n -1);
            if(e > s)
            {
                e++;
            }
        }
        this.start = route.stations.get(s);
        this.end = route.stations.get(e);
    }

    public static void Spawnbatch(List<Passenger> into, List<Stops.Routes> routes)
    {
        if(routes.isEmpty())
        {
            return;
        }
        int count = 10 + random.nextInt(11);
        for(int i = 0; i < count; i++)
        {
            Stops.Routes r = routes.get(random.nextInt(routes.size()));
            into.add(new Passenger("pass" + (++counter), r));
        }
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
        if(curStop == null)
        {
            return;
        }
        if(state == State.Waitting && trainRoute == route && curStop.equals(start))
        {
            onBoard.add(this);
            state = State.Riding;
        } else if (state == State.Riding && onBoard.contains(this) && curStop.equals(end))
        {
            onBoard.remove(this);
            state = State.Arrived;
        }
    }
}

