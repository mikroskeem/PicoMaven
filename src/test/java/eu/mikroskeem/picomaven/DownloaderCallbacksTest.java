package eu.mikroskeem.picomaven;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

/**
 * @author Mark Vainomaa
 */
@SuppressWarnings("deprecation")
public class DownloaderCallbacksTest {
    /*
     * Leaves onFailure's both variants unimplemented
     */
    @Test
    public void testBadImplementation() {
        class BadImpl implements DownloaderCallbacks {
            @Override
            public void onSuccess(Dependency dependency, Path dependencyPath) {

            }
        }

        DownloaderCallbacks callbacks = new BadImpl();
        Assertions.assertThrows(AbstractMethodError.class, () -> {
            callbacks.onFailure(null, new Exception("foo"));
        });
        Assertions.assertThrows(AbstractMethodError.class, () -> {
            callbacks.onFailure(null, new IOException("bar"));
        });
    }

    /*
     * Implements only deprecated onFailure method (old code)
     */
    @Test
    public void testDeprecatedImplementation() {
        class DeprecatedImpl implements DownloaderCallbacks {
            @Override
            public void onSuccess(Dependency dependency, Path dependencyPath) {

            }

            @Override
            public void onFailure(Dependency dependency, IOException exception) {

            }
        }

        DownloaderCallbacks callbacks = new DeprecatedImpl();
        callbacks.onFailure(null, new Exception("foo"));
        callbacks.onFailure(null, new IOException("bar"));
    }

    /*
     * Implements new onFailue method
     */
    @Test
    public void testGoodImplementation() {
        class GoodImpl implements DownloaderCallbacks {
            @Override
            public void onSuccess(Dependency dependency, Path dependencyPath) {

            }

            @Override
            public void onFailure(Dependency dependency, Exception exception) {

            }
        }

        DownloaderCallbacks callbacks = new GoodImpl();
        callbacks.onFailure(null, new Exception("foo"));
        callbacks.onFailure(null, new IOException("bar"));
    }
}
