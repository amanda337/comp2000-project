import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;

// Loads background pictures from the asserts folder. A terminal, an IDE run
// button and a different folder all start with a different working directory,
// so each likely folder is tried in turn. A missing or unreadable picture just
// means no background instead of a crash.
//
// Pictures are cached, and preload() reads them on a background thread while
// the Home screen is showing, so Panel finds them ready when it opens. The
// cache is used by that thread and by the UI thread, so it is a
// ConcurrentHashMap (see load() for the race this avoids).
public class Assets {

    private static final String[] SEARCH_FOLDERS = {
        "asserts/",
        "bin/asserts/",
        "../asserts/",
        "COMP2000 project/asserts/",
        "COMP2000 project/bin/asserts/"
    };

    private static final Map<String, Image> cache = new ConcurrentHashMap<>();

    // Counts real disk reads. An AtomicInteger increments safely from any
    // thread; a plain int++ is read-add-write and can lose updates.
    private static final AtomicInteger diskReads = new AtomicInteger();

    static int diskReads() {
        return diskReads.get();
    }

    // computeIfAbsent checks and reads in one atomic step. With a plain
    // "if not in map, read and put" two threads asking for the same file at
    // once would both read it, and each would end up holding a different copy.
    public static Image load(String fileName) {
        return cache.computeIfAbsent(fileName, Assets::readFromDisk);
    }

    // Starts one background thread that loads the files in order. The gain is
    // moving the ~60 ms of decoding off the UI thread, not parallel speed: a
    // parallel stream over these three small files measured no faster (about
    // 70 ms against 64 ms). Daemon, so it never keeps the program alive on exit.
    public static void preload(String... fileNames) {
        Thread thread = new Thread(
                () -> Arrays.stream(fileNames).forEach(Assets::load),
                "asset-preload");
        thread.setDaemon(true);
        thread.start();
    }

    // Returns null when the file is missing or unreadable (and so is not cached).
    private static Image readFromDisk(String fileName) {
        File file = Arrays.stream(SEARCH_FOLDERS)
                .map(folder -> new File(folder + fileName))
                .filter(File::exists)
                .findFirst()
                .orElse(null);

        if (file == null) {
            System.out.println("Could not find " + fileName + " in any asserts folder.");
            return null;
        }
        try {
            diskReads.incrementAndGet();
            return ImageIO.read(file);
        } catch (IOException ex) {
            System.out.println("Could not read " + file.getPath() + ": " + ex.getMessage());
            return null;
        }
    }
}
