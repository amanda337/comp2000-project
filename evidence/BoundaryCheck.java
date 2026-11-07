import java.lang.reflect.*;

// checks the service hours at the edge times
public class BoundaryCheck {
    public static void main(String[] a) throws Exception {
        // the clock starts at 06:00, so after midnight add 24 hours
        Object[][] cases = {
            {"05:59:59", 23 * 3600 + 3599L, false},
            {"06:00:00", 0L,                true},
            {"06:00:01", 1L,                true},
            {"12:00:00", 6 * 3600L,         true},
            {"23:59:59", 18 * 3600 - 1L,    true},
            {"00:00:00", 18 * 3600L,        true},
            {"02:59:59", 21 * 3600 - 1L,    true},
            {"03:00:00", 21 * 3600L,        false},
            {"03:00:01", 21 * 3600 + 1L,    false},
            {"04:30:00", 22 * 3600 + 1800L, false},
        };
        int failed = 0;
        for (Object[] c : cases) {
            Time t; Vehicles[] tr; boolean fog;
            int attempts = 0;
            do {   // weather is random, try again if it is fog because fog stops the trains too
                t = new Time(e -> {});
                t.simSeconds = (Long) c[1];
                tr = new Vehicles[]{Train.t1(), Train.t2(), Train.t3(), Train.t4()};
                t.applyEvents(tr);
                fog = t.getWeather().stopsTrains();
            } while (fog && ++attempts < 200);
            boolean service = t.isServiceHours();
            int stopped = 0;
            for (Vehicles v : tr) if (v.isStopped()) stopped++;
            boolean ok = service == (Boolean) c[2] && (fog || (service ? stopped == 0 : stopped == 4));
            if (!ok) failed++;
            System.out.println(c[0] + "  expected service=" + c[2] + "  actual service=" + service + "  trains stopped=" + stopped + "/4" + (fog ? " (fog, ignored)" : "") + "  " + (ok ? "PASS" : "FAIL"));
        }
        System.out.println(cases.length + " cases, " + failed + " failed");
        Runtime.getRuntime().halt(0);
    }
}
