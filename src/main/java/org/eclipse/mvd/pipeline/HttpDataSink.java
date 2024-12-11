package org.eclipse.edc.mvd.pipeline;

import org.eclipse.edc.connector.dataplane.http.spi.HttpRequestParams;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.connector.dataplane.util.sink.ParallelSink;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.mvd.params.HttpRequestFactory;

import java.util.List;
import java.util.Objects;

import static java.lang.String.format;

/**
 * Writes data in a streaming fashion to an HTTP endpoint.
 */
public class HttpDataSink extends ParallelSink {
    private static final StreamResult<Void> ERROR_WRITING_DATA = StreamResult.error("Error writing data");

    private HttpRequestParams params;
    private EdcHttpClient httpClient;
    private HttpRequestFactory requestFactory;

    @Override
    protected StreamResult<Void> transferParts(List<DataSource.Part> parts) {
        for (var part : parts) {
            var request = requestFactory.toRequest(params, part::openStream);
            try (var response = httpClient.execute(request)) {
                if (!response.isSuccessful()) {
                    monitor.severe(format("Error {%s: %s} received writing HTTP data %s to endpoint %s for request: %s",
                            response.code(), response.message(), part.name(), request.url().url(), request));
                    return ERROR_WRITING_DATA;
                }

                return StreamResult.success();
            } catch (Exception e) {
                monitor.severe(format("Error writing HTTP data %s to endpoint %s for request: %s", part.name(), request.url().url(), request), e);
                return ERROR_WRITING_DATA;
            }
        }
        return StreamResult.success();
    }

    private HttpDataSink() {
    }

    public static class Builder extends ParallelSink.Builder<Builder, HttpDataSink> {

        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {
            super(new HttpDataSink());
        }

        public Builder params(HttpRequestParams params) {
            sink.params = params;
            return this;
        }

        public Builder httpClient(EdcHttpClient httpClient) {
            sink.httpClient = httpClient;
            return this;
        }

        public Builder requestFactory(HttpRequestFactory requestFactory) {
            sink.requestFactory = requestFactory;
            return this;
        }

        @Override
        protected void validate() {
            Objects.requireNonNull(sink.requestFactory, "requestFactory");
        }
    }
}
