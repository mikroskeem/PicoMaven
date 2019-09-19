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
import org.apache.maven.model.Model;
import org.apache.maven.model.Repository;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static eu.mikroskeem.picomaven.PicoMaven.REMOTE_CHECKSUM_ALGOS;

/**
 * @author Mark Vainomaa
 */
public final class DownloaderTask implements Callable<DownloadResult> {
    private static final Logger logger = LoggerFactory.getLogger(DownloaderTask.class);

    private final ExecutorService executorService;
    private final Dependency dependency;
    private final Path downloadPath;
    // Whether dependency downloading failure is fatal or not
    private final boolean optional;
    private final Set<URL> repositoryUrls;
    private final Deque<Future<DownloadResult>> transitiveDownloads;

    private final boolean isChild;

    public DownloaderTask(ExecutorService executorService, Dependency dependency, Path downloadPath, List<URL> repositoryUrls) {
        this(executorService, dependency, downloadPath,
                Collections.synchronizedSet(new HashSet<>(repositoryUrls)),
                false,
                new ConcurrentLinkedDeque<>(), false);
    }

    private DownloaderTask(ExecutorService executorService, Dependency dependency, Path downloadPath,
                           Set<URL> repositoryUrls, boolean optional, Deque<Future<DownloadResult>> transitiveDownloads,
                           boolean isChild) {
        this.executorService = executorService;
        this.dependency = dependency;
        this.downloadPath = downloadPath;
        this.optional = optional;
        this.repositoryUrls = repositoryUrls;
        this.transitiveDownloads = transitiveDownloads;
        this.isChild = isChild;
    }

    private DownloaderTask(DownloaderTask parent, Dependency dependency, boolean optional) {
        this(parent.executorService, dependency, parent.downloadPath, parent.repositoryUrls,
                optional, parent.transitiveDownloads, true);
    }

