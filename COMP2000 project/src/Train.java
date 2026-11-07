public class Train extends Vehicles{

    public Train(){
        super();
    }

    // start is not used, a train starts at the first stop of its line
    public Train(int capacity, String name, Stops.Routes route, Stops start){
        super(capacity, name, route);
    }

    // one factory per train, each on its own line
    public static Train t1() {
        return new Train(50, "T1", Stops.Routes.redLine(), Stops.Brookchester);
    }
    public static Train t2() {
        return new Train(50, "T2", Stops.Routes.blueLine(), Stops.SunsetPoint);
    }
    public static Train t3(){
        return new Train(50, "T3", Stops.Routes.purpleLine(), Stops.TownHall);
    }
    public static Train t4(){
        return new Train(50, "T4", Stops.Routes.greenLine(), Stops.MountPresley);
    }
}
