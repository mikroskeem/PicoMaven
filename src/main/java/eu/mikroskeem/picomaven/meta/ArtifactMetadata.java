package eu.mikroskeem.picomaven.meta;

import lombok.Getter;
import lombok.ToString;

/**
 * Represents Artifact metadata
 *
 * @author Mark Vainomaa
 */
@Getter
@ToString
public class ArtifactMetadata {
    private String groupId;
    private String artifactId;
    private String version;
    private Versioning versioning;

    @Getter
    @ToString
    public static class Versioning {
        private Snapshot snapshot;
        /* private List<SnapshotVersion> snapshotVersions; */
        private String lastUpdated;

        @Getter
        @ToString
        public static class Snapshot {
            private String timestamp;
            private int buildNumber;
        }

        /*
        @Getter
        @ToString
        public static class SnapshotVersion {
            private String extension;
            private String value;
            private long updated;
        }
        */
    }
}
