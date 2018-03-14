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
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

class UrlUtils {
    @Contract("null, null -> fail")
    static URI buildGroupMetaURI(URI repositoryURI, Dependency dependency) {
        return URI.create(repositoryURI.toString() + "/" + concatGroupArtifact(dependency) + "/maven-metadata.xml");
    }

    @Contract("null, null, null -> fail")
    static URI buildArtifactMetaURI(URI repositoryURI, Metadata metadata, Dependency dependency) {
        return URI.create(repositoryURI.toString() + "/" + concatGroupArtifact(metadata) +
                "/" + dependency.getVersion() +  "/maven-metadata.xml");
    }

    @NotNull
    @Contract("null, _, null -> fail")
    static URI buildArtifactJarURI(URI repositoryURI, Metadata artifactMetadata, Dependency dependency) {
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

    @NotNull
    @Contract("null, null -> fail")
    static Path formatLocalPath(Path parent, Dependency dependency) {
        return Paths.get(parent.toString(),
                dependency.getGroupId().replace('.', '/'),
                dependency.getArtifactId(),
                dependency.getVersion(),
                formatJarNameFromDependency(dependency)
        );
    }

    @NotNull
    @Contract("null -> fail")
    private static String concatGroupArtifact(Dependency dependency) {
        return dependency.getGroupId().replace('.', '/') + "/" + dependency.getArtifactId();
    }

    @NotNull
    @Contract("null -> fail")
    private static String concatGroupArtifact(Metadata metadata) {
        return metadata.getGroupId().replace('.', '/') + "/" + metadata.getArtifactId();
    }

    @NotNull
    @Contract("null -> fail")
    private static String formatJarNameFromDependency(Dependency dependency) {
        return dependency.getArtifactId() + "-" + dependency.getVersion() + ".jar";
    }

    /* Gets always latest version */
    @NotNull
    @Contract("null, null -> fail")
    private static String formatJarNameFromMeta(Dependency dependency, Metadata metadata) {
        /* Explicit NPE */
        //noinspection ResultOfMethodCallIgnored
        dependency.getClass();
        //noinspection ResultOfMethodCallIgnored
        metadata.getClass();

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
