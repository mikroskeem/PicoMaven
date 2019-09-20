package eu.mikroskeem.picomaven;

import eu.mikroskeem.picomaven.internal.SneakyThrow;
import fi.iki.elonen.NanoHTTPD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author Mark Vainomaa
 */
public final class RepositoryServer extends NanoHTTPD {
    private static final Logger logger = LoggerFactory.getLogger(RepositoryServer.class);

    private final Path repositoryRoot;

    public RepositoryServer(Path repositoryRoot) {
        super("127.0.0.1", pickPort());
        this.repositoryRoot = repositoryRoot;
        SneakyThrow.run(() -> this.start(NanoHTTPD.SOCKET_READ_TIMEOUT, true));
    }

    public URL getRepositoryURL() {
        return SneakyThrow.get(() -> new URL("http://" + this.getHostname() + ":" + this.getListeningPort()));
    }

    @Override
    public Response serve(IHTTPSession session) {
        // Much trash, very nom
        if (session.getMethod() == Method.GET || session.getMethod() == Method.HEAD) {
            Path resource = Paths.get(this.repositoryRoot.toString() + session.getUri());
            if (Files.exists(resource)) {
                InputStream stream = SneakyThrow.get(() -> Files.newInputStream(resource));
                long available = SneakyThrow.get(stream::available);

                if (session.getMethod() == Method.GET) {
                    return newFixedLengthResponse(Response.Status.OK, "application/octet-stream", stream, available);
                } else {
                    // uhhhhh
                }
            }
        }

        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not found");
    }

    private static int pickPort() {
        int port;
        while (true) {
            port = ThreadLocalRandom.current().nextInt(20000, 35000);

            try {
                ServerSocket socket = new ServerSocket(port);
                socket.close();

                logger.info("Bound to port {}", port);
                return port;
            } catch (IOException e) {
                logger.info("Failed to bind on port {} ({}), trying next...", port, e.getMessage());
            }
        }
    }
}
