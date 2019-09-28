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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author Mark Vainomaa
 */
public final class TaskUtils {
    // Seriously Java?
    public static <T> CompletableFuture<T> wrapFuture(Future<T> future) {
        if (future instanceof CompletableFuture) {
            return (CompletableFuture<T>) future;
        }

        return CompletableFuture.supplyAsync(() -> SneakyThrow.get(future::get));
    }

    public static <T> CompletableFuture<Void> waitForAll(Collection<? extends CompletableFuture<T>> completableFutures) {
        return CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[0]));
    }

    public static <T> CompletableFuture<Void> waitForAllFutures(Collection<? extends Future<T>> futures) {
        List<CompletableFuture<T>> converted = new ArrayList<>(futures.size());
        for (Future<T> future : futures) {
            converted.add(wrapFuture(future));
        }
        return waitForAll(converted);
    }

    public static <T> void waitForAllUninterruptibly(Collection<? extends CompletableFuture<T>> completableFutures) {
        CompletableFuture[] completableFuturesArray = completableFutures.toArray(new CompletableFuture[0]);
        allOfUninterruptible(completableFuturesArray);
    }

    public static <T> void waitForAllFuturesUninterruptibly(Collection<? extends Future<T>> futures) {
        CompletableFuture[] completableFuturesArray = new CompletableFuture[futures.size()];
        int i = 0;
        for (Future<T> future : futures) {
            completableFuturesArray[i++] = wrapFuture(future);
        }

        allOfUninterruptible(completableFuturesArray);
    }

    private static void allOfUninterruptible(CompletableFuture[] completableFutures) {
        while (true) {
            try {
                CompletableFuture.allOf(completableFutures).get();
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                if (e.getCause() != null) {
                    SneakyThrow.rethrow(e.getCause());
                } else {
                    SneakyThrow.rethrow(e);
                }
            }
        }
    }
}
