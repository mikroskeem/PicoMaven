package eu.mikroskeem.picomaven;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import eu.mikroskeem.picomaven.meta.ArtifactMetadata;
import eu.mikroskeem.picomaven.meta.Metadata;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
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
    private final boolean shouldCloseExecutorService;

    private final Queue<Dependency> downloadedDependencies = new ConcurrentLinkedQueue<>();
    private final List<Callable<Void>> downloadTasks = new ArrayList<>();

    public List<Path> downloadAll() throws ExecutionException, InterruptedException {
        ObjectMapper objectMapper = new XmlMapper();

        /* Iterate through all dependencies */
        for(Dependency dependency : dependencyList) {
            Callable<Void> task = () -> {
                Path theDownloadPath = UrlUtils.formatLocalPath(downloadPath, dependency);
                if(!Files.exists(theDownloadPath)) {
                    /* Iterate through every repository */
                    try {
                        for (URI repositoryUri : repositoryUris) {
                            Metadata metadata = null;
                            ArtifactMetadata artifactMetadata = null;
                            URI groupMetaURI = UrlUtils.buildGroupMetaURI(repositoryUri, dependency);

                            /* Try to parse group meta */
                            Request request = new Request.Builder().url(HttpUrl.get(groupMetaURI)).build();
                            try (Response groupMetaResponse = httpClient.newCall(request).execute()) {
                                if (groupMetaResponse.isSuccessful() && (metadata = objectMapper.readValue(groupMetaResponse.body()
                                        .byteStream(), Metadata.class)) != null) {
                                    URI artifactMetaURI = UrlUtils.buildArtifactMetaURI(repositoryUri, metadata, dependency);

                                    /* Try to parse artifact metadata */
                                    request = new Request.Builder().url(HttpUrl.get(artifactMetaURI)).build();
                                    try (Response artifactMetaResponse = httpClient.newCall(request).execute()) {
                                        if (artifactMetaResponse.isSuccessful()) {
                                            artifactMetadata = objectMapper.readValue(artifactMetaResponse.body()
                                                    .byteStream(), ArtifactMetadata.class);
                                        }
                                    }
                                }
                            }

                            /* Build artifact url */
                            URI artifactJarURI = UrlUtils.buildArtifactJarURI(repositoryUri, artifactMetadata, dependency);
                            request = new Request.Builder().url(HttpUrl.get(artifactJarURI)).build();
                            try (Response artifactJarResponse = httpClient.newCall(request).execute()) {
                                if (artifactJarResponse.isSuccessful()) {
                                    Path parentPath = theDownloadPath.getParent();
                                    if (!Files.exists(parentPath)) Files.createDirectories(parentPath);
                                    Files.copy(artifactJarResponse.body().byteStream(), theDownloadPath,
                                            StandardCopyOption.REPLACE_EXISTING);
                                    /* Download success! */
                                    downloadedDependencies.add(dependency);
                                    break;
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
        private DownloaderCallbacks downloaderCallbacks = null;
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

        public Builder withDownloaderCallbacks(DownloaderCallbacks downloaderCallbacks){
            this.downloaderCallbacks = downloaderCallbacks;
            return this;
        }

        public Builder shouldCloseExecutorService(boolean value) {
            this.shouldCloseExecutorService = value;
            return this;
        }

        public PicoMaven build() {
            if(downloadPath == null) throw new IllegalStateException("Download path cannot be unset!");
            if(dependencies == null) dependencies = Collections.emptyList();
            if(repositories == null) repositories = Collections.emptyList();
            if(httpClient == null) httpClient = new OkHttpClient();
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
                    executorService, downloaderCallbacks, shouldCloseExecutorService);
        }
    }
}
