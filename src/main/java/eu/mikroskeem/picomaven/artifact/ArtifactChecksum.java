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

import eu.mikroskeem.picomaven.internal.SneakyThrow;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Objects;
import java.util.function.BiFunction;

/**
 * An artifact checksum
 *
 * @author Mark Vainomaa
 */
public final class ArtifactChecksum {
    private final ChecksumAlgo algo;
    private final ChecksumEncoding encoding;
    private final String checksum;

    /**
     * Constructs a new artifact checksum object
     *
     * @param algo Checksum type
     * @param encoding Checksum encoding
     * @param checksum Checksum
     */
    public ArtifactChecksum(ArtifactChecksum.@NonNull ChecksumAlgo algo,
                            ArtifactChecksum.@NonNull ChecksumEncoding encoding,
                            @NonNull String checksum) {
        this.algo = algo;
        this.encoding = encoding;
        this.checksum = checksum;
    }

    /**
     * Gets checksum type
     *
     * @return Checksum type
     */
    @NonNull
    public ChecksumAlgo getAlgo() {
        return algo;
    }

    @NonNull
    public ChecksumEncoding getEncoding() {
        return encoding;
    }

    /**
     * Gets checksum
     *
     * @return Checksum
     */
    @NonNull
    public String getChecksum() {
        return checksum;
    }

    @Override
    public String toString() {
        return "ArtifactChecksum{" +
                "algo=" + algo +
                ", encoding=" + encoding +
                ", checksum='" + checksum + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArtifactChecksum that = (ArtifactChecksum) o;
        return algo == that.algo &&
                encoding == that.encoding &&
                checksum.equals(that.checksum);
    }

    @Override
    public int hashCode() {
        return Objects.hash(algo, encoding, checksum);
    }

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

    /**
     * Supported checksum types
     */
    public enum ChecksumAlgo {
        /**
         * SHA-1
         */
        SHA1("sha1", "SHA1"),

        /**
         * MD-5
         */
        MD5("md5", "MD5"),

        /**
         * SHA-256
         */
        SHA256("sha256", "SHA256")
        ;

        private final String ext;
        private final String messageDigestAlgo;

        ChecksumAlgo(@NonNull String ext, @NonNull String messageDigestAlgo) {
            this.ext = ext;
            this.messageDigestAlgo = messageDigestAlgo;
        }

        /**
         * Gets file extension for given checksum algorithm
         *
         * @return File extension
         */
        @NonNull
        public String getExt() {
            return ext;
        }

        /**
         * Shortcut to get MessageDigest for given algorithm
         *
         * @return {@link MessageDigest} for given algorithm
         */
        @NonNull
        public MessageDigest getMessageDigest() {
            return SneakyThrow.get(() -> MessageDigest.getInstance(this.messageDigestAlgo));
        }
    }

    /**
     * Checksum encodings
     */
    public enum ChecksumEncoding {
        /**
         * Hex checksum (most common)
         */
        HEX((md, checksum) -> {
            StringBuilder sb = new StringBuilder();
            for (byte b : md.digest()) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString().equalsIgnoreCase(checksum);
        }),

        /**
         * Base64 checksum
         */
        BASE64((md, checksum) -> {
            return new String(Base64.getEncoder().encode(md.digest()), StandardCharsets.UTF_8).equals(checksum);
        }),
        ;

        private final BiFunction<MessageDigest, String, Boolean> verifier;

        ChecksumEncoding(@NonNull BiFunction<MessageDigest, String, Boolean> verifier) {
            this.verifier = verifier;
        }

        /**
         * Verifies checksum
         *
         * @param md Finished {@link MessageDigest}
         * @param checksum Checksum to check against
         * @return Whether checksums match or not
         */
        public boolean verify(@NonNull MessageDigest md, @NonNull String checksum) {
            return verifier.apply(md, checksum);
        }
    }
}
