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
    default void onFailure(Dependency dependency, Exception exception) {
        try {
            java.lang.reflect.Method method = this.getClass().getMethod("onFailure", Dependency.class, IOException.class);
            if(method.isDefault()) throw new AbstractMethodError();

            /* Pass execution to #onFailure(Dependency, IOException) */
            onFailure(dependency, new IOException(exception));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Invoked when dependency download fails
     *
     * @param dependency Dependency object
     * @param exception IOException
     * @see Dependency
     * @see IOException
     * @deprecated Use and override {@link #onFailure(Dependency, Exception)} instead
     */
    @Deprecated
    default void onFailure(Dependency dependency, IOException exception) {
        onFailure(dependency, (Exception) exception);
    }
}
