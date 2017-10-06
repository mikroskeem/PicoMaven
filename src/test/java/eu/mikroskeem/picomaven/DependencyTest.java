package eu.mikroskeem.picomaven;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * @author Mark Vainomaa
 */
public class DependencyTest {
    @Test
    public void testDependencyFromString() {
        String groupId = "com.zaxxer";
        String artifactId = "HikariCP";
        String version = "2.6.3";

        Dependency dependency = Dependency.fromGradle(groupId + ':' + artifactId + ':' + version);
        Assertions.assertEquals(groupId, dependency.getGroupId());
        Assertions.assertEquals(artifactId, dependency.getArtifactId());
        Assertions.assertEquals(version, dependency.getVersion());
    }
}
