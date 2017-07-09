package eu.mikroskeem.picomaven;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;

/**
 * @author Mark Vainomaa
 */
class DataProcessor {
    @Nullable
    @Contract("null, null -> fail")
    static Metadata getMetadata(OkHttpClient client, URI url) throws IOException {
        Request request = new Request.Builder()
                .url(HttpUrl.get(url))
                .build();
        try(Response response = client.newCall(request).execute()) {
            if(response.isSuccessful())
                return new MetadataXpp3Reader().read(response.body().charStream());
        } catch (XmlPullParserException e) {
            throw new IOException(e);
        }
        return null;
    }
}
