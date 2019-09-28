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

import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/**
 * @author Mark Vainomaa
 */
public final class FileUtils {
    private FileUtils() {}

    @NonNull
    public static Path getTemporaryFileName(@NonNull Path target) {
        return target.resolveSibling(target.getFileName() + ".tmp");
    }

    public static void writeAtomicReplace(@NonNull Path target, @NonNull Path temporary, byte @NonNull [] data) throws IOException {
        // Create parent directory if target file does not exist.
        if (Files.notExists(target)) {
            Files.createDirectories(target.getParent());
        }

        // Write to temporary file
        Files.write(temporary, data, StandardOpenOption.CREATE, StandardOpenOption.WRITE);

        // Atomic replace
        Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    public static void writeAtomicReplace(@NonNull Path target, byte @NonNull [] data) throws IOException {
        Path temporary = getTemporaryFileName(target);
        writeAtomicReplace(target, temporary, data);
    }
}
