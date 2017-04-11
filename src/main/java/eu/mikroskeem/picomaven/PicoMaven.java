package eu.mikroskeem.picomaven;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import okhttp3.OkHttpClient;
import sun.plugin.dom.exception.InvalidStateException;

import java.io.Closeable;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * PicoMaven
 *
 * @author Mark Vainomaa
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class PicoMaven implements Closeable {
    private final Path downloadPath;
    private final List<Dependency> dependencyList;
    private final List<URI> repositoryUris;
    private final OkHttpClient httpClient;
    private final ExecutorService executorService;
    private final boolean shouldCloseExecutorService;

    public void downloadAll() {
        /* Iterate through all dependencies */
        dependencyList.forEach(dependency -> {
            /* Iterate through every repository */
            for (URI repositoryUri : repositoryUris) {
                URI groupMetaUri = UrlUtils.buildGroupMetaURI(repositoryUri, dependency);
                break;
            }
        });
    }

    @Override
    @SneakyThrows(InterruptedException.class)
    public void close() {
        if(shouldCloseExecutorService) {
            executorService.shutdown();
            executorService.awaitTermination(150, TimeUnit.MILLISECONDS);
        }
    }

    public static class Builder {
        private Path downloadPath = null;
        private OkHttpClient httpClient = null;
        private List<Dependency> dependencies = null;
        private List<URI> repositories = null;
        private ExecutorService executorService = null;
        private boolean shouldCloseExecutorService = false;

        public Builder withDownloadPath(@NonNull Path path) {
            this.downloadPath = path;
            return this;
        }

        public Builder withOkHttpClient(OkHttpClient client) {
            this.httpClient = client;
            return this;
        }

        public Builder withDependencies(List<Dependency> dependencies) {
            this.dependencies = Collections.unmodifiableList(dependencies);
            return this;
        }

        public Builder withRepositories(List<URI> repositories) {
            this.repositories = Collections.unmodifiableList(repositories);
            return this;
        }

        public Builder withExecutorService(ExecutorService executorService) {
            this.executorService = executorService;
            return this;
        }

        public Builder shouldCloseExecutorService(boolean value) {
            this.shouldCloseExecutorService = value;
            return this;
        }

        public PicoMaven build() {
            if(downloadPath == null) throw new InvalidStateException("Download path cannot be unset!");
            if(dependencies == null) dependencies = Collections.emptyList();
            if(repositories == null) repositories = Collections.emptyList();
            if(httpClient == null) httpClient = new OkHttpClient();
            if(executorService == null) {
                executorService = Executors.newCachedThreadPool();
                shouldCloseExecutorService = true;
            }
            return new PicoMaven(downloadPath, dependencies, repositories, httpClient,
                    executorService, shouldCloseExecutorService);
        }
    }
}
