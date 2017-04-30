package eu.mikroskeem.picomaven;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.NonNull;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.net.URI;

/**
 * @author Mark Vainomaa
 */
class DataProcessor {
    static ArtifactMetadata getRepositoryMetadata(@NonNull OkHttpClient client, @NonNull URI groupMeta) throws IOException {
        ObjectMapper objectMapper = new XmlMapper();
        Request request = new Request.Builder()
                .url(HttpUrl.get(groupMeta))
                .build();
        try(Response response = client.newCall(request).execute()) {
            if(response.isSuccessful()) {
                return objectMapper.readValue(response.body().bytes(), ArtifactMetadata.class);
            }
        }
        return null;
    }

    static ArtifactMetadata getArtifactMetadata(@NonNull OkHttpClient client, @NonNull URI artifactMeta) throws IOException {
        ObjectMapper objectMapper = new XmlMapper();
        Request request = new Request.Builder()
                .url(HttpUrl.get(artifactMeta))
                .build();
        try(Response response = client.newCall(request).execute()) {
            if(response.isSuccessful()) {
                return objectMapper.readValue(response.body().bytes(), ArtifactMetadata.class);
            }
        }
        return null;
    }
}
