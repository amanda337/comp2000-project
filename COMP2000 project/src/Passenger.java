import java.util.ArrayList;
import java.util.function.Consumer;

public class Passenger {
    // A commuter with a fixed random start and end stop on one line.
    // checkBoarding compares a train's current stop against start/end, so
    // the passenger boards and alights without needing its own position.

    private String name;
    private Stops start;
    private Stops end;

    public Passenger(String name) {
        this.name = name;
        this.start = startPoint(allRoutes()).get(0);
        this.end = startPoint(allRoutes()).get(1);
    }

    public static ArrayList<Routes> allRoutes(){
        // adds all the trainlines into one array for easy accsess
        ArrayList<Routes> totalRoutes = new ArrayList<>();
        totalRoutes.add(Routes.redLine());
        totalRoutes.add(Routes.blueLine());
        totalRoutes.add(Routes.purpleLine());
        totalRoutes.add(Routes.greenLine());
        return totalRoutes;
    }

    public static void spawnPassengers(){
        //Spawns 100 Passangers every 60 tick or 30 in gmae mintues.
        startPoint(allRoutes());
    }
    // Consumer<Time> spawnPassengers = time ->{
    //     if(time.ticks % 60 == 0){
    //         for(int i =0;i<100;i++){
    //             startPoint(allRoutes());
    //         }
    //     }
    //     };

    public static ArrayList<Stops> startPoint(ArrayList<Routes> route){
        // Returns a start end stop through an arraylist with pos 0 being start
        // and pos 1 being end. using random math for spawn locations
        ArrayList<Stops> startEnd = new ArrayList<>();
        Stops startpoint;
        Stops endpoint;
        int randomRoute = (int) (Math.random() * (route.size() - 1)) ;
        Routes routes;
        // keeps start and stop on the same route to avoid swaping
        routes = route.get(randomRoute);
        int randomStart = (int) (Math.random() * (routes.stations.size() - 1)) + 0;
        int randomEnd = (int) (Math.random() * (routes.stations.size() -  1)) + 0;

        startpoint = routes.stations.get(randomStart);
        endpoint = routes.stations.get(randomEnd);
        startEnd.add(startpoint);
        startEnd.add(endpoint);
        return startEnd;

    }

    public String getName() {
        return name;
    }

    // Example passenger commuting between Jubilee and Percy Port.
    public static Passenger pass1() {
        return new Passenger("pass1");
    }

    // Boards this passenger when curStop matches start, disembarks at end.
    public void checkBoarding(Stops curStop, ArrayList<Passenger> onBoard) {
        if (curStop.equals(start) && !onBoard.contains(this)) {
            onBoard.add(this);
        } else if (curStop.equals(end) && onBoard.contains(this)) {
            onBoard.remove(this);
        }
    }
}
