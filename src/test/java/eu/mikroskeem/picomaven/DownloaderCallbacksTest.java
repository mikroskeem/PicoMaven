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

import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

/**
 * @author Mark Vainomaa
 */
@SuppressWarnings("deprecation")
public class DownloaderCallbacksTest {
    private final static Dependency DUMMY_DEPENDENCY = new Dependency("foo", "bar", "0.1");

    /*
     * Leaves onFailure's both variants unimplemented
     */
    @Test
    public void testBadImplementation() {
        class BadImpl implements DownloaderCallbacks {
            @Override
            public void onSuccess(@NonNull Dependency dependency, @NonNull Path dependencyPath) {

            }
        }

        DownloaderCallbacks callbacks = new BadImpl();
        Assertions.assertThrows(AbstractMethodError.class, () -> {
            callbacks.onFailure(DUMMY_DEPENDENCY, new Exception("foo"));
        });
        Assertions.assertThrows(AbstractMethodError.class, () -> {
            callbacks.onFailure(DUMMY_DEPENDENCY, new IOException("bar"));
        });
    }

    /*
     * Implements only deprecated onFailure method (old code)
     */
    @Test
    public void testDeprecatedImplementation() {
        class DeprecatedImpl implements DownloaderCallbacks {
            @Override
            public void onSuccess(@NonNull Dependency dependency, @NonNull Path dependencyPath) {

            }

            @Override
            public void onFailure(@NonNull Dependency dependency, @NonNull IOException exception) {

            }
        }

        DownloaderCallbacks callbacks = new DeprecatedImpl();
        callbacks.onFailure(DUMMY_DEPENDENCY, new Exception("foo"));
        callbacks.onFailure(DUMMY_DEPENDENCY, new IOException("bar"));
    }

    /*
     * Implements new onFailue method
     */
    @Test
    public void testGoodImplementation() {
        class GoodImpl implements DownloaderCallbacks {
            @Override
            public void onSuccess(@NonNull Dependency dependency, @NonNull Path dependencyPath) {

            }

            @Override
            public void onFailure(@NonNull Dependency dependency, @NonNull Exception exception) {

            }
        }

        DownloaderCallbacks callbacks = new GoodImpl();
        callbacks.onFailure(DUMMY_DEPENDENCY, new Exception("foo"));
        callbacks.onFailure(DUMMY_DEPENDENCY, new IOException("bar"));
    }
}
