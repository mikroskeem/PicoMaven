/*
 * This file is part of project PicoMaven, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2017-2019 Mark Vainomaa <mikroskeem@mikroskeem.eu>
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

import eu.mikroskeem.picomaven.artifact.Dependency;
import eu.mikroskeem.picomaven.internal.DataProcessor;
import eu.mikroskeem.picomaven.internal.SneakyThrow;
import eu.mikroskeem.picomaven.internal.UrlUtils;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * @author Mark Vainomaa
 */
public class ClientTest {
    private static final URI MAVEN_CENTRAL_REPOSITORY = URI.create("https://repo.maven.apache.org/maven2");
    private static final URI MOJANG_REPOSITORY = URI.create("https://libraries.minecraft.net");

    private static File downloadDir;
    static {
        downloadDir = new File("./target/_downloadTest" + Math.random());
        downloadDir.mkdirs();
    }

    @Test
    public void testDefaultBuilder() throws Exception {
        List<Dependency> dependencies = Arrays.asList(UrlUtilsTest.SAMPLE_DEPENDENCY, UrlUtilsTest.SAMPLE_DEPENDENCY2);
        try (
            PicoMaven picoMaven = new PicoMaven.Builder()
                .withDownloadPath(downloadDir.toPath())
                .withRepositories(Arrays.asList(MAVEN_CENTRAL_REPOSITORY, MOJANG_REPOSITORY, UrlUtilsTest.DEFAULT_REPOSITORY2))
                .withDependencies(dependencies)
                .withTransitiveDependencyProcessors(Arrays.asList(
                        dep -> {
                            if ("org.apache.logging.log4j".equalsIgnoreCase(dep.getGroupId()))
                                dep.setAllowed(false);
                            if ("commons-io".equalsIgnoreCase(dep.getGroupId()))
                                dep.setAllowed(false);
                            if (dep.getGroupId() != null && dep.getGroupId().startsWith("net."))
                                dep.setAllowed(false);
                            if (dep.getGroupId() != null && dep.getGroupId().startsWith("com.google"))
                                dep.setAllowed(false);
                        }
                ))
                .build()
        ) {
            Map<Dependency, Future<DownloadResult>> downloads = picoMaven.downloadAllArtifacts();
            int downloaded = 0;
            for (Future<DownloadResult> value : downloads.values()) {
                while (true) {
                    try {
                        DownloadResult res = value.get();
                        if (res.isSuccess()) {
                            downloaded++;
                        } else {
                            System.err.println(res);
                        }
                        break;
                    } catch (InterruptedException ignored) {
                    } catch (Exception e) {
                        // This is here because I am lazy - I could run Gradle with --stacktrace but nyeehhh
                        e.printStackTrace();
                        SneakyThrow.rethrow(e);
                    }
                }
            }

            Assertions.assertEquals(dependencies.size(), downloaded);
        }
    }

    @Test
    public void testDataFetching() throws Exception {
        URL url = UrlUtils.buildGroupMetaURL(UrlUtilsTest.DEFAULT_REPOSITORY2.toURL(), UrlUtilsTest.SAMPLE_DEPENDENCY2);
        Metadata metadata = DataProcessor.getMetadata(url);
        Assertions.assertNotNull(metadata);
        URL url2 = UrlUtils.buildArtifactMetaURL(UrlUtilsTest.DEFAULT_REPOSITORY2.toURL(), metadata, UrlUtilsTest.SAMPLE_DEPENDENCY2);
        Metadata metadata2 = DataProcessor.getMetadata(url2);
        Assertions.assertNotNull(metadata2);
        URL url3 = UrlUtils.buildArtifactURL(UrlUtilsTest.DEFAULT_REPOSITORY2.toURL(), metadata2, UrlUtilsTest.SAMPLE_DEPENDENCY2, "jar");
        Assertions.assertNotNull(url3);
    }
}
