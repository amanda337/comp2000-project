import java.lang.reflect.*;
import java.util.*;

// weather and fire checks, run on the old and the fixed version
public class BugCheck {
    static Object get(Object o, String n) throws Exception { Field f = o.getClass().getDeclaredField(n); f.setAccessible(true); return f.get(o); }
    static void set(Object o, String n, Object v) throws Exception { Field f = o.getClass().getDeclaredField(n); f.setAccessible(true); f.set(o, v); }
    static Vehicles[] fresh() { return new Vehicles[]{Train.t1(), Train.t2(), Train.t3(), Train.t4()}; }

    public static void main(String[] a) throws Exception {
        // weather changes over 3,000 days
        Time t = new Time(e -> {});
        Vehicles[] tr = fresh();
        for (long i = 0; i < 5L * 288 * 3000; i++) {
            try { t.advance(); } catch (SimulationTimeException e) { }
            if (t.ticks % 5 == 0) t.applyEvents(tr);
        }
        List<Weather.Type> h = (List<Weather.Type>) get(t.getWeather(), "history");
        int repeats = 0, clash = 0, sunnyOver = 0;
        for (int i = 1; i < h.size(); i++) {
            if (h.get(i) == h.get(i - 1)) repeats++;
            boolean rf = (h.get(i) == Weather.Type.RAIN && h.get(i - 1) == Weather.Type.FOG);
            boolean fr = (h.get(i) == Weather.Type.FOG && h.get(i - 1) == Weather.Type.RAIN);
            if (rf || fr) clash++;
        }
        System.out.println("1. weather: " + h.size() + " spells, same weather twice in a row = " + repeats + ", rain<->fog directly = " + clash);

        // how big a large fire at Central gets
        Method spread = Fire.class.getDeclaredMethod("spread", Fire.Size.class, Stops.class, Vehicles[].class);
        spread.setAccessible(true);
        Map<Integer, Integer> dist = new TreeMap<>();
        for (int i = 0; i < 20000; i++) {
            Fire f = new Fire();
            Set<Stops> burning = (Set<Stops>) get(f, "burning");
            burning.add(Stops.Central);
            spread.invoke(f, Fire.Size.LARGE, Stops.Central, fresh());
            dist.merge(burning.size(), 1, Integer::sum);
        }
        System.out.println("2. large fire, stations burning (20,000 fires): " + dist);

        // fire timer after rain
        Weather sunny = new Weather(), rain = new Weather();
        Field tf = Weather.class.getDeclaredField("type"); tf.setAccessible(true);
        tf.set(sunny, Weather.Type.SUNNY); tf.set(rain, Weather.Type.RAIN);
        Fire f = new Fire();
        set(f, "changesWithoutFire", 10);
        f.maybeStart(sunny, fresh(), 50000);
        Object spell = get(f, "spell");
        Object before = get(spell, "endSeconds");
        f.maybeStart(rain, fresh(), 60000);
        Object after = get(spell, "endSeconds");
        System.out.println("3. fire timer: end time while burning = " + before + ", after rain = " + after + "  (burning text now: " + f.getText() + ")");
        Runtime.getRuntime().halt(0);
    }
}
