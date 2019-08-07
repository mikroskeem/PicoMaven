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
import eu.mikroskeem.picomaven.internal.DataProcessor;
import eu.mikroskeem.picomaven.internal.SneakyThrow;
import eu.mikroskeem.picomaven.internal.UrlUtils;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
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
    private static final List<ArtifactChecksum.ChecksumAlgo> REMOTE_CHECKSUM_ALGOS = Arrays.asList(
            ArtifactChecksum.ChecksumAlgo.MD5,
            ArtifactChecksum.ChecksumAlgo.SHA1
    );

    private final Path downloadPath;
    private final List<Dependency> dependencyList;
    private final List<URL> repositoryUris;
    private final ExecutorService executorService;
    private final boolean shouldCloseExecutorService;

    private void downloadArtifact(@NonNull Dependency dependency, @NonNull URL artifactUrl,
                                  @NonNull Path target, @NonNull InputStream is) throws IOException {
        Path parentPath = target.getParent();
        if (!Files.exists(parentPath)) {
            Files.createDirectories(parentPath);
        }

        // Copy artifact into memory
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int b;
        while ((b = is.read(buf, 0, buf.length)) != -1) {
            baos.write(buf, 0, b);
        }

        // Check specified checksums
        if (!dependency.getChecksums().isEmpty()) {
            logger.trace("{} has checksums set, using them to check consistency", dependency);
            for (ArtifactChecksum checksum : dependency.getChecksums()) {
                if (!DataProcessor.verifyChecksum(checksum, baos.toByteArray())) {
                    throw new IOException(checksum.getAlgo().name() + " checksum mismatch");
                }
            }
        } else {
            // Attempt to fetch remote checksums
            logger.trace("{} does not have any checksums, fetching them from remote repository", dependency);
            List<CompletableFuture<ArtifactChecksum>> futures = new ArrayList<>(REMOTE_CHECKSUM_ALGOS.size());
            for (ArtifactChecksum.ChecksumAlgo remoteChecksumAlgo : REMOTE_CHECKSUM_ALGOS) {
                futures.add(DataProcessor.getArtifactChecksum(executorService, artifactUrl, remoteChecksumAlgo));
            }

            // Wait for both checksum queries to finish
            ArtifactChecksum artifactChecksum;
            boolean checksumVerified = false;
            while (true) {
                try {
                    for (CompletableFuture<?> future : futures) {
                        future.get();
                    }
                    break;
                }
                catch (ExecutionException | InterruptedException ignored) {}
            }

            // Verify checksums
            for (CompletableFuture<ArtifactChecksum> future : futures) {
                if ((artifactChecksum = future.getNow(null)) != null) {
                    logger.trace("{} repository {} checksum is {}", dependency, artifactChecksum.getAlgo().name(), artifactChecksum.getChecksum());
                    if (!DataProcessor.verifyChecksum(artifactChecksum, baos.toByteArray())) {
                        throw new IOException(artifactChecksum.getAlgo().name() + " checksum mismatch");
                    }
                    checksumVerified = true;
                }
            }

            if (!checksumVerified) {
                logger.debug("{}'s {} checksums weren't available remotely", REMOTE_CHECKSUM_ALGOS, dependency);
            }
        }

        // Copy
        Files.copy(new ByteArrayInputStream(baos.toByteArray()), target, StandardCopyOption.REPLACE_EXISTING);

        // Download success!
        logger.debug("{} download succeeded!", dependency);
    }

    public Map<@NonNull Dependency, @NonNull Future<@Nullable DownloadResult>> downloadAllArtifacts() {
        Map<Dependency, Future<DownloadResult>> tasks = new LinkedHashMap<>(dependencyList.size());
        for (final Dependency dependency : dependencyList) {
            tasks.put(dependency, executorService.submit(() -> {
                logger.trace("Trying to download dependency {}", dependency);
                Path artifactDownloadPath = UrlUtils.formatLocalPath(downloadPath, dependency, "jar");
                URL artifactUrl;
                if (Files.exists(artifactDownloadPath)) {
                    logger.debug("{} is already downloaded", dependency);

                    // TODO: transitive dependencies
                    return DownloadResult.of(dependency, artifactDownloadPath);
                }

                // Iterate through repositories until an artifact is found
                for (URL repository : repositoryUris) {
                    logger.debug("Trying repository {} for {}", repository, dependency);
                    Metadata groupMetadata = null;
                    Metadata artifactMetadata = null;
                    URLConnection connection = null;

                    // Do dumb check whether we can download artifact without parsing XML at all
                    if (!dependency.getVersion().endsWith("-SNAPSHOT")) {
                        connection = UrlUtils.openConnection((artifactUrl = UrlUtils.buildDirectArtifactUrl(repository, dependency, "jar")));
                        logger.trace("{} direct artifact URL: {}", dependency, artifactUrl);
                        try (InputStream is = connection.getInputStream()) {
                            UrlUtils.ensureSuccessfulRequest(connection);
                            downloadArtifact(dependency, artifactUrl, artifactDownloadPath, is);

                            // TODO: transitive dependencies
                            return DownloadResult.of(dependency, artifactDownloadPath);
                        } catch (IOException e) {
                            // Non-fatal error, continue
                            logger.trace("{} direct artifact URL {} did not work, trying to fetch XML", dependency, artifactUrl);
                        }
                    }

                    // Try to find group metadata xml and grab artifact metadata xml URL from it
                    URL groupMetaURI = UrlUtils.buildGroupMetaURL(repository, dependency);
                    logger.trace("{} group meta URL: {}", dependency, groupMetaURI);
                    try {
                        if ((groupMetadata = DataProcessor.getMetadata(groupMetaURI)) != null) {
                            URL artifactMetaURI = UrlUtils.buildArtifactMetaURL(repository, groupMetadata, dependency);
                            logger.trace("{} artifact meta URL: {}", dependency, artifactMetaURI);
                            artifactMetadata = DataProcessor.getMetadata(artifactMetaURI);
                        } else {
                            throw new FileNotFoundException();
                        }
                    } catch (FileNotFoundException e) {
                        logger.debug("{} not found in repository {}", dependency, repository);
                        continue;
                    } catch (IOException e) {
                        // Skip this repository
                        continue;
                    }

                    // Figure out artifact URL and attempt to download it
                    artifactUrl = UrlUtils.buildArtifactURL(repository, artifactMetadata, dependency, "jar");
                    logger.trace("Downloading {} from {}", dependency, artifactUrl);
                    connection = UrlUtils.openConnection(artifactUrl);
                    try (InputStream is = connection.getInputStream()) {
                        UrlUtils.ensureSuccessfulRequest(connection);
                        downloadArtifact(dependency, artifactUrl, artifactDownloadPath, is);

                        // TODO: transitive dependencies
                        return DownloadResult.of(dependency, artifactDownloadPath);
                    } catch (FileNotFoundException e) {
                        logger.debug("{} not found in repository {}", dependency, repository);
                    } catch (IOException e) {
                        logger.debug("{} download failed!", dependency);
                    }
                }

                return DownloadResult.of(dependency, artifactDownloadPath, new IOException("Not found"));
            }));
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
                throw new RuntimeException(e);
            }
        }
    }

    private PicoMaven(Path downloadPath, List<Dependency> dependencyList, List<URL> repositoryUris,
                      ExecutorService executorService, boolean shouldCloseExecutorService) {
        this.downloadPath = downloadPath;
        this.dependencyList = dependencyList;
        this.repositoryUris = repositoryUris;
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
            return new PicoMaven(downloadPath, dependencies, repositories, executorService, shouldCloseExecutorService);
        }
    }
}
