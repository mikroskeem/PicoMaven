package eu.mikroskeem.picomaven.meta;

import lombok.Getter;
import lombok.ToString;

import java.util.List;

/**
 * Represents GroupId metadata
 *
 * @author Mark Vainomaa
 */
@Getter
@ToString
public class Metadata {
    private String groupId;
    private String artifactId;
    private Versioning versioning;

    @Getter
    @ToString
    public static class Versioning {
        private List<String> versions;
        private String lastUpdated;
        private String latest;
        private String release;
    }
}
