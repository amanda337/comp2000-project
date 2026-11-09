import java.util.ArrayList;
import java.util.List;
//import java.util.function.Consumer;  // only used by the unused code below
import java.util.Random;

public class Passenger {
    // a commuter with a random start and end stop on one line

    // private String name;
    // private Stops start;
    // private Stops end;

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

    // picks a random start and end stop
//     public Passenger(String name) {
//         this.name = name;
//         this.start = startPoint(allRoutes()).get(0);
//         this.end = startPoint(allRoutes()).get(1);
//     }

//     public static ArrayList<Stops.Routes> allRoutes(){
//         // all four train lines in one list
//         ArrayList<Stops.Routes> totalRoutes = new ArrayList<>();
//         totalRoutes.add(Stops.Routes.redLine());
//         totalRoutes.add(Stops.Routes.blueLine());
//         totalRoutes.add(Stops.Routes.purpleLine());
//         totalRoutes.add(Stops.Routes.greenLine());
//         return totalRoutes;
//     }

//     // unused: only picks a start and end, no passenger is created from it yet
//     // public static void spawnPassengers(){
//     //     startPoint(allRoutes());
//     // }
//     // Consumer<Time> spawnPassengers = time ->{
//     //     if(time.ticks % 60 == 0){
//     //         for(int i =0;i<100;i++){
//     //             startPoint(allRoutes());
//     //         }
//     //     }
//     //     };

//     public static ArrayList<Stops> startPoint(ArrayList<Stops.Routes> route){
//         // random start (position 0) and end (position 1) stop
//         ArrayList<Stops> startEnd = new ArrayList<>();
//         Stops startpoint;
//         Stops endpoint;
//         int randomRoute = (int) (Math.random() * (route.size() - 1)) ;
//         Stops.Routes routes;
//         // start and end are on the same line
//         routes = route.get(randomRoute);
//         int randomStart = (int) (Math.random() * (routes.stations.size() - 1)) + 0;
//         int randomEnd = (int) (Math.random() * (routes.stations.size() -  1)) + 0;

//         startpoint = routes.stations.get(randomStart);
//         endpoint = routes.stations.get(randomEnd);
//         startEnd.add(startpoint);
//         startEnd.add(endpoint);
//         return startEnd;

//     }

//     public String getName() {
//         return name;
//     }

//     // the one passenger the simulation uses
//     public static Passenger pass1() {
//         return new Passenger("pass1");
//     }

//     // gets on at the start stop and off at the end stop
//     public void checkBoarding(Stops curStop, ArrayList<Passenger> onBoard) {
//         if (curStop.equals(start) && !onBoard.contains(this)) {
//             onBoard.add(this);
//         } else if (curStop.equals(end) && onBoard.contains(this)) {
//             onBoard.remove(this);
//         }
//     }
// }
