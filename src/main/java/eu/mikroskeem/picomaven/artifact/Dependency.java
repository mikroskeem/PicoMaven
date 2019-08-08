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

package eu.mikroskeem.picomaven.artifact;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents dependency
 *
 * @author Mark Vainomaa
 */
public final class Dependency {
    /**
     * GroupId of dependency
     */
    private final String groupId;

    /**
     * ArtifactId of dependency
     */
    private final String artifactId;

    /**
     * Version of dependency
     */
    private final String version;

    /**
     * Artifact classifier. By default {@code null}
     */
    private final String classifier;

    /**
     * Whether to download transitive dependencies as well or not
     */
    private final boolean transitive;

    /**
     * Checksums to validate this dependency against. If empty, then checksums are
     * fetched from remote repository (which is not tamper-proof, use with care!)
     */
    private final List<ArtifactChecksum> checksums;

    /**
     * Dependency information constructor
     *
     * @param groupId Dependency group id
     * @param artifactId Dependency artifact id
     * @param version Dependency version
     * @param classifier Jar classifier
     * @param transitive Whether to download transitive dependencies as well or not
     * @param checksums Predefined checksums
     */
    public Dependency(@NonNull String groupId, @NonNull String artifactId, @NonNull String version,
                      @Nullable String classifier, boolean transitive, @NonNull Collection<ArtifactChecksum> checksums) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.classifier = classifier;
        this.transitive = transitive;
        this.checksums = new ArrayList<>(checksums);
    }

    /**
     * Dependency information constructor. Classifier is {@code null} and transitive dependencies are downloaded
     *
     * @param groupId Dependency group id
     * @param artifactId Dependency artifact id
     * @param version Dependency version
     */
    public Dependency(@NonNull String groupId, @NonNull String artifactId, @NonNull String version) {
        this(groupId, artifactId, version, null, true, Collections.emptyList());
    }

    /**
     * Dependency information constructor. Classifier is {@code null} and transitive dependencies are downloaded
     *
     * @param groupId Dependency group id
     * @param artifactId Dependency artifact id
     * @param version Dependency version
     * @param checksums Checksums
     */
    public Dependency(@NonNull String groupId, @NonNull String artifactId, @NonNull String version, @NonNull ArtifactChecksum... checksums) {
        this(groupId, artifactId, version, null, true, Arrays.asList(checksums));
    }

    /**
     * Dependency copy information constructor
     *
     * @param other Other {@link Dependency} to copy information from
     */
    public Dependency(@NonNull Dependency other) {
        this(other.groupId, other.artifactId, other.version, other.classifier, other.transitive, other.checksums);
    }

    /**
     * Gets {@link Dependency} group id
     *
     * @return Dependency group id
     */
    @Pure
    @NonNull
    public String getGroupId() {
        return groupId;
    }

    /**
     * Gets {@link Dependency} artifact id
     *
     * @return Dependency artifact id
     */
    @Pure
    @NonNull
    public String getArtifactId() {
        return artifactId;
    }

    /**
     * Gets {@link Dependency} version
     *
     * @return Dependency version
     */
    @Pure
    @NonNull
    public String getVersion() {
        return version;
    }


    /**
     * Gets {@link Dependency} artifact classifier
     *
     * @return Dependency classifier
     */
    @Pure
    @Nullable
    public String getClassifier() {
        return classifier;
    }


    /**
     * Returns whether dependency is transitive or not
     *
     * @return Whether dependency is transitive or not
     */
    @Pure
    public boolean isTransitive() {
        return transitive;
    }

    /**
     * Gets the list of predefined checksums for this artifact.
     *
     * @return Predefined checksums for this artifact
     */
    @Pure
    @NonNull
    public List<@NonNull ArtifactChecksum> getChecksums() {
        return checksums;
    }

    @Pure
    @Override
    public String toString() {
        return "Dependency{" +
                "groupId='" + groupId + '\'' +
                ", artifactId='" + artifactId + '\'' +
                ", version='" + version + '\'' +
                ", classifier='" + classifier + '\'' +
                ", transitive=" + transitive +
                ", checksums=" + checksums +
                '}';
    }

    @Pure
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Dependency that = (Dependency) o;
        return transitive == that.transitive &&
                groupId.equals(that.groupId) &&
                artifactId.equals(that.artifactId) &&
                version.equals(that.version) &&
                Objects.equals(classifier, that.classifier) &&
                checksums.equals(that.checksums);
    }

    @Pure
    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId, version, classifier, transitive, checksums);
    }

    /**
     * Parse dependency string (like {@code org.projectlombok:lombok:1.16.16</pre>})
     * and return it as {@link Dependency} object
     *
     * @param dependencyString Gradle dependency string
     * @return {@link Dependency}
     */
    @NonNull
    public static Dependency fromString(@NonNull String dependencyString) {
        String[] parts = dependencyString.split(":");
        String groupId;
        String artifactId;
        String version;
        String classifier;

        if (parts.length < 3 || parts.length > 4) {
            throw new IllegalStateException("Invalid dependency string: '" + dependencyString + "'");
        }

        groupId = parts[0];
        artifactId = parts[1];
        version = parts[2];
        classifier = parts.length == 4 ? parts[3] : null;

        return new Dependency(groupId, artifactId, version, classifier, true, Collections.emptyList());
    }
}
