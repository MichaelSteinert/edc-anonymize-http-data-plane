package org.eclipse.edc.mvd.pipeline;

import org.eclipse.edc.connector.dataplane.http.spi.HttpRequestParamsProvider;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSourceFactory;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.HttpDataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.eclipse.edc.mvd.params.HttpRequestFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Instantiates {@link HttpDataSource}s for requests whose source data type is {@link HttpDataAddress#HTTP_DATA}.
 */
public class HttpDataSourceFactory implements DataSourceFactory {

    private final EdcHttpClient httpClient;
    private final HttpRequestParamsProvider requestParamsProvider;
    private final Monitor monitor;
    private final HttpRequestFactory requestFactory;
    private final static String HTTP_DATA_TYPE = "HttpDataAnonymize";

    public HttpDataSourceFactory(EdcHttpClient httpClient, HttpRequestParamsProvider requestParamsProvider, Monitor monitor, HttpRequestFactory requestFactory) {
        this.httpClient = httpClient;
        this.requestParamsProvider = requestParamsProvider;
        this.monitor = monitor;
        this.requestFactory = requestFactory;
    }

    @Override
    public boolean canHandle(DataFlowRequest request) {
        return HTTP_DATA_TYPE.equals(request.getSourceDataAddress().getType());
    }

    @Override
    public @NotNull Result<Void> validateRequest(DataFlowRequest request) {
        try {
            createSource(request);
        } catch (Exception e) {
            return Result.failure("Failed to build HttpDataSource: " + e.getMessage());
        }
        return Result.success();
    }

    @Override
    public DataSource createSource(DataFlowRequest request) {
        var dataAddress = HttpDataAddress.Builder.newInstance()
                .copyFrom(request.getSourceDataAddress())
                .build();
        return HttpDataSource.Builder.newInstance()
                .httpClient(httpClient)
                .monitor(monitor)
                .requestId(request.getId())
                .name(dataAddress.getName())
                .params(requestParamsProvider.provideSourceParams(request))
                .requestFactory(requestFactory)
                .build();
    }
}
