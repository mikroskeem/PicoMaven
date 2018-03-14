/*
 * This file is part of project PicoMaven, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2017-2018 Mark Vainomaa <mikroskeem@mikroskeem.eu>
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

import okhttp3.OkHttpClient;
import org.apache.maven.artifact.repository.metadata.Metadata;
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
    private final static URI MAVEN_CENTRAL_REPOSITORY = URI.create("https://repo.maven.apache.org/maven2");
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
                .withRepositories(Arrays.asList(MAVEN_CENTRAL_REPOSITORY, UriUtilsTest.DEFAULT_REPOSITORY2))
                .withDependencies(dependencies)
                    .withDebugLoggerImpl((format, contents) -> System.err.format(format + "\n", contents))
                .build()
        ) {
            List<Path> downloadedDeps = picoMaven.downloadAll();
            Assertions.assertEquals(dependencies.size(), downloadedDeps.size());
        }
    }

    @Test
    public void testDataFetching() throws Exception {
        OkHttpClient httpClient = new OkHttpClient();

        URI uri = UrlUtils.buildGroupMetaURI(UriUtilsTest.DEFAULT_REPOSITORY2, UriUtilsTest.SAMPLE_DEPENDENCY2);
        Metadata metadata = DataProcessor.getMetadata(httpClient, uri);
        Assertions.assertNotNull(metadata);
        URI uri2 = UrlUtils.buildArtifactMetaURI(UriUtilsTest.DEFAULT_REPOSITORY2, metadata, UriUtilsTest.SAMPLE_DEPENDENCY2);
        Metadata metadata2 = DataProcessor.getMetadata(httpClient, uri2);
        Assertions.assertNotNull(metadata2);
        URI uri3 = UrlUtils.buildArtifactJarURI(UriUtilsTest.DEFAULT_REPOSITORY2, metadata2, UriUtilsTest.SAMPLE_DEPENDENCY2);
        Assertions.assertNotNull(uri3);
    }
}
