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
    private final String groupId;
    private final String artifactId;
    private final String version;
}
