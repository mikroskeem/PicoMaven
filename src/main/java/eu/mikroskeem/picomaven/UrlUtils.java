package eu.mikroskeem.picomaven;

import lombok.NonNull;
import org.apache.maven.artifact.repository.metadata.Metadata;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

class UrlUtils {
    static URI buildGroupMetaURI(@NonNull URI repositoryURI, @NonNull Dependency dependency) {
        return URI.create(repositoryURI.toString() + "/" + concatGroupArtifact(dependency) + "/maven-metadata.xml");
    }
    
    static URI buildArtifactMetaURI(@NonNull URI repositoryURI, @NonNull Metadata metadata, @NonNull Dependency dependency) {
        return URI.create(repositoryURI.toString() + "/" + concatGroupArtifact(metadata) +
                "/" + dependency.getVersion() +  "/maven-metadata.xml");
    }
    
    static URI buildArtifactJarURI(@NonNull URI repositoryURI, Metadata artifactMetadata, @NonNull Dependency dependency) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(repositoryURI.toString());
        stringBuilder.append('/');
        stringBuilder.append(concatGroupArtifact(dependency));
        stringBuilder.append('/');
        stringBuilder.append(dependency.getVersion());
        stringBuilder.append('/');
        if(artifactMetadata != null) {
            stringBuilder.append(formatJarNameFromMeta(dependency, artifactMetadata));
        } else {
            stringBuilder.append(formatJarNameFromDependency(dependency));
        }
        return URI.create(stringBuilder.toString());
    }

    static Path formatLocalPath(@NonNull Path parent, @NonNull Dependency dependency) {
        return Paths.get(parent.toString(),
                dependency.getGroupId().replace('.', '/'),
                dependency.getArtifactId(),
                dependency.getVersion(),
                formatJarNameFromDependency(dependency)
        );
    }

    private static String concatGroupArtifact(@NonNull Dependency dependency) {
        return dependency.getGroupId().replace('.', '/') + "/" + dependency.getArtifactId();
    }

    private static String concatGroupArtifact(@NonNull Metadata metadata) {
        return metadata.getGroupId().replace('.', '/') + "/" + metadata.getArtifactId();
    }

    private static String formatJarNameFromDependency(@NonNull Dependency dependency) {
        return String.format("%s-%s.jar", dependency.getArtifactId(), dependency.getVersion());
    }

    /* Gets always latest version */
    private static String formatJarNameFromMeta(@NonNull Dependency dependency, @NonNull Metadata metadata) {
        try {
            return String.format(
                    "%s-%s-%s-%s.jar",
                    dependency.getArtifactId(),
                    dependency.getVersion().replaceAll("-SNAPSHOT", ""),
                    metadata.getVersioning().getSnapshot().getTimestamp(),
                    metadata.getVersioning().getSnapshot().getBuildNumber()
            );
        } catch (NullPointerException e) {
            /* Dear god, there are so many different formats of maven metadatas... */
            return formatJarNameFromDependency(dependency);
        }
    }
}
