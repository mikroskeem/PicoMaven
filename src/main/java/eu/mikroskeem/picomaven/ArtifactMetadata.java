package eu.mikroskeem.picomaven;

import lombok.Getter;
import lombok.ToString;

import java.util.List;

/**
 * Represents Artifact metadata
 * http://maven.apache.org/ref/3.2.1/maven-repository-metadata/repository-metadata.html
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
    private List<Plugin> plugins;

    @Getter
    @ToString
    public static class Versioning {
        private String latest;
        private String release;
        private Snapshot snapshot;
        private List<String> versions;
        private String lastUpdated;
        private List<SnapshotVersion> snapshotVersions;

        @Getter
        @ToString
        public static class Snapshot {
            private String timestamp;
            private int buildNumber;
            private boolean localCopy;
        }

        @Getter
        @ToString
        public static class SnapshotVersion {
            private String classifier;
            private String extension;
            private String value;
            private String updated;
        }
    }

    @Getter
    @ToString
    public static class Plugin {
        private String name;
        private String prefix;
        private String artifactId;
    }
}
