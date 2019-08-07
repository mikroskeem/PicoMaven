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

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.dataflow.qual.Pure;

/**
 * Represents dependency
 *
 * @author Mark Vainomaa
 */
public class Dependency {
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
     * Dependency information constructor
     *
     * @param groupId Dependency group id
     * @param artifactId Dependency artifact id
     * @param version Dependency version
     */
    public Dependency(@NonNull String groupId, @NonNull String artifactId, @NonNull String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    /**
     * Gets {@link Dependency} group id
     *
     * @return Dependency group id
     */
    @NonNull
    public String getGroupId() {
        return groupId;
    }

    /**
     * Gets {@link Dependency} artifact id
     *
     * @return Dependency artifact id
     */
    @NonNull
    @Pure
    public String getArtifactId() {
        return artifactId;
    }

    /**
     * Gets {@link Dependency} version
     *
     * @return Dependency version
     */
    @NonNull
    @Pure
    public String getVersion() {
        return version;
    }

    /**
     * Parse Gradle dependency string (like <pre>org.projectlombok:lombok:1.16.16</pre>)
     * and return it as {@link Dependency} object
     *
     * @param dependencyString Gradle dependency string
     * @return {@link Dependency}
     */
    @NonNull
    public static Dependency fromGradle(@NonNull String dependencyString) {
        String[] parts = dependencyString.split(":");
        if (parts.length != 3) throw new IllegalStateException("Invalid dependency string: " + dependencyString);
        return new Dependency(parts[0], parts[1], parts[2]);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Dependency that = (Dependency) o;

        return groupId.equals(that.groupId) && artifactId.equals(that.artifactId) && version.equals(that.version);
    }

    @Override
    public int hashCode() {
        int result = groupId.hashCode();
        result = 31 * result + artifactId.hashCode();
        result = 31 * result + version.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Dependency{groupId='" + groupId + '\'' + ", artifactId='" + artifactId + '\'' + ", version='" +
                version + '\'' + '}';
    }
}
