package eu.mikroskeem.picomaven;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;


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
    public Dependency(@NotNull String groupId, @NotNull String artifactId, @NotNull String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    /**
     * Gets {@link Dependency} group id
     *
     * @return Dependency group id
     */
    @NotNull
    @Contract(pure = true)
    public String getGroupId() {
        return groupId;
    }

    /**
     * Gets {@link Dependency} artifact id
     *
     * @return Dependency artifact id
     */
    @NotNull
    @Contract(pure = true)
    public String getArtifactId() {
        return artifactId;
    }

    /**
     * Gets {@link Dependency} version
     *
     * @return Dependency version
     */
    @NotNull
    @Contract(pure = true)
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
    @NotNull
    @Contract("null -> fail")
    public static Dependency fromGradle(String dependencyString) {
        String[] parts = dependencyString.split(":");
        if(parts.length != 3) throw new IllegalStateException("Invalid dependency string: " + dependencyString);
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
