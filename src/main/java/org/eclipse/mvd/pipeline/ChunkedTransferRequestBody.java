package org.eclipse.edc.mvd.pipeline;

import okio.BufferedSink;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;

/**
 * Streams content into an OK HTTP buffered sink in chunks.
 * <p>
 * Due to OkHttp implementation an extra header will be created (no-overridable) Transfer-Encoding with value chunked
 *
 * @see <a href="https://github.com/square/okhttp/blob/master/docs/features/calls.md">OkHttp Documentation</a>
 */
public class ChunkedTransferRequestBody extends AbstractTransferRequestBody {

    private final Supplier<InputStream> bodySupplier;

    public ChunkedTransferRequestBody(Supplier<InputStream> bodySupplier, String contentType) {
        super(contentType);
        this.bodySupplier = bodySupplier;
    }

    @Override
    public void writeTo(@NotNull BufferedSink sink) throws IOException {
        try (var os = sink.outputStream(); var is = bodySupplier.get()) {
            is.transferTo(os);
        }
    }
}
