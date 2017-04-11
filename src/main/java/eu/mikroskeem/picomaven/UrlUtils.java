package eu.mikroskeem.picomaven;

import eu.mikroskeem.picomaven.meta.ArtifactMetadata;
import eu.mikroskeem.picomaven.meta.Metadata;
import lombok.NonNull;

import java.net.URI;

class UrlUtils {
    static URI buildGroupMetaURI(@NonNull URI repositoryURI, @NonNull Dependency dependency) {
        return URI.create(repositoryURI.toString() + "/" + concatGroupArtifact(dependency) + "/maven-metadata.xml");
    }
    
    static URI buildArtifactMetaURI(@NonNull URI repositoryURI, @NonNull Metadata metadata, @NonNull Dependency dependency) {
        return URI.create(repositoryURI.toString() + "/" + concatGroupArtifact(metadata) +
                "/" + dependency.getVersion() +  "/maven-metadata.xml");
    }
    
    static URI buildArtifactJarURI(@NonNull URI repositoryURI, ArtifactMetadata artifactMetadata, @NonNull Dependency dependency) {
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
    private static String formatJarNameFromMeta(@NonNull Dependency dependency, @NonNull ArtifactMetadata metadata) {
        return String.format(
                "%s-%s-%s-%s.jar",
                dependency.getArtifactId(),
                dependency.getVersion().replaceAll("-SNAPSHOT", ""),
                metadata.getVersioning().getSnapshot().getTimestamp(),
                metadata.getVersioning().getSnapshot().getBuildNumber()
        );
    }
}
