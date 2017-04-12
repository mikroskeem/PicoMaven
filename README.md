# PicoMaven

[![Build Status](https://travis-ci.org/mikroskeem/PicoMaven.svg?branch=master)](https://travis-ci.org/mikroskeem/PicoMaven)

Download libraries from Maven repositories before app start

## How to use?

```java
public class MyApp {
    public static void main(String... args) {
        List<Dependency> dependencies = Arrays.asList(
                new Dependency("org.ow2.asm", "asm-all", "5.2"),
                new Dependency("io.github.lukehutch", "fast-classpath-scanner", "2.0.18")
        );
        PicoMaven.Builder picoMavenBase = new PicoMaven.Builder()
                .withDownloadPath(Paths.get(".", "libraries"))
                .withRepositories(Collections.singletonList(Constants.MAVEN_CENTRAL_REPOSITORY))
                .withDependencies(dependencies);
        
        try(PicoMaven picoMaven = picoMavenBase.build()) {
            List<Path> downloaded = picoMaven.downloadAll();
            URL[] urls = downloaded.stream().map(MyApp::conv).collect(Collectors.toList()).toArray(new URL[0]);
            
            URLClassLoader ucl = URLClassLoader.newInstance(urls);
            
            
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    @SneakyThrows(MalformedURLException.class)
    private static URL conv(Path path){ return path.toUri().toURL(); }
}
```