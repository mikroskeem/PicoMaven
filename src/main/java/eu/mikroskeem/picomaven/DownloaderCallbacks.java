package eu.mikroskeem.picomaven;

import java.io.IOException;
import java.nio.file.Path;

/**
 * @author Mark Vainomaa
 */
public interface DownloaderCallbacks {
    /**
     * Invoked when dependency download succeeds
     *
     * @param dependency Dependency object
     * @param dependencyPath Dependency path
     * @see Dependency
     * @see Path
     */
    void onSuccess(Dependency dependency, Path dependencyPath);

    /**
     * Invoked when dependency download fails
     *
     * @param dependency Dependency object
     * @param exception Exception
     * @see Dependency
     * @see Exception
     */
    void onFailure(Dependency dependency, Exception exception);

    /**
     * Invoked when dependency download fails
     *
     * @param dependency Dependency object
     * @param exception IOException
     * @see Dependency
     * @see IOException
     * @deprecated Use {@link #onFailure(Dependency, Exception)} instead
     */
    @Deprecated
    default void onFailure(Dependency dependency, IOException exception) {
        onFailure(dependency, (Exception) exception);
    }
}
