package eu.mikroskeem.picomaven;

import eu.mikroskeem.picomaven.meta.ArtifactMetadata;
import eu.mikroskeem.picomaven.meta.Metadata;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URI;

/**
 * @author Mark Vainomaa
 */
public class TestClient {
    private static File downloadDir;
    static {
        downloadDir = new File("./downloadTest" + Math.random());
        downloadDir.mkdirs();
        downloadDir.deleteOnExit();
    }

    @Test
    public void testDefaultBuilder() {
        try (
            PicoMaven picoMaven = new PicoMaven.Builder()
                .withDownloadPath(downloadDir.toPath())
                .build()
        ) {
            picoMaven.downloadAll();
        }
    }

    @Test
    public void testDataFetching() throws Exception {
        OkHttpClient client = new OkHttpClient();
        URI uri = UrlUtils.buildGroupMetaURI(TestUriUtils.DEFAULT_REPOSITORY2, TestUriUtils.SAMPLE_DEPENDENCY2);
        Metadata metadata = DataProcessor.getMetadata(client, uri);
        URI uri2 = UrlUtils.buildArtifactMetaURI(TestUriUtils.DEFAULT_REPOSITORY2, metadata, TestUriUtils.SAMPLE_DEPENDENCY2);
        ArtifactMetadata metadata2 = DataProcessor.getArtifactMetadata(client, uri2);
        URI uri3 = UrlUtils.buildArtifactJarURI(TestUriUtils.DEFAULT_REPOSITORY2, metadata2, TestUriUtils.SAMPLE_DEPENDENCY2);
        System.out.println(uri3);
    }
}
