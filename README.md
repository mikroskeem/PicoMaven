# PicoMaven

[![Build Status](https://travis-ci.org/mikroskeem/PicoMaven.svg?branch=master)](https://travis-ci.org/mikroskeem/PicoMaven)

Download libraries from Maven repositories before app start

## How to use?

```java
package my.app;

import eu.mikroskeem.picomaven.artifact.Dependency;

import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class MyApp {
    private final static URI MAVEN_CENTRAL_REPOSITORY = URI.create("https://repo.maven.apache.org/maven2");

    public static void main(String... args) {
        List<Dependency> dependencies = Arrays.asList(
                new Dependency("org.ow2.asm", "asm-all", "5.2"),
                new Dependency("io.github.lukehutch", "fast-classpath-scanner", "2.0.18")
        );
        PicoMaven.Builder picoMavenBase = new PicoMaven.Builder()
                .withDownloadPath(Paths.get(".", "libraries"))
                .withRepositories(Collections.singletonList(MAVEN_CENTRAL_REPOSITORY))
                .withDependencies(dependencies);

        try (PicoMaven picoMaven = picoMavenBase.build()) {
            List<Path> downloaded = new LinkedList<>();
            picoMaven.downloadAllArtifacts().values().stream().map(MyApp::getFuture).forEach(downloadResult -> {
                if (downloadResult.isSuccess()) {
                    downloaded.add(downloadResult.getArtifactPath());
                }
            });
            
            URL[] urls = downloaded.stream().map(MyApp::conv).collect(Collectors.toList()).toArray(new URL[0]);
            URLClassLoader ucl = URLClassLoader.newInstance(urls);
            
            // Do your things with UCL
        }
    }

    private static <T> T getFuture(Future<T> future) {
        while (true) {
            try {
                return future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    private static URL conv(Path path) {
        try {
            return path.toUri().toURL();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```