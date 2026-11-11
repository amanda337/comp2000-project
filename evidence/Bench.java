import java.io.File;
import java.util.*;
import java.util.stream.*;
import javax.imageio.ImageIO;

// times each version 30 times after a warm-up and prints the median
public class Bench {
    static double median(List<Double> v) { Collections.sort(v); return v.get(v.size() / 2); }
    public static void main(String[] a) throws Exception {
        // four trains, sequential vs parallel
        Vehicles[] tr = {Train.t1(), Train.t2(), Train.t3(), Train.t4()};
        long[] sink = new long[1];
        Runnable seq = () -> sink[0] += Arrays.stream(tr).filter(t -> t.getName().length() > 1).mapToInt(t -> t.getName().length()).sum();
        Runnable par = () -> sink[0] += Arrays.stream(tr).parallel().filter(t -> t.getName().length() > 1).mapToInt(t -> t.getName().length()).sum();
        for (Runnable r : new Runnable[]{seq, par}) for (int i = 0; i < 20000; i++) r.run();   // warm up
        List<Double> s = new ArrayList<>(), p = new ArrayList<>();
        for (int trial = 0; trial < 30; trial++) {
            long t0 = System.nanoTime(); for (int i = 0; i < 2000; i++) seq.run(); s.add((System.nanoTime() - t0) / 2000.0);
            t0 = System.nanoTime(); for (int i = 0; i < 2000; i++) par.run(); p.add((System.nanoTime() - t0) / 2000.0);
        }
        System.out.printf("1. four trains, per call, median of 30 trials of 2,000 calls: sequential %.0f ns, parallel %.0f ns, parallel is %.1f times slower%n", median(s), median(p), median(p) / median(s));

        // the three pictures, sequential vs parallel (ColdLoad does the first-load case)
        File[] f = {new File("asserts/1.png"), new File("asserts/home.png"), new File("asserts/ins.png")};
        Runnable ls = () -> { for (File x : f) try { ImageIO.read(x); } catch (Exception e) { } };
        Runnable lp = () -> Arrays.stream(f).parallel().forEach(x -> { try { ImageIO.read(x); } catch (Exception e) { } });
        for (int i = 0; i < 5; i++) { ls.run(); lp.run(); }
        List<Double> a1 = new ArrayList<>(), a2 = new ArrayList<>();
        for (int trial = 0; trial < 30; trial++) {
            long t0 = System.nanoTime(); ls.run(); a1.add((System.nanoTime() - t0) / 1e6);
            t0 = System.nanoTime(); lp.run(); a2.add((System.nanoTime() - t0) / 1e6);
        }
        System.out.printf("2. three pictures, median of 30 trials after warm-up: sequential %.1f ms, parallel %.1f ms%n", median(a1), median(a2));
    }
}
