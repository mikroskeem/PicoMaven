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

import eu.mikroskeem.picomaven.artifact.ArtifactChecksum;
import eu.mikroskeem.picomaven.artifact.Dependency;
import eu.mikroskeem.picomaven.internal.SneakyThrow;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * PicoMaven
 *
 * @author Mark Vainomaa
 */
public class PicoMaven implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(PicoMaven.class);
    static final List<ArtifactChecksum.ChecksumAlgo> REMOTE_CHECKSUM_ALGOS = Arrays.asList(
            ArtifactChecksum.ChecksumAlgo.MD5,
            ArtifactChecksum.ChecksumAlgo.SHA1
    );

    private final Path downloadPath;
    private final List<Dependency> dependencyList;
    private final List<URL> repositoryUrls;
    private final ExecutorService executorService;
    private final boolean shouldCloseExecutorService;

    public Map<@NonNull Dependency, @NonNull Future<@Nullable DownloadResult>> downloadAllArtifacts() {
        Map<Dependency, Future<DownloadResult>> tasks = new LinkedHashMap<>(dependencyList.size());
        for (final Dependency dependency : dependencyList) {
            DownloaderTask task = new DownloaderTask(executorService, dependency, downloadPath, repositoryUrls);
            tasks.put(dependency, executorService.submit(task));
        }

        return Collections.unmodifiableMap(tasks);
    }

    /**
     * Shuts down {@link ExecutorService}, if configured
     */
    @Override
    public void close() {
        if (shouldCloseExecutorService) {
            executorService.shutdown();
            try {
                executorService.awaitTermination(150, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                SneakyThrow.rethrow(e);
            }
        }
    }

    private PicoMaven(Path downloadPath, List<Dependency> dependencyList, List<URL> repositoryUrls,
                      ExecutorService executorService, boolean shouldCloseExecutorService) {
        this.downloadPath = downloadPath;
        this.dependencyList = dependencyList;
        this.repositoryUrls = repositoryUrls;
        this.executorService = executorService;
        this.shouldCloseExecutorService = shouldCloseExecutorService;
    }

    /**
     * {@link PicoMaven} builder
     */
    public static class Builder {
        private Path downloadPath = null;
        private List<Dependency> dependencies = null;
        private List<URL> repositories = null;
        private ExecutorService executorService = null;
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
            withRepositories((Collection<URI>) repositories);
            return this;
        }

        /**
         * Set repositories, where to look up dependencies
         *
         * @param repositories Collection of repository {@link URI}s
         * @return this (for chaining)
         */
        @NonNull
        public Builder withRepositories(@NonNull Collection<URI> repositories) {
            this.repositories = repositories.stream()
                    .map(u -> SneakyThrow.get(u::toURL))
                    .collect(Collectors.toList());
            return this;
        }

        /**
         * Set repositories list, where to look up dependencies
         *
         * @param repositories List of repository {@link URL}s
         * @return this (for chaining)
         */
        @NonNull
        public Builder withRepositoryURLs(@NonNull Collection<URL> repositories) {
            this.repositories = Collections.unmodifiableList(new ArrayList<>(repositories));
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
        @NonNull
        public PicoMaven build() {
            if (downloadPath == null) throw new IllegalStateException("Download path cannot be unset!");
            if (dependencies == null) dependencies = Collections.emptyList();
            if (repositories == null) repositories = Collections.emptyList();
            if (executorService == null) {
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
            return new PicoMaven(downloadPath, dependencies, new ArrayList<>(repositories),
                    executorService, shouldCloseExecutorService);
        }
    }
}
