package titan.ccp.kiekerbridge.raritan.simulator;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Push messages via HTTP to a configured destination ({@link URI}).
 */
public class HttpPusher {

  private static final int PUSH_TIMEOUT_SECONDS = 10;

  private static final HttpClient.Version HTTP_VERSION = Version.HTTP_1_1;

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpPusher.class);

  private final HttpClient client = HttpClient.newBuilder().version(HTTP_VERSION).build();

  private final URI pushUri;

  public HttpPusher(final URI pushUri) {
    this.pushUri = pushUri;
  }

  /**
   * Push the passed message.
   */
  public void sendMessage(final String message) {
    final HttpRequest request = HttpRequest.newBuilder()
        .uri(this.pushUri)
        .timeout(Duration.ofSeconds(PUSH_TIMEOUT_SECONDS))
        .POST(HttpRequest.BodyPublishers.ofString(message))
        .build();
    final BodyHandler<Void> bodyHandler = HttpResponse.BodyHandlers.discarding();

    this.client.sendAsync(request, bodyHandler).thenAccept(r -> {
      LOGGER.debug("Pushed message to '{}'. Received status code '{}'.", r.request(),
          r.statusCode());
    }).exceptionally(e -> {
      LOGGER.warn("Failed to push message.", e);
      return null;
    });

  }
}
