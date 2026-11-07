import java.util.List;
import java.util.Random;

// The project's small generics, in one file: the helper methods in GenericUtil
// and the Pair class below.
//
// "T extends Comparable<T>" is a bounded type parameter: it means T can be any
// type, as long as that type implements Comparable<T> - which guarantees
// a.compareTo(b) exists to call.
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

// A generic pair of two values that can be different types, e.g. a label and
// its value. T and U are type parameters: Pair<String, String> makes both
// getFirst() and getSecond() return String, Pair<String, Integer> makes
// getSecond() return an Integer, checked at compile time.
class Pair<T, U> {

    private T first;
    private U second;

    public Pair(T first, U second) {
        this.first = first;
        this.second = second;
    }

    public T getFirst() {
        return first;
    }

    public U getSecond() {
        return second;
    }
}