    @Override
    public DownloadResult call() throws Exception {
        logger.trace("Trying to download dependency {}", dependency);
        Path artifactPomDownloadPath = UrlUtils.formatLocalPath(downloadPath, dependency, "pom");
        Path artifactDownloadPath = UrlUtils.formatLocalPath(downloadPath, dependency, "jar");
        List<DownloadResult> transitive = new LinkedList<>();
        URL artifactPomUrl;
        URL artifactUrl;

        try {
            // Check if artifact already exists
            if (Files.exists(artifactDownloadPath)) {
                logger.debug("{} is already downloaded", dependency);

                if (Files.exists(artifactPomDownloadPath) && dependency.isTransitive()) {
                    transitive = downloadTransitive(null, artifactPomDownloadPath.toUri().toURL());
                }
                return DownloadResult.ofSuccess(dependency, artifactDownloadPath, optional, transitive);
            }

            // Iterate through repositories until the artifact is found
            for (URL repository : repositoryUrls) {
                logger.debug("Trying repository {} for {}", repository, dependency);
                Metadata groupMetadata = null;
                Metadata artifactMetadata = null;
                URLConnection connection = null;

                // Do dumb check whether we can download artifact without parsing XML at all
                if (!dependency.getVersion().endsWith("-SNAPSHOT")) {
                    logger.trace("Attempting to download artifact without parsing XML");
                    artifactPomUrl = UrlUtils.buildDirectArtifactUrl(repository, dependency, "pom");
                    artifactUrl = UrlUtils.buildDirectArtifactUrl(repository, dependency, "jar");

                    downloadDependency(repository, artifactPomUrl, artifactUrl, transitive);
                    if (dependency.isTransitive()) {
                        try {
                            logger.trace("{} direct artifact POM URL: {}", dependency, artifactPomUrl);
                            transitive = downloadTransitive(artifactPomDownloadPath, artifactPomUrl);
                        } catch (FileNotFoundException e) {
                            logger.trace("{} direct artifact POM not found", dependency);
                        } catch (IOException e) {
                            logger.warn("Failed to download {} POM: {}", dependency, e.getMessage());
                        }
                    }


                    logger.trace("{} direct artifact URL: {}", dependency, artifactUrl);
                    connection = UrlUtils.openConnection(artifactUrl);
                    try (InputStream is = connection.getInputStream()) {
                        UrlUtils.ensureSuccessfulRequest(connection);
                        downloadArtifact(dependency, artifactUrl, artifactDownloadPath, is);
                        return DownloadResult.ofSuccess(dependency, artifactDownloadPath, optional, transitive);
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
                artifactPomUrl = UrlUtils.buildArtifactURL(repository, artifactMetadata, dependency, "pom");
                artifactUrl = UrlUtils.buildArtifactURL(repository, artifactMetadata, dependency, "jar");
                return downloadDependency(repository, artifactPomUrl, artifactUrl, null);
            }

            // No repositories left to try
            throw new IOException("Not found");
        } catch (IOException e) {
            return DownloadResult.ofFailure(dependency, artifactDownloadPath, optional, e);
        }
    }

    private DownloadResult downloadDependency(URL repository, URL artifactPomUrl, URL artifactUrl, List<DownloadResult> transitive) throws IOException {
        Path artifactPomDownloadPath = UrlUtils.formatLocalPath(downloadPath, dependency, "pom");
        Path artifactDownloadPath = UrlUtils.formatLocalPath(downloadPath, dependency, "jar");
        List<DownloadResult> dependencyTransitiveDownloads = Collections.emptyList();

        if (dependency.isTransitive()) {
            try {
                logger.trace("Downloading {} POM from {}", dependency, artifactPomUrl);
                dependencyTransitiveDownloads = downloadTransitive(artifactPomDownloadPath, artifactPomUrl);
            } catch (FileNotFoundException e) {
                logger.trace("{} POM not found", dependency);
            } catch (IOException e) {
                logger.warn("Failed to download {} POM: {}", dependency, e.getMessage());
            }
        }

        logger.trace("Downloading {} from {}", dependency, artifactUrl);
        URLConnection connection = UrlUtils.openConnection(artifactUrl);
        try (InputStream is = connection.getInputStream()) {
            UrlUtils.ensureSuccessfulRequest(connection);
            downloadArtifact(dependency, artifactUrl, artifactDownloadPath, is);
            return DownloadResult.ofSuccess(dependency, artifactDownloadPath, optional, dependencyTransitiveDownloads);
        } catch (FileNotFoundException e) {
            logger.debug("{} not found in repository {}", dependency, repository);
            return DownloadResult.ofFailure(dependency, artifactDownloadPath, optional, e);
        } catch (IOException e) {
            logger.debug("{} download failed!", dependency);
            return DownloadResult.ofFailure(dependency, artifactDownloadPath, optional, e);
        }
    }

    @NonNull
    private List<DownloadResult> downloadTransitive(@Nullable Path pomPath, @NonNull URL artifactPomUrl) throws IOException {
        List<Future<DownloadResult>> transitive = Collections.emptyList();
        Model model;
        if ((model = DataProcessor.getPom(artifactPomUrl)) != null) {
            // Write model to disk
            if (pomPath != null) {
                Files.createDirectories(pomPath.getParent());
                try (BufferedWriter w = Files.newBufferedWriter(pomPath, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
                    DataProcessor.serializeModel(model, w, true);
                }
            }

            // Grab all dependencies
            if (!model.getDependencies().isEmpty()) {
                // Add all repositories from transitive POM
                if (!model.getRepositories().isEmpty()) {
                    for (Repository repository : model.getRepositories()) {
                        URL url = SneakyThrow.get(() -> new URL(repository.getUrl()));
                        logger.debug("Adding new repository URL {}", url);
                        repositoryUrls.add(url);
                    }
                }

                transitive = new ArrayList<>(model.getDependencies().size());
                for (org.apache.maven.model.Dependency modelDependency : model.getDependencies()) {
                    // Ignore certain scopes
                    if (!DataProcessor.RELEVANT_SCOPE_PREDICATE.test(modelDependency)) {
                        continue;
                    }

                    // Build PicoMaven dependency object
                    boolean transitiveOptional = "true".equalsIgnoreCase(modelDependency.getOptional());
                    Dependency transitiveDependency = new Dependency(
                            fixupIdentifiers(dependency, modelDependency.getGroupId()),
                            modelDependency.getArtifactId(),
                            fixupIdentifiers(dependency, modelDependency.getVersion()),
                            modelDependency.getClassifier(),
                            true,
                            Collections.emptyList()
                    );

                    // Validate
                    try {
                        Objects.requireNonNull(transitiveDependency.getGroupId(), "Group id cannot be null");
                        Objects.requireNonNull(transitiveDependency.getArtifactId(), "Artifact id cannot be null");
                        Objects.requireNonNull(transitiveDependency.getVersion(), "Version cannot be null");
                    } catch (NullPointerException e) {
                        logger.warn("{} transitive dependency {} is invalid: {}", dependency, transitive, e.getMessage());
                        continue;
                    }

                    logger.debug("{} requires transitive dependency {}", dependency, transitiveDependency);

                    DownloaderTask task = new DownloaderTask(this, transitiveDependency, transitiveOptional);
                    Future<DownloadResult> future = executorService.submit(task);
                    transitiveDownloads.add(future);
                    transitive.add(future);
                }
            }

            // Wait until all futures are done
            while (true) {
                try {
                    for (Future<DownloadResult> downloadResultFuture : transitive) {
                        downloadResultFuture.get();
                    }
                    break;
                } catch (ExecutionException e) {
                    SneakyThrow.rethrow(e);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            logger.trace("{} transitive dependencies download finished", dependency);

            // Collect download results
            List<DownloadResult> downloads = new ArrayList<>(transitive.size());
            for (Future<DownloadResult> future : transitive) {
                DownloadResult res = SneakyThrow.get(future::get);

                if (!res.isSuccess()) {
                    if (res.isOptional()) {
                        continue;
                    }
                    logger.trace("Failed to download {}: {}", res.getDependency(), res.getDownloadException().getMessage());
                }
                downloads.add(res);
            }

            return downloads;
        } else {
            throw new FileNotFoundException();
        }
    }

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
                } catch (ExecutionException e) {
                    SneakyThrow.rethrow(e);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
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

    private String fixupIdentifiers(@NonNull Dependency parent, String identifier) {
        // Apparently that's a thing
        if ("${project.groupId}".equalsIgnoreCase(identifier)) {
            return parent.getGroupId();
        }
        if ("${project.version}".equalsIgnoreCase(identifier)) {
            return parent.getVersion();
        }
        return identifier;
    }

    private static class TransitiveDependencyNotFoundException extends Exception {

    }
}
