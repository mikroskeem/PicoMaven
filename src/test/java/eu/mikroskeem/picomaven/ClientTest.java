package eu.mikroskeem.picomaven;

import eu.mikroskeem.picomaven.meta.ArtifactMetadata;
import eu.mikroskeem.picomaven.meta.Metadata;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * @author Mark Vainomaa
 */
public class ClientTest {
    private static File downloadDir;
    static {
        downloadDir = new File("./target/_downloadTest" + Math.random());
        downloadDir.mkdirs();
    }

    @Test
    public void testDefaultBuilder() throws Exception {
        List<Dependency> dependencies = Arrays.asList(UriUtilsTest.SAMPLE_DEPENDENCY, UriUtilsTest.SAMPLE_DEPENDENCY2);
        try (
            PicoMaven picoMaven = new PicoMaven.Builder()
                .withDownloadPath(downloadDir.toPath())
                .withRepositories(Arrays.asList(Constants.MAVEN_CENTRAL_REPOSITORY, UriUtilsTest.DEFAULT_REPOSITORY2))
                .withDependencies(dependencies)
                .build()
        ) {
            List<Path> downloadedDeps = picoMaven.downloadAll();
            Assertions.assertEquals(dependencies.size(), downloadedDeps.size());
        }
    }

    @Test
    public void testDataFetching() throws Exception {
        OkHttpClient client = new OkHttpClient();
        URI uri = UrlUtils.buildGroupMetaURI(UriUtilsTest.DEFAULT_REPOSITORY2, UriUtilsTest.SAMPLE_DEPENDENCY2);
        Metadata metadata = DataProcessor.getMetadata(client, uri);
        Assertions.assertNotNull(metadata);
        URI uri2 = UrlUtils.buildArtifactMetaURI(UriUtilsTest.DEFAULT_REPOSITORY2, metadata, UriUtilsTest.SAMPLE_DEPENDENCY2);
        ArtifactMetadata metadata2 = DataProcessor.getArtifactMetadata(client, uri2);
        Assertions.assertNotNull(metadata2);
        URI uri3 = UrlUtils.buildArtifactJarURI(UriUtilsTest.DEFAULT_REPOSITORY2, metadata2, UriUtilsTest.SAMPLE_DEPENDENCY2);
        Assertions.assertNotNull(uri3);
    }
}
