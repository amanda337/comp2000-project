import java.lang.reflect.*;

// runs the window at 4x past the end of service and counts what the trains show
public class ServiceCheck {
    public static void main(String[] a) throws Exception {
        long start = Long.parseLong(a[0]);
        long runSeconds = Long.parseLong(a[1]);
        Panel p = new Panel();
        Thread.sleep(800);
        Field tf = Panel.class.getDeclaredField("time"); tf.setAccessible(true);
        Time time = (Time) tf.get(p);
        Vehicles[] tr = {p.train1, p.train2, p.train3, p.train4};
        java.awt.EventQueue.invokeAndWait(() -> { time.simSeconds = start; time.setSpeed(4); time.toggle(); });
        int[] last = new int[4];
        for (int i = 0; i < 4; i++) last[i] = tr[i].stationIndex;
        int offFrames = 0, trainSamples = 0, shownRunning = 0, moves = 0;
        long end = System.currentTimeMillis() + runSeconds * 1000;
        while (System.currentTimeMillis() < end) {
            Thread.sleep(15);
            boolean on = time.isServiceHours();
            for (int i = 0; i < 4; i++) {
                if (tr[i].stationIndex != last[i]) {
                    if (!on) moves++;
                    last[i] = tr[i].stationIndex;
                }
            }
            if (!on) {
                offFrames++;
                for (Vehicles v : tr) {
                    trainSamples++;
                    if (!v.isStopped() && !v.isDelayed()) shownRunning++;   // would show Running
                }
            }
        }
        System.out.println("frames with service off: " + offFrames + ", train moves while off: " + moves
                + ", train samples shown as running (no stop sign, not delayed): " + shownRunning + " of " + trainSamples
                + " (" + (trainSamples == 0 ? 0 : 100 * shownRunning / trainSamples) + "%)");
        Runtime.getRuntime().halt(0);
    }
}
