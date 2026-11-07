import java.util.List;
import java.util.Random;

// small generic helpers
public class GenericUtil {

    // the bigger of two values, T must have compareTo
    public static <T extends Comparable<T>> T max(T a, T b) {
        if (a.compareTo(b) > 0) {
            return a;
        }
        return b;
    }

    // random item from any list
    public static <T> T randomItem(List<T> items, Random random) {
        return items.get(random.nextInt(items.size()));
    }

    // holds two values of any type, for example a label and its value
    public static class Pair<T, U> {

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
}
