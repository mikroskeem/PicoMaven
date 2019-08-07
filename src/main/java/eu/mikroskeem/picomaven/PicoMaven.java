/*
 * This file is part of project PicoMaven, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2017-2019 Mark Vainomaa <mikroskeem@mikroskeem.eu>
 * Copyright (c) Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package eu.mikroskeem.picomaven;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * PicoMaven
 *
 * @author Mark Vainomaa
 */
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
     * @return Map of {@link Dependency}s and their respective {@link Path}s pointing to dependencies stored on filesystem
     * @throws InterruptedException thrown by {@link ExecutorService#invokeAll(Collection)}
     */
    @NonNull
    public Map<Dependency, Path> downloadAllArtifacts() throws InterruptedException {
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
                            try {
                                metadata = DataProcessor.getMetadata(httpClient, groupMetaURI);
                                if (metadata != null) {
                                    URI artifactMetaURI = UrlUtils.buildArtifactMetaURI(repositoryUri, metadata, dependency);
                                    logger.debug("%s artifact meta URI: %s", dependency, artifactMetaURI);
                                    artifactMetadata = DataProcessor.getMetadata(httpClient, artifactMetaURI);
                                }
                            } catch (IOException e) {
                                /* Skip repository */
                                if(downloaderCallbacks != null) downloaderCallbacks.onFailure(dependency, (Exception)e);
                                continue;
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
                            } catch (IOException e) {
                                if(downloaderCallbacks != null) downloaderCallbacks.onFailure(dependency, (Exception) e);
                            }
                        }
                        if(downloadedDependencies.contains(dependency)) {
                            if(downloaderCallbacks != null) downloaderCallbacks.onSuccess(dependency, theDownloadPath);
                        } else {
                            IOException exception = new IOException("Not found");
                            if(downloaderCallbacks != null) downloaderCallbacks.onFailure(dependency, (Exception) exception);
                        }
                    } catch (Exception e) {
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

        /* Build dependencies map */
        Map<Dependency, Path> dependencies = new LinkedHashMap<>();
        downloadedDependencies.forEach(dependency -> {
            dependencies.put(dependency, UrlUtils.formatLocalPath(downloadPath, dependency));
        });

        return Collections.unmodifiableMap(dependencies);
    }

    /**
     * Download all dependencies from configured repositories
     *
     * @return List of {@link Path}s pointing to dependencies stored on filesystem
     * @throws InterruptedException thrown by {@link ExecutorService#invokeAll(Collection)}
     * @deprecated Use {@link #downloadAllArtifacts()} instead. This method will be removed in new version
     */
    @NonNull
    @Deprecated
    public List<Path> downloadAll() throws InterruptedException {
        Map<Dependency, Path> dependencies = this.downloadAllArtifacts();
        return Collections.unmodifiableList(new ArrayList<>(dependencies.values()));
    }

    /**
     * Shuts down {@link ExecutorService}, if configured
     */
    @Override
    public void close() {
        if(shouldCloseExecutorService) {
            executorService.shutdown();
            try {
                executorService.awaitTermination(150, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private PicoMaven(Path downloadPath, List<Dependency> dependencyList, List<URI> repositoryUris, OkHttpClient
            httpClient, ExecutorService executorService, DownloaderCallbacks downloaderCallbacks, DebugLoggerImpl
            logger, boolean shouldCloseExecutorService) {
        this.downloadPath = downloadPath;
        this.dependencyList = dependencyList;
        this.repositoryUris = repositoryUris;
        this.httpClient = httpClient;
        this.executorService = executorService;
        this.downloaderCallbacks = downloaderCallbacks;
        this.logger = logger;
        this.shouldCloseExecutorService = shouldCloseExecutorService;
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
        @NonNull
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
        @NonNull
        public Builder withOkHttpClient(@Nullable OkHttpClient client) {
            this.httpClient = client;
            return this;
        }

        /**
         * Set depencencies list, what to download
         *
         * @param dependencies List of {@link Dependency}
         * @return this (for chaining)
         */
        @NonNull
        public Builder withDependencies(@NonNull List<Dependency> dependencies) {
            this.dependencies = Collections.unmodifiableList(dependencies);
            return this;
        }

        /**
         * Set repositories list, where to look up dependencies
         *
         * @param repositories List of repository {@link URI}s
         * @return this (for chaining)
         */
        @NonNull
        public Builder withRepositories(@NonNull List<URI> repositories) {
            this.repositories = Collections.unmodifiableList(repositories);
            return this;
        }

        /**
         * Set {@link ExecutorService} what to use to launch downloader tasks
         *
         * @param executorService {@link ExecutorService} instance
         * @return this (for chaining)
         */
        @NonNull
        public Builder withExecutorService(@Nullable ExecutorService executorService) {
            this.executorService = executorService;
            return this;
        }

        /**
         * Set {@link DownloaderCallbacks} to get information about downloads
         *
         * @param downloaderCallbacks Implementation of {@link DownloaderCallbacks}
         * @return this (for chaining)
         */
        @NonNull
        public Builder withDownloaderCallbacks(@Nullable DownloaderCallbacks downloaderCallbacks) {
            this.downloaderCallbacks = downloaderCallbacks;
            return this;
        }

        /**
         * Set {@link DebugLoggerImpl} implementing logger instance
         *
         * @param loggerImpl Logger instance
         * @return this (for chaining)
         */
        @NonNull
        public Builder withDebugLoggerImpl(@Nullable DebugLoggerImpl loggerImpl) {
            this.loggerImpl = loggerImpl;
            return this;
        }

        /**
         * Set whether {@link ExecutorService} should be shut down or not after {@link PicoMaven} close.
         *
         * @param value Boolean
         * @return this (for chaining)
         */
        @NonNull
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
                    public Thread newThread(@NonNull Runnable runnable) {
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
