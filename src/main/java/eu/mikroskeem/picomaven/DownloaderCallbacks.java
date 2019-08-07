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

import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.IOException;
import java.nio.file.Path;

/**
 * @author Mark Vainomaa
 */
public interface DownloaderCallbacks {
    /**
     * Invoked when dependency download succeeds
     *
     * @param dependency Dependency object
     * @param dependencyPath Dependency path
     * @see Dependency
     * @see Path
     */
    void onSuccess(@NonNull Dependency dependency, @NonNull Path dependencyPath);

    /**
     * Invoked when dependency download fails
     *
     * @param dependency Dependency object
     * @param exception Exception
     * @see Dependency
     * @see Exception
     */
    @SuppressWarnings("deprecation")
    default void onFailure(@NonNull Dependency dependency, @NonNull Exception exception) {
        try {
            java.lang.reflect.Method method = this.getClass().getMethod("onFailure", Dependency.class, IOException.class);
            if(method.isDefault()) throw new AbstractMethodError(
                    "Either one of 'onEither' method must be implemented for DownloaderCallbacks");

            /* Pass execution to #onFailure(Dependency, IOException) */
            onFailure(dependency, new IOException(exception));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Invoked when dependency download fails
     *
     * @param dependency Dependency object
     * @param exception IOException
     * @see Dependency
     * @see IOException
     * @deprecated Use and override {@link #onFailure(Dependency, Exception)} instead
     */
    @Deprecated
    default void onFailure(@NonNull Dependency dependency, @NonNull IOException exception) {
        onFailure(dependency, (Exception) exception);
    }
}
