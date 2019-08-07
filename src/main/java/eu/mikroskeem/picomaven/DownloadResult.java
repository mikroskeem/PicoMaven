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

import eu.mikroskeem.picomaven.artifact.Dependency;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * PicoMaven download result
 *
 * @author Mark Vainomaa
 */
public final class DownloadResult {
    private final Dependency dependency;
    private final List<DownloadResult> transitiveDependencies;

    private final Path artifactPath;
    private final boolean success;
    private final Exception downloadException;

    public DownloadResult(@NonNull Dependency dependency,
                          @NonNull List<DownloadResult> transitiveDependencies,
                          @NonNull Path artifactPath,
                          boolean success,
                          @Nullable Exception downloadException) {
        this.dependency = dependency;
        this.transitiveDependencies = transitiveDependencies;
        this.artifactPath = artifactPath;
        this.success = success;
        this.downloadException = downloadException;
    }

    public DownloadResult(@NonNull Dependency dependency,
                          @NonNull List<DownloadResult> transitiveDependencies,
                          @NonNull Path artifactPath) {
        this(dependency, transitiveDependencies, artifactPath, true, null);
    }

    public DownloadResult(@NonNull Dependency dependency,
                          @NonNull Path artifactPath,
                          @NonNull Exception downloadException) {
        this(dependency, Collections.emptyList(), artifactPath, false, downloadException);
    }

    @NonNull
    public List<Path> getAllDownloadedFiles() {
        List<Path> files = new LinkedList<>();
        files.add(getArtifactPath());

        for (DownloadResult transitiveDependency : getTransitiveDependencies()) {
            files.addAll(transitiveDependency.getAllDownloadedFiles());
        }

        return Collections.unmodifiableList(files);
    }

    @NonNull
    public Dependency getDependency() {
        return dependency;
    }

    public List<DownloadResult> getTransitiveDependencies() {
        return transitiveDependencies;
    }

    @NonNull
    public Path getArtifactPath() {
        return artifactPath;
    }

    public boolean isSuccess() {
        return success;
    }

    @Nullable
    public Exception getDownloadException() {
        return downloadException;
    }

    @Override
    public String toString() {
        return "DownloadResult{" +
                "dependency=" + dependency +
                ", artifactPath=" + artifactPath +
                ", success=" + success +
                ", downloadException=" + downloadException +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DownloadResult that = (DownloadResult) o;
        return success == that.success &&
                dependency.equals(that.dependency) &&
                transitiveDependencies.equals(that.transitiveDependencies) &&
                artifactPath.equals(that.artifactPath) &&
                Objects.equals(downloadException, that.downloadException);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dependency, artifactPath, success, downloadException);
    }

    static DownloadResult of(@NonNull Dependency dependency, @NonNull Path artifactPath) {
        return new DownloadResult(dependency, Collections.emptyList(), artifactPath);
    }

    static DownloadResult of(@NonNull Dependency dependency, @NonNull Path artifactPath, @NonNull Exception downloadException) {
        return new DownloadResult(dependency, artifactPath, downloadException);
    }
}
