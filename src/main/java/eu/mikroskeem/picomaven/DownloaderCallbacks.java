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
     * @param exception IOException
     * @see Dependency
     * @see IOException
     */
    void onFailure(Dependency dependency, IOException exception);
}
