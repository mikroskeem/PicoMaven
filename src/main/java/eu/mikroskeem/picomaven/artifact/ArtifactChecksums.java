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

package eu.mikroskeem.picomaven.artifact;

import org.checkerframework.checker.nullness.qual.NonNull;

import static eu.mikroskeem.picomaven.artifact.ArtifactChecksum.ChecksumAlgo;
import static eu.mikroskeem.picomaven.artifact.ArtifactChecksum.ChecksumEncoding;

/**
 * @author Mark Vainomaa
 */
public final class ArtifactChecksums {
    private ArtifactChecksums() {}

    @NonNull
    public static ArtifactChecksum of(@NonNull ChecksumAlgo type, @NonNull ChecksumEncoding encoding, @NonNull String checksum) {
        return new ArtifactChecksum(type, encoding, checksum);
    }

    @NonNull
    public static ArtifactChecksum sha1HexSumOf(@NonNull String sha1sum) {
        return new ArtifactChecksum(ChecksumAlgo.SHA1, ChecksumEncoding.HEX, sha1sum);
    }

    @NonNull
    public static ArtifactChecksum sha256HexSumOf(@NonNull String sha256sum) {
        return new ArtifactChecksum(ChecksumAlgo.SHA256, ChecksumEncoding.HEX, sha256sum);
    }

    @NonNull
    public static ArtifactChecksum md5HexSumOf(@NonNull String md5sum) {
        return new ArtifactChecksum(ChecksumAlgo.MD5, ChecksumEncoding.HEX,md5sum);
    }

    @NonNull
    public static ArtifactChecksum sha1Base64SumOf(@NonNull String sha1sum) {
        return new ArtifactChecksum(ChecksumAlgo.SHA1, ChecksumEncoding.BASE64, sha1sum);
    }

    @NonNull
    public static ArtifactChecksum sha256Base64SumOf(@NonNull String sha256sum) {
        return new ArtifactChecksum(ChecksumAlgo.SHA256, ChecksumEncoding.BASE64, sha256sum);
    }

    @NonNull
    public static ArtifactChecksum md5Base64SumOf(@NonNull String md5sum) {
        return new ArtifactChecksum(ChecksumAlgo.MD5, ChecksumEncoding.BASE64, md5sum);
    }
}
