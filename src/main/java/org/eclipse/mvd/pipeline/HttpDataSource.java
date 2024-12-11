package org.eclipse.edc.mvd.pipeline;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.eclipse.edc.connector.dataplane.http.spi.HttpRequestParams;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.mvd.model.Building;
import org.eclipse.edc.mvd.params.HttpRequestFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult.error;
import static org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult.success;

public class HttpDataSource implements DataSource {
  private static final int FORBIDDEN = 401;
  private static final int NOT_AUTHORIZED = 403;
  private static final int NOT_FOUND = 404;

  private String name;
  private HttpRequestParams params;
  private String requestId;
  private Monitor monitor;
  private EdcHttpClient httpClient;
  private HttpRequestFactory requestFactory;

  @Override
  public StreamResult<Stream<Part>> openPartStream() {
    var request = requestFactory.toRequest(params);
    monitor.debug(() -> "Executing HTTP request: " + request.url());
    // NB: Do not close the response as the body input stream needs to be read after
    // this method returns. The response closes the body stream.
    try (var response = httpClient.execute(request)) {
      if (response.isSuccessful()) {
        var body = response.body();
        if (body == null) {
          throw new EdcException(format("Received empty response body transferring HTTP data for request %s: %s",
              requestId, response.code()));
        }
        // Anonymize data in body
        // String responseBodyString = body.string();
        // InputStream processedBodyStream = processBody(responseBodyString);
        // return success(Stream.of(new HttpPart(name, processedBodyStream)));
        return success(Stream.of(new HttpPart(name, body.byteStream())));
      } else {
        try {
          if (NOT_AUTHORIZED == response.code() || FORBIDDEN == response.code()) {
            return StreamResult.notAuthorized();
          } else if (NOT_FOUND == response.code()) {
            return StreamResult.notFound();
          } else {
            return error(format("Received code transferring HTTP data: %s - %s.", response.code(), response.message()));
          }
        } finally {
          try {
            response.close();
          } catch (Exception e) {
            monitor.info("Error closing failed response", e);
          }
        }
      }
    } catch (IOException e) {
      throw new EdcException(e);
    }
  }

  private ByteArrayInputStream processBody(String bodyAsString) throws IOException {
    if (!isValidJson(bodyAsString)) {
      monitor.warning("Invalid JSON format. Skipping anonymization.");
      return new ByteArrayInputStream(bodyAsString.getBytes(StandardCharsets.UTF_8));
    }
    String anonymizedBodyAsString = anonymizeBuildingData(bodyAsString);
    if (!isValidJson(anonymizedBodyAsString)) {
      monitor.warning("Anonymization resulted in invalid JSON. Returning original body.");
      return new ByteArrayInputStream(bodyAsString.getBytes(StandardCharsets.UTF_8));
    }
    return new ByteArrayInputStream(anonymizedBodyAsString.getBytes(StandardCharsets.UTF_8));
  }

  private String anonymizeBuildingData(String bodyAsString) {
    final Gson gson = new Gson();
    Building building;
    try {
      building = gson.fromJson(bodyAsString, Building.class);
    } catch (JsonSyntaxException e) {
      monitor.warning("Failed to process JSON body: Invalid JSON format or not a Building object.");
      return bodyAsString;
    }
    if (building == null) {
      monitor.warning("Building object is null after JSON parsing. Skipping anonymization.");
      return bodyAsString;
    }
    List<String> providersToAnonymize = List.of("aggregationProvider");
    // Perform anonymization only if the data trustee is in the list of providers to
    // anonymize.
    if (providersToAnonymize.contains(building.getDataTrustee())) {
      String originalFirstName = building.getFirstName();
      String anonymizedFirstName = originalFirstName == null ? null : "*".repeat(originalFirstName.length());
      String originalLastName = building.getLastName();
      String anonymizedLastName = originalLastName == null ? null : "*".repeat(originalLastName.length());
      building.setFirstName(anonymizedFirstName);
      building.setLastName(anonymizedLastName);
    }
    return gson.toJson(building);
  }

  private boolean isValidJson(String jsonString) {
    try {
      new Gson().fromJson(jsonString, Object.class);
      return true;
    } catch (JsonSyntaxException ex) {
      return false;
    }
  }

  private HttpDataSource() {
  }

  public static class Builder {
    private final HttpDataSource dataSource;

    public static Builder newInstance() {
      return new Builder();
    }

    private Builder() {
      dataSource = new HttpDataSource();
    }

    public Builder params(HttpRequestParams params) {
      dataSource.params = params;
      return this;
    }

    public Builder name(String name) {
      dataSource.name = name;
      return this;
    }

    public Builder requestId(String requestId) {
      dataSource.requestId = requestId;
      return this;
    }

    public Builder httpClient(EdcHttpClient httpClient) {
      dataSource.httpClient = httpClient;
      return this;
    }

    public Builder monitor(Monitor monitor) {
      dataSource.monitor = monitor;
      return this;
    }

    public Builder requestFactory(HttpRequestFactory requestFactory) {
      dataSource.requestFactory = requestFactory;
      return this;
    }

    public HttpDataSource build() {
      Objects.requireNonNull(dataSource.requestId, "requestId");
      Objects.requireNonNull(dataSource.httpClient, "httpClient");
      Objects.requireNonNull(dataSource.monitor, "monitor");
      Objects.requireNonNull(dataSource.requestFactory, "requestFactory");
      return dataSource;
    }
  }

  private record HttpPart(String name, InputStream content) implements Part {
    @Override
    public InputStream openStream() {
      return content;
    }
  }
}
