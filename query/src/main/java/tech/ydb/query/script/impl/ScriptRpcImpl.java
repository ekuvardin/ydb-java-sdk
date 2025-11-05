package tech.ydb.query.script.impl;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.operation.Operation;
import tech.ydb.core.operation.OperationBinder;
import tech.ydb.core.operation.StatusExtractor;
import tech.ydb.proto.OperationProtos;
import tech.ydb.proto.operation.v1.OperationServiceGrpc;
import tech.ydb.proto.query.YdbQuery;
import tech.ydb.proto.query.v1.QueryServiceGrpc;
import tech.ydb.query.script.ScriptRpc;

import javax.annotation.WillNotClose;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class ScriptRpcImpl implements ScriptRpc {

    private static final StatusExtractor<YdbQuery.FetchScriptResultsResponse> FETCH_SCRIPT = StatusExtractor.of(
            YdbQuery.FetchScriptResultsResponse::getStatus,
            YdbQuery.FetchScriptResultsResponse::getIssuesList
    );

    private final GrpcTransport transport;

    private ScriptRpcImpl(GrpcTransport grpcTransport) {
        this.transport = grpcTransport;
    }

    public static ScriptRpcImpl useTransport(@WillNotClose GrpcTransport grpcTransport) {
        return new ScriptRpcImpl(grpcTransport);
    }

    @Override
    public CompletableFuture<Operation<Result<OperationProtos.Operation>>> getOperation(
            String operationId, GrpcRequestSettings settings
    ) {
        OperationProtos.GetOperationRequest request = OperationProtos.GetOperationRequest.newBuilder()
                .setId(operationId)
                .build();

        return transport
                .unaryCall(OperationServiceGrpc.getGetOperationMethod(), settings, request)
                .thenApply(OperationBinder.bindAsync(
                        transport, OperationProtos.GetOperationResponse::getOperation, OperationProtos.Operation.class
                ));
    }

    @Override
    public CompletableFuture<Operation<Result<OperationProtos.Operation>>> executeScript(
            YdbQuery.ExecuteScriptRequest request, GrpcRequestSettings settings) {
       return transport.unaryCall(QueryServiceGrpc.getExecuteScriptMethod(), settings, request)
                .thenApply(OperationBinder.bindAsync(
                        transport, ee -> ee, OperationProtos.Operation.class
                ));
    }

    /**
     * Fetches the results of a previously executed script.
     *
     * <p>This method retrieves the next portion of script execution results,
     * supporting pagination and partial fetch using tokens.</p>
     *
     * @param request  the {@link YdbQuery.FetchScriptResultsRequest} specifying the fetch parameters
     * @param settings gRPC request settings
     * @return a future resolving to {@link Result} containing {@link YdbQuery.FetchScriptResultsResponse}
     */
    @Override
    public CompletableFuture<Result<YdbQuery.FetchScriptResultsResponse>> fetchScriptResults(
            YdbQuery.FetchScriptResultsRequest request, GrpcRequestSettings settings) {

        return transport
                .unaryCall(QueryServiceGrpc.getFetchScriptResultsMethod(), settings, request);
    }

    @Override
    public <R> Function<Result<R>, Operation<Status>> bindAsync(
            Function<R, OperationProtos.Operation> method) {
        return OperationBinder.bindAsync(this.transport, method);
    }
}
