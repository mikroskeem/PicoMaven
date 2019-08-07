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

package eu.mikroskeem.picomaven.internal;

import eu.mikroskeem.picomaven.artifact.Dependency;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

/**
 * @author Mark Vainomaa
 */
public final class UrlUtils {
    @NonNull
    public static URL buildGroupMetaURL(@NonNull URL repository, @NonNull Dependency dependency) {
        return createURL(String.format("%s/%s/%s/maven-metadata.xml",
                repository.toString(),
                dependency.getGroupId().replace('.', '/'),
                dependency.getArtifactId()
        ));
    }

    @NonNull
    public static URL buildArtifactMetaURL(@NonNull URL repository, @NonNull Metadata metadata, @NonNull Dependency dependency) {
        return createURL(String.format("%s/%s/%s/%s/maven-metadata.xml",
                repository.toString(),
                metadata.getGroupId().replace('.', '/'),
                metadata.getArtifactId(),
                dependency.getVersion()
        ));
    }

    @NonNull
    public static URL buildDirectArtifactUrl(@NonNull URL repositoryURI, @NonNull Dependency dependency, @NonNull String ext) {
        return createURL(String.format("%s/%s/%s/%s/%s",
                repositoryURI.toString(),
                dependency.getGroupId().replace('.', '/'),
                dependency.getArtifactId(),
                dependency.getVersion(),
                formatArtifactNameFromDependency(dependency, ext)
        ));
    }

    @NonNull
    public static URL buildArtifactURL(@NonNull URL repository, @Nullable Metadata artifactMetadata,
                                @NonNull Dependency dependency, @NonNull String ext) {
        String artifactFileName = artifactMetadata != null ?
                formatArtifactNameFromMeta(dependency, artifactMetadata, ext) :
                formatArtifactNameFromDependency(dependency, ext);

        return createURL(String.format("%s/%s/%s/%s/%s",
                repository.toString(),
                dependency.getGroupId().replace('.', '/'),
                dependency.getArtifactId(),
                dependency.getVersion(),
                artifactFileName
        ));
    }

    @NonNull
    public static Path formatLocalPath(@NonNull Path parent, @NonNull Dependency dependency, @NonNull String ext) {
        return Paths.get(parent.toString(),
                dependency.getGroupId().replace('.', '/'),
                dependency.getArtifactId(),
                dependency.getVersion(),
                formatArtifactNameFromDependency(dependency, ext)
        );
    }

    @NonNull
    public static URLConnection openConnection(@NonNull URL url) throws IOException {
        URLConnection connection = url.openConnection();
        if (connection instanceof HttpURLConnection) {
            HttpURLConnection httpConnection = (HttpURLConnection) connection;
            connection.setRequestProperty("User-Agent", "PicoMaven/__PICOMAVEN_VERSION__");
            httpConnection.setInstanceFollowRedirects(true);

            // Authentication
            if (url.getUserInfo() != null) {
                String auth = "Basic " + Base64.getEncoder().encodeToString(url.getUserInfo().getBytes(StandardCharsets.UTF_8));
                connection.setRequestProperty("Authorization", auth);
            }
        }
        connection.setUseCaches(false);
        connection.setDoInput(true);

        return connection;
    }

    public static void ensureSuccessfulRequest(@NonNull URLConnection connection) throws IOException {
        if (connection instanceof HttpURLConnection) {
            HttpURLConnection httpConnection = (HttpURLConnection) connection;
            if (httpConnection.getResponseCode() != 200) { // SUCCESS
                throw new IOException("Unexpected response code '" + httpConnection.getResponseCode() + '"');
            }
        }

        // Probably it is *shrug*
    }

    @NonNull
    private static String formatArtifactNameFromDependency(@NonNull Dependency dependency, @NonNull String ext) {
        return String.format("%s-%s%s.%s",
                dependency.getArtifactId(),
                dependency.getVersion(),
                (dependency.getClassifier() != null ? "-" + dependency.getClassifier() : ""),
                ext
        );
    }

    @NonNull
    private static String formatArtifactNameFromMeta(@NonNull Dependency dependency, @NonNull Metadata metadata, @NonNull String ext) {
        if (dependency.getVersion().endsWith("-SNAPSHOT") && metadata.getVersioning().getSnapshot() != null) {
            return String.format("%s-%s-%s-%s%s.%s",
                    metadata.getArtifactId(),
                    metadata.getVersion().replaceAll("-SNAPSHOT", ""),
                    metadata.getVersioning().getSnapshot().getTimestamp(),
                    metadata.getVersioning().getSnapshot().getBuildNumber(),
                    (dependency.getClassifier() != null ? "-" + dependency.getClassifier() : ""),
                    ext
            );
        } else {
            return formatArtifactNameFromDependency(dependency, ext);
        }
    }

    @NonNull
    private static URL createURL(@NonNull String raw) {
        try {
            return new URL(raw);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
