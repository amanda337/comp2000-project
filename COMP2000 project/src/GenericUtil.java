import java.util.List;
import java.util.Random;

// Small generics helpers. "T extends Comparable<T>" is a bounded type
// parameter: it means T can be any type, as long as that type implements
// Comparable<T> - which guarantees a.compareTo(b) exists to call.
public class GenericUtil {

    public static <T extends Comparable<T>> T max(T a, T b) {
        if (a.compareTo(b) > 0) {
            return a;
        }
        return b;
    }

    // A random item from any list (weather types, stations, trains...), so the
    // "items.get(random.nextInt(items.size()))" line is written once.
    public static <T> T randomItem(List<T> items, Random random) {
        return items.get(random.nextInt(items.size()));
    }
}
