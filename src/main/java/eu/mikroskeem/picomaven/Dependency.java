package eu.mikroskeem.picomaven;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * Represents dependency
 *
 * @author Mark Vainomaa
 */
@Getter
@ToString
@RequiredArgsConstructor
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
}
