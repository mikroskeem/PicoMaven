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

import eu.mikroskeem.picomaven.artifact.ArtifactChecksum;
import eu.mikroskeem.picomaven.artifact.Dependency;
import eu.mikroskeem.picomaven.internal.UrlUtils;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.net.URI;
import java.net.URL;

/**
 * @author Mark Vainomaa
 */
public class UrlUtilsTest {
    public static final URI DEFAULT_REPOSITORY = URI.create("https://repo.maven.apache.org/maven2");
    public static final URI DEFAULT_REPOSITORY2 = URI.create("https://repo.spongepowered.org/maven");
    public static final Dependency SAMPLE_DEPENDENCY = new Dependency("org.ow2.asm", "asm-all", "5.2", ArtifactChecksum.sha1HexSumOf("2ea49e08b876bbd33e0a7ce75c8f371d29e1f10a"));
    public static final Dependency SAMPLE_DEPENDENCY2 = new Dependency("org.spongepowered", "mixin", "0.6.8-SNAPSHOT");
    public static final String SAMPLE_METADATA =
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
    public void testMetadataUriBuilding() throws Exception {
        URL groupMetaUri = UrlUtils.buildGroupMetaURL(DEFAULT_REPOSITORY.toURL(), SAMPLE_DEPENDENCY);
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

        URL artifactMetaUri = UrlUtils.buildArtifactMetaURL(DEFAULT_REPOSITORY.toURL(), metadata, SAMPLE_DEPENDENCY);
        Assertions.assertEquals("https://repo.maven.apache.org/maven2/org/ow2/asm/asm-all/5.2/maven-metadata.xml",
                artifactMetaUri.toString());
    }

    @Test
    public void testArtifactJarUriBuilding() throws Exception {
        Metadata metadata = new MetadataXpp3Reader().read(new StringReader(SAMPLE_ARTIFACT_METADATA));
        URL artifactUri = UrlUtils.buildArtifactURL(DEFAULT_REPOSITORY2.toURL(), metadata, SAMPLE_DEPENDENCY2, "jar");
        String exp = "https://repo.spongepowered.org/maven/org/spongepowered/mixin/0.6.8-SNAPSHOT/mixin-0.6.8-20170320.130808-7.jar";
        Assertions.assertEquals(exp, artifactUri.toString());
    }

    @Test
    public void testArtifactJarUriBuildingWithoutMetadata() throws Exception {
        URL artifactUri = UrlUtils.buildArtifactURL(DEFAULT_REPOSITORY.toURL(), null, SAMPLE_DEPENDENCY, "jar");
        String exp = "https://repo.maven.apache.org/maven2/org/ow2/asm/asm-all/5.2/asm-all-5.2.jar";
        Assertions.assertEquals(exp, artifactUri.toString());
    }
}
