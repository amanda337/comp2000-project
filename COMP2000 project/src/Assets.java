import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;

// loads the background pictures, tries each likely folder because the
// working folder is different when run from a terminal or an IDE.
// a missing picture gives null (no background) instead of a crash.
// pictures are cached and read on a background thread while Home shows,
// so the cache is a ConcurrentHashMap (two threads use it).
public class Assets {

    private static final String[] SEARCH_FOLDERS = {
        "asserts/",
        "bin/asserts/",
        "../asserts/",
        "COMP2000 project/asserts/",
        "COMP2000 project/bin/asserts/"
    };

    private static final Map<String, Image> cache = new ConcurrentHashMap<>();

    // counts real disk reads, AtomicInteger stays correct when two threads add
    private static final AtomicInteger diskReads = new AtomicInteger();

    // how many files were really read from disk
    static int diskReads() {
        return diskReads.get();
    }

    // computeIfAbsent checks and reads in one step, so a file is never loaded twice
    public static Image load(String fileName) {
        return cache.computeIfAbsent(fileName, Assets::readFromDisk);
    }

    // loads the files one after another on a background thread so the screen does not wait
    public static void preload(String... fileNames) {
        Thread thread = new Thread(
                () -> Arrays.stream(fileNames).forEach(Assets::load),
                "asset-preload");
        thread.setDaemon(true);
        thread.start();
    }

    // null when the file is missing or cannot be read
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
