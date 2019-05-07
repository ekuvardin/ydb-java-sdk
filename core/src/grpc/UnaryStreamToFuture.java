package ru.yandex.ydb.core.grpc;

import java.util.concurrent.CompletableFuture;

import io.grpc.ClientCall;
import io.grpc.Metadata;
import io.grpc.Status;

import ru.yandex.ydb.core.Issue;
import ru.yandex.ydb.core.Result;
import ru.yandex.ydb.core.StatusCode;

import static ru.yandex.yql.proto.IssueSeverity.TSeverityIds.ESeverityId.S_ERROR;


/**
 * @author Sergey Polovko
 */
public class UnaryStreamToFuture<T> extends ClientCall.Listener<T> {

    private final CompletableFuture<Result<T>> responseFuture;
    private T value;

    public UnaryStreamToFuture(CompletableFuture<Result<T>> responseFuture) {
        this.responseFuture = responseFuture;
    }

    @Override
    public void onMessage(T value) {
        if (this.value != null) {
            Issue issue = Issue.of("More than one value received for gRPC unary call", S_ERROR);
            responseFuture.complete(Result.fail(StatusCode.CLIENT_INTERNAL_ERROR, issue));
        }
        this.value = value;
    }

    @Override
    public void onClose(Status status, Metadata trailers) {
        if (status.isOk()) {
            if (value == null) {
                Issue issue = Issue.of("No value received for gRPC unary call", S_ERROR);
                responseFuture.complete(Result.fail(StatusCode.CLIENT_INTERNAL_ERROR, issue));
            } else {
                responseFuture.complete(Result.success(value));
            }
        } else {
            responseFuture.complete(GrpcStatuses.translate(status));
        }
    }
}
