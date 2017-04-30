package eu.mikroskeem.picomaven;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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
    private final DownloaderCallbacks downloaderCallbacks;
    private final DebugLoggerImpl logger;
    private final boolean shouldCloseExecutorService;

    private final Queue<Dependency> downloadedDependencies = new ConcurrentLinkedQueue<>();
    private final List<Callable<Void>> downloadTasks = new ArrayList<>();

    /**
     * Download all dependencies from configured repositories
     *
     * @return List of {@link Path}s pointing to dependencies stored on filesystem
     * @throws InterruptedException thrown by {@link ExecutorService#invokeAll(Collection)}
     */
    public List<Path> downloadAll() throws InterruptedException {
        /* Iterate through all dependencies */
        for(Dependency dependency : dependencyList) {
            logger.debug("Trying to download dependency %s", dependency);
            Callable<Void> task = () -> {
                Path theDownloadPath = UrlUtils.formatLocalPath(downloadPath, dependency);
                logger.debug("%s path: %s", dependency, theDownloadPath);
                if(!Files.exists(theDownloadPath)) {
                    /* Iterate through every repository */
                    try {
                        for (URI repositoryUri : repositoryUris) {
                            logger.debug("Trying repository %s for %s", repositoryUri, dependency);
                            Metadata metadata = null;
                            Metadata artifactMetadata = null;
                            URI groupMetaURI = UrlUtils.buildGroupMetaURI(repositoryUri, dependency);
                            logger.debug("%s group meta URI: %s", dependency, groupMetaURI);

                            /* Try to parse repository meta */
                            metadata = DataProcessor.getMetadata(httpClient, groupMetaURI);
                            if (metadata != null) {
                                URI artifactMetaURI = UrlUtils.buildArtifactMetaURI(repositoryUri, metadata, dependency);
                                logger.debug("%s artifact meta URI: %s", dependency, artifactMetaURI);
                                artifactMetadata = DataProcessor.getMetadata(httpClient, artifactMetaURI);
                            }

                            /* Build artifact url */
                            URI artifactJarURI = UrlUtils.buildArtifactJarURI(repositoryUri, artifactMetadata, dependency);
                            logger.debug("Downloading %s from %s", dependency, artifactJarURI);
                            Request request = new Request.Builder().url(HttpUrl.get(artifactJarURI)).build();
                            try (Response artifactJarResponse = httpClient.newCall(request).execute()) {
                                if (artifactJarResponse.isSuccessful()) {
                                    Path parentPath = theDownloadPath.getParent();
                                    if (!Files.exists(parentPath)) Files.createDirectories(parentPath);
                                    Files.copy(artifactJarResponse.body().byteStream(), theDownloadPath,
                                            StandardCopyOption.REPLACE_EXISTING);
                                    /* Download success! */
                                    logger.debug("%s download succeeded!", dependency);
                                    downloadedDependencies.add(dependency);
                                    break;
                                } else {
                                    logger.debug("%s download failed!", dependency);
                                }
                            }
                        }
                        if(downloadedDependencies.contains(dependency)) {
                            if(downloaderCallbacks != null) downloaderCallbacks.onSuccess(dependency, theDownloadPath);
                        } else {
                            IOException exception = new IOException("Not found");
                            if(downloaderCallbacks != null) downloaderCallbacks.onFailure(dependency, exception);
                        }
                    } catch (IOException e) {
                        if(downloaderCallbacks != null) downloaderCallbacks.onFailure(dependency, e);
                    }
                } else {
                    logger.debug("%s is already downloaded", dependency);
                    downloadedDependencies.add(dependency);
                }
                return null; /* What? */
            };
            downloadTasks.add(task);
        }
        /* Execute them all */
        executorService.invokeAll(downloadTasks);

        /* Convert to list */
        return downloadedDependencies.stream()
                .map(dep -> UrlUtils.formatLocalPath(downloadPath, dep))
                .collect(Collectors.toList());
    }

    /**
     * Shuts down {@link ExecutorService}, if configured
     */
    @Override
    @SneakyThrows(InterruptedException.class)
    public void close() {
        if(shouldCloseExecutorService) {
            executorService.shutdown();
            executorService.awaitTermination(150, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * {@link PicoMaven} builder
     */
    public static class Builder {
        private Path downloadPath = null;
        private OkHttpClient httpClient = null;
        private List<Dependency> dependencies = null;
        private List<URI> repositories = null;
        private ExecutorService executorService = null;
        private DownloaderCallbacks downloaderCallbacks = null;
        private DebugLoggerImpl loggerImpl = null;
        private boolean shouldCloseExecutorService = false;

        /**
         * Set download path
         *
         * @param path Path (non-null)
         * @return this (for chaining)
         */
        public Builder withDownloadPath(@NonNull Path path) {
            this.downloadPath = path;
            return this;
        }

        /**
         * Set {@link OkHttpClient} instance what'll be used in this {@link PicoMaven} instance
         *
         * @param client {@link OkHttpClient} instance
         * @return this (for chaining)
         */
        public Builder withOkHttpClient(OkHttpClient client) {
            this.httpClient = client;
            return this;
        }

        /**
         * Set depencencies list, what to download
         *
         * @param dependencies List of {@link Dependency}
         * @return this (for chaining)
         */
        public Builder withDependencies(List<Dependency> dependencies) {
            this.dependencies = Collections.unmodifiableList(dependencies);
            return this;
        }

        /**
         * Set repositories list, where to look up dependencies
         *
         * @param repositories List of repository {@link URI}s
         * @return this (for chaining)
         */
        public Builder withRepositories(List<URI> repositories) {
            this.repositories = Collections.unmodifiableList(repositories);
            return this;
        }

        /**
         * Set {@link ExecutorService} what to use to launch downloader tasks
         *
         * @param executorService {@link ExecutorService} instance
         * @return this (for chaining)
         */
        public Builder withExecutorService(ExecutorService executorService) {
            this.executorService = executorService;
            return this;
        }

        /**
         * Set {@link DownloaderCallbacks} to get information about downloads
         *
         * @param downloaderCallbacks Implementation of {@link DownloaderCallbacks}
         * @return this (for chaining)
         */
        public Builder withDownloaderCallbacks(DownloaderCallbacks downloaderCallbacks){
            this.downloaderCallbacks = downloaderCallbacks;
            return this;
        }

        /**
         * Set {@link DebugLoggerImpl} implementing logger instance
         *
         * @param loggerImpl Logger instance
         * @return this (for chaining)
         */
        public Builder withDebugLoggerImpl(DebugLoggerImpl loggerImpl) {
            this.loggerImpl = loggerImpl;
            return this;
        }

        /**
         * Set whether {@link ExecutorService} should be shut down or not after {@link PicoMaven} close.
         *
         * @param value Boolean
         * @return this (for chaining)
         */
        public Builder shouldCloseExecutorService(boolean value) {
            this.shouldCloseExecutorService = value;
            return this;
        }

        /**
         * Build {@link PicoMaven} instance
         *
         * @return Instance of {@link PicoMaven}
         */
        public PicoMaven build() {
            if(downloadPath == null) throw new IllegalStateException("Download path cannot be unset!");
            if(dependencies == null) dependencies = Collections.emptyList();
            if(repositories == null) repositories = Collections.emptyList();
            if(httpClient == null) httpClient = new OkHttpClient();
            if(loggerImpl == null) loggerImpl = DebugLoggerImpl.DummyDebugLogger.INSTANCE;
            if(executorService == null) {
                executorService = Executors.newCachedThreadPool(new ThreadFactory() {
                    private final AtomicInteger THREAD_COUNTER = new AtomicInteger(0);
                    @Override
                    public Thread newThread(Runnable runnable) {
                        Thread thread = new Thread(runnable);
                        thread.setName("PicoMaven downloader thread " + THREAD_COUNTER.getAndIncrement());
                        return thread;
                    }
                });
                shouldCloseExecutorService = true;
            }
            return new PicoMaven(downloadPath, dependencies, repositories, httpClient,
                    executorService, downloaderCallbacks, loggerImpl, shouldCloseExecutorService);
        }
    }
}
