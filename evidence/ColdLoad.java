import java.io.File;
import java.util.Arrays;
import javax.imageio.ImageIO;

// loads the three pictures once in a new JVM
public class ColdLoad {
    public static void main(String[] a) throws Exception {
        File[] f = {new File("asserts/1.png"), new File("asserts/home.png"), new File("asserts/ins.png")};
        long t0 = System.nanoTime();
        if (a[0].equals("parallel")) Arrays.stream(f).parallel().forEach(x -> { try { ImageIO.read(x); } catch (Exception e) { } });
        else Arrays.stream(f).forEach(x -> { try { ImageIO.read(x); } catch (Exception e) { } });
        System.out.printf("%.1f%n", (System.nanoTime() - t0) / 1e6);
    }
}
