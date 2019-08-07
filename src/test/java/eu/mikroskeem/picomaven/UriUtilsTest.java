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

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.net.URI;

/**
 * @author Mark Vainomaa
 */
public class UriUtilsTest {
    public final static URI DEFAULT_REPOSITORY = URI.create("https://repo.maven.apache.org/maven2");
    public final static URI DEFAULT_REPOSITORY2 = URI.create("https://repo.spongepowered.org/maven");
    public final static Dependency SAMPLE_DEPENDENCY = new Dependency("org.ow2.asm", "asm-all", "5.2");
    public final static Dependency SAMPLE_DEPENDENCY2 = new Dependency("org.spongepowered", "mixin", "0.6.8-SNAPSHOT");
    public final static String SAMPLE_METADATA =
            "<metadata>\n" +
            "  <groupId>org.ow2.asm</groupId>\n" +
            "  <artifactId>asm-all</artifactId>\n" +
            "  <versioning>\n" +
            "    <latest>6.0_ALPHA</latest>\n" +
            "    <release>6.0_ALPHA</release>\n" +
            "    <versions>\n" +
            "      <version>4.0</version>\n" +
            "      <version>4.1</version>\n" +
            "      <version>4.2</version>\n" +
            "      <version>5.0</version>\n" +
            "      <version>5.0_ALPHA</version>\n" +
            "      <version>5.0_BETA</version>\n" +
            "      <version>5.0.1</version>\n" +
            "      <version>5.0.2</version>\n" +
            "      <version>5.0.3</version>\n" +
            "      <version>5.0.4</version>\n" +
            "      <version>5.1</version>\n" +
            "      <version>5.2</version>\n" +
            "      <version>6.0_ALPHA</version>\n" +
            "    </versions>\n" +
            "    <lastUpdated>20161223094512</lastUpdated>\n" +
            "  </versioning>\n" +
            "</metadata>";
    private final static String SAMPLE_ARTIFACT_METADATA =
            "<metadata>\n" +
            "  <groupId>org.spongepowered</groupId>\n" +
            "  <artifactId>mixin</artifactId>\n" +
            "  <version>0.6.8-SNAPSHOT</version>\n" +
            "  <versioning>\n" +
            "    <snapshot>\n" +
            "      <timestamp>20170320.130808</timestamp>\n" +
            "      <buildNumber>7</buildNumber>\n" +
            "    </snapshot>\n" +
            "    <lastUpdated>20170320130808</lastUpdated>\n" +
            "  </versioning>\n" +
            "</metadata>";

    @Test
    public void testMetadataUriBuilding() {
        URI groupMetaUri = UrlUtils.buildGroupMetaURI(DEFAULT_REPOSITORY, SAMPLE_DEPENDENCY);
        Assertions.assertEquals("https://repo.maven.apache.org/maven2/org/ow2/asm/asm-all/maven-metadata.xml",
                groupMetaUri.toString());
    }

    @Test
    public void testMetadataXmlObjectMapping() throws Exception {
        Metadata metadata = new MetadataXpp3Reader().read(new StringReader(SAMPLE_METADATA));
    }

    @Test
    public void testArtifactMetadataXmlObjectMapping() throws Exception {
        Metadata metadata = new MetadataXpp3Reader().read(new StringReader(SAMPLE_ARTIFACT_METADATA));
    }

    @Test
    public void testArtifactMetadataUriBuilding() throws Exception {
        Metadata metadata = new MetadataXpp3Reader().read(new StringReader(SAMPLE_METADATA));

        URI artifactMetaUri = UrlUtils.buildArtifactMetaURI(DEFAULT_REPOSITORY, metadata, SAMPLE_DEPENDENCY);
        Assertions.assertEquals("https://repo.maven.apache.org/maven2/org/ow2/asm/asm-all/5.2/maven-metadata.xml",
                artifactMetaUri.toString());
    }

    @Test
    public void testArtifactJarUriBuilding() throws Exception {
        Metadata metadata = new MetadataXpp3Reader().read(new StringReader(SAMPLE_ARTIFACT_METADATA));
        URI artifactUri = UrlUtils.buildArtifactJarURI(DEFAULT_REPOSITORY2, metadata, SAMPLE_DEPENDENCY2);
        String exp = "https://repo.spongepowered.org/maven/org/spongepowered/mixin/0.6.8-SNAPSHOT/mixin-0.6.8-20170320.130808-7.jar";
        Assertions.assertEquals(exp, artifactUri.toString());
    }

    @Test
    public void testArtifactJarUriBuildingWithoutMetadata() throws Exception {
        URI artifactUri = UrlUtils.buildArtifactJarURI(DEFAULT_REPOSITORY, null, SAMPLE_DEPENDENCY);
        String exp = "https://repo.maven.apache.org/maven2/org/ow2/asm/asm-all/5.2/asm-all-5.2.jar";
        Assertions.assertEquals(exp, artifactUri.toString());
    }
}
