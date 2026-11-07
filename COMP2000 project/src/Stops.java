import java.util.ArrayList;
import java.util.Objects;
import java.util.function.IntSupplier;

public class Stops {
    int x;
    int y;
    String name;
    int capacity;
    int totalComuters = 0;
    boolean onFire = false; // set by Fire; Vehicles draws a flame at a burning stop

    public Stops(int x, int y, String name, int capacity) {
        this.x = x;
        this.y = y;
        this.name = name;
        this.capacity = capacity;
    }

    // Named stops with pixel positions matching where Panel draws them.
    public static final Stops TownHall = new Stops(640, 60, "Town Hall", 100);
    public static final Stops Eagleston = new Stops(640, 160, "Eagleston", 50);
    public static final Stops Rosebury = new Stops(640, 260, "Rosebury", 50);
    public static final Stops Central= new Stops(640, 360, "Central", 100);
    public static final Stops Jubilee = new Stops(740, 360, "Jubilee", 50);
    public static final Stops Bakerville = new Stops(840, 360, "Bakerville", 50);
    public static final Stops PercyPort = new Stops(940, 360, "Percy Port", 50);
    public static final Stops Merrybrook = new Stops(1040, 460, "Merrybrook", 50);
    public static final Stops Brookchester = new Stops(1040, 560, "Brookchester", 50);
    

    public static final Stops SunsetPoint = new Stops(1040, 60, "Sunset Point", 50);
    public static final Stops DaisyHill = new Stops(1040, 160, "Daisy Hill", 50);
    public static final Stops Reeds = new Stops(1040, 260, "Reeds", 50);
    public static final Stops SherieGrove = new Stops(540, 360, "Sherie Grove", 50);
    public static final Stops Prudence = new Stops(440, 360, "Prudence", 50);

    public static final Stops MountPresley = new Stops(240, 160, "Mount Presley", 50);
    public static final Stops AndiePark = new Stops(340, 260, "Andie Park", 50);

    public static final Stops TigerBay = new Stops(440, 460, "Tiger Bay", 50);
    public static final Stops TrollUponBridge = new Stops(440, 560, "Troll-upon-Bridge", 100);
    public static final Stops Celeste = new Stops(340, 660, "Celeste", 50);
    public static final Stops Hailstone = new Stops(240, 660, "Hailstone", 50);
    public static final Stops WindyJunction = new Stops(140, 660, "Windy Junction", 50);



    public String getName() {
        return name;
    }

    // public int checkCapacity() {
    //     int boardingTime = 0;
    //     if (totalComuters >= capacity / 3) {
    //         boardingTime = 5; // a busy stop takes a little longer to board
    //     }
    //     return boardingTime;
    // }   
    IntSupplier checkCapacity = () -> (totalComuters >= capacity / 3) ? 5 : 0;


    // Same name means same station, so curStop.equals(end) works reliably.
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Stops)) return false;
        return name.equals(((Stops) obj).name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return name;
    }
}

class Routes {
    String name;
    ArrayList<Stops> stations;

    public Routes(String name, ArrayList<Stops> stations) {
        this.name = name;
        this.stations = stations;
    }

    // The full red line, running past Percy Port to its actual end.
    public static Routes redLine() {
        ArrayList<Stops> stops = new ArrayList<>();
        stops.add(Stops.TownHall);
        stops.add(Stops.Eagleston);
        stops.add(Stops.Rosebury);
        stops.add(Stops.Central);
        stops.add(Stops.Jubilee);
        stops.add(Stops.Bakerville);
        stops.add(Stops.PercyPort);
        stops.add(Stops.Merrybrook);
        stops.add(Stops.Brookchester);
        return new Routes("Red Line", stops);
    }
    public static Routes blueLine() {
        ArrayList<Stops> stops = new ArrayList<>();
        stops.add(Stops.SunsetPoint);
        stops.add(Stops.DaisyHill);
        stops.add(Stops.Reeds);
        stops.add(Stops.PercyPort);
        stops.add(Stops.Bakerville);
        stops.add(Stops.Jubilee);
        stops.add(Stops.Central);
        stops.add(Stops.SherieGrove);
        stops.add(Stops.Prudence);
        stops.add(Stops.TigerBay);
        stops.add(Stops.TrollUponBridge);
        stops.add(Stops.Celeste);
        stops.add(Stops.Hailstone);
        stops.add(Stops.WindyJunction);
        return new Routes("Blue Line", stops);
    }
        public static Routes purpleLine() {
        ArrayList<Stops> stops = new ArrayList<>();
        stops.add(Stops.TownHall);
        stops.add(Stops.Eagleston);
        stops.add(Stops.Rosebury);
        stops.add(Stops.Central);
        stops.add(Stops.SherieGrove);
        stops.add(Stops.Prudence);
        stops.add(Stops.TigerBay);
        stops.add(Stops.TrollUponBridge);
        return new Routes("Purple Line", stops);
    }
    public static Routes greenLine() {
        ArrayList<Stops> stops = new ArrayList<>();
        stops.add(Stops.Central);
        stops.add(Stops.SherieGrove);
        stops.add(Stops.AndiePark);
        stops.add(Stops.MountPresley);
        return new Routes("Green Line", stops);
    }
}
