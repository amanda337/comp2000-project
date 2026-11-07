import java.util.ArrayList;
//import java.util.function.Consumer;  // only used by the unused code below

public class Passenger {
    // a commuter with a random start and end stop on one line

    private String name;
    private Stops start;
    private Stops end;

    // picks a random start and end stop
    public Passenger(String name) {
        this.name = name;
        this.start = startPoint(allRoutes()).get(0);
        this.end = startPoint(allRoutes()).get(1);
    }

    public static ArrayList<Stops.Routes> allRoutes(){
        // all four train lines in one list
        ArrayList<Stops.Routes> totalRoutes = new ArrayList<>();
        totalRoutes.add(Stops.Routes.redLine());
        totalRoutes.add(Stops.Routes.blueLine());
        totalRoutes.add(Stops.Routes.purpleLine());
        totalRoutes.add(Stops.Routes.greenLine());
        return totalRoutes;
    }

    // unused: only picks a start and end, no passenger is created from it yet
    // public static void spawnPassengers(){
    //     startPoint(allRoutes());
    // }
    // Consumer<Time> spawnPassengers = time ->{
    //     if(time.ticks % 60 == 0){
    //         for(int i =0;i<100;i++){
    //             startPoint(allRoutes());
    //         }
    //     }
    //     };

    public static ArrayList<Stops> startPoint(ArrayList<Stops.Routes> route){
        // random start (position 0) and end (position 1) stop
        ArrayList<Stops> startEnd = new ArrayList<>();
        Stops startpoint;
        Stops endpoint;
        int randomRoute = (int) (Math.random() * (route.size() - 1)) ;
        Stops.Routes routes;
        // start and end are on the same line
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

    // the one passenger the simulation uses
    public static Passenger pass1() {
        return new Passenger("pass1");
    }

    // gets on at the start stop and off at the end stop
    public void checkBoarding(Stops curStop, ArrayList<Passenger> onBoard) {
        if (curStop.equals(start) && !onBoard.contains(this)) {
            onBoard.add(this);
        } else if (curStop.equals(end) && onBoard.contains(this)) {
            onBoard.remove(this);
        }
    }
}
