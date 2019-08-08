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

package eu.mikroskeem.picomaven.internal;

import eu.mikroskeem.picomaven.artifact.ArtifactChecksum;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * @author Mark Vainomaa
 */
public final class DataProcessor {
    @Nullable
    public static Metadata getMetadata(@NonNull URL url) throws IOException {
        URLConnection connection = UrlUtils.openConnection(url);
        try (InputStream is = connection.getInputStream()) {
            UrlUtils.ensureSuccessfulRequest(connection);
            return new MetadataXpp3Reader().read(is);
        } catch (FileNotFoundException e) {
            return null;
        } catch (XmlPullParserException e) {
            throw new IOException("Unable to parse XML", e);
        }
    }

    @Nullable
    public static Model getPom(@NonNull URL url) throws IOException {
        URLConnection connection = UrlUtils.openConnection(url);
        try (InputStream is = connection.getInputStream()) {
            UrlUtils.ensureSuccessfulRequest(connection);
            return new MavenXpp3Reader().read(is);
        } catch (FileNotFoundException e) {
            return null;
        } catch (XmlPullParserException e) {
            throw new IOException("Unable to parse XML", e);
        }
    }

    @NonNull
    public static CompletableFuture<@Nullable ArtifactChecksum> getArtifactChecksum(@NonNull Executor executor,
                                                                                    @NonNull URL artifactUrl,
                                                                                    ArtifactChecksum.ChecksumAlgo cst) {
        final URL url = SneakyThrow.get(() -> new URL(artifactUrl.toString() + "." + cst.getExt()));

        return CompletableFuture.supplyAsync(() -> {
            URLConnection connection = SneakyThrow.get(() -> UrlUtils.openConnection(url));
            try (BufferedReader is = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String response = is.lines().collect(Collectors.joining());
                String[] splitted = response.split("\\s", 2);
                String checksum = splitted.length == 2 ? splitted[0] : response;
                return new ArtifactChecksum(cst, ArtifactChecksum.ChecksumEncoding.HEX, checksum);
            } catch (FileNotFoundException e) {
                return null;
            } catch (IOException e) {
                SneakyThrow.rethrow(e);
            }
            return null;
        }, executor);
    }

    public static boolean verifyChecksum(@NonNull ArtifactChecksum artifactChecksum, @NonNull byte[] data) {
        MessageDigest md = artifactChecksum.getAlgo().getMessageDigest();
        md.update(data);
        return artifactChecksum.getEncoding().verify(md, artifactChecksum.getChecksum());
    }
}
