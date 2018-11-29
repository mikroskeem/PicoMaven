/*
 * This file is part of project PicoMaven, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2017-2018 Mark Vainomaa <mikroskeem@mikroskeem.eu>
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

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

class UrlUtils {
    @NonNull
    static URI buildGroupMetaURI(@NonNull URI repositoryURI, @NonNull Dependency dependency) {
        return URI.create(repositoryURI.toString() + "/" + concatGroupArtifact(dependency) + "/maven-metadata.xml");
    }

    @NonNull
    static URI buildArtifactMetaURI(@NonNull URI repositoryURI, @NonNull Metadata metadata, @NonNull Dependency dependency) {
        return URI.create(repositoryURI.toString() + "/" + concatGroupArtifact(metadata) +
                "/" + dependency.getVersion() +  "/maven-metadata.xml");
    }

    @NonNull
    static URI buildArtifactJarURI(@NonNull URI repositoryURI, @Nullable Metadata artifactMetadata, @NonNull Dependency dependency) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(repositoryURI.toString());
        stringBuilder.append('/');
        stringBuilder.append(concatGroupArtifact(dependency));
        stringBuilder.append('/');
        stringBuilder.append(dependency.getVersion());
        stringBuilder.append('/');
        stringBuilder.append(artifactMetadata != null ? formatJarNameFromMeta(dependency, artifactMetadata) :
                formatJarNameFromDependency(dependency));
        return URI.create(stringBuilder.toString());
    }

    @NonNull
    static Path formatLocalPath(@NonNull Path parent, @NonNull Dependency dependency) {
        return Paths.get(parent.toString(),
                dependency.getGroupId().replace('.', '/'),
                dependency.getArtifactId(),
                dependency.getVersion(),
                formatJarNameFromDependency(dependency)
        );
    }

    @NonNull
    private static String concatGroupArtifact(@NonNull Dependency dependency) {
        return dependency.getGroupId().replace('.', '/') + "/" + dependency.getArtifactId();
    }

    @NonNull
    private static String concatGroupArtifact(@NonNull Metadata metadata) {
        return metadata.getGroupId().replace('.', '/') + "/" + metadata.getArtifactId();
    }

    @NonNull
    private static String formatJarNameFromDependency(@NonNull Dependency dependency) {
        return dependency.getArtifactId() + "-" + dependency.getVersion() + ".jar";
    }

    /* Gets always latest version */
    @NonNull
    private static String formatJarNameFromMeta(@NonNull Dependency dependency, @NonNull Metadata metadata) {
        try {
            return  dependency.getArtifactId()
                            + "-" +
                    dependency.getVersion().replaceAll("-SNAPSHOT", "")
                            + "-" +
                    metadata.getVersioning().getSnapshot().getTimestamp()
                            + "-" +
                    metadata.getVersioning().getSnapshot().getBuildNumber()
                            + ".jar";
        } catch (NullPointerException e) {
            /* Dear god, there are so many different formats of maven metadatas... */
            return formatJarNameFromDependency(dependency);
        }
    }
}
