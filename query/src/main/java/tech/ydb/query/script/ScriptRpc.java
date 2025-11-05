package tech.ydb.query.script;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.operation.Operation;
import tech.ydb.proto.OperationProtos;
import tech.ydb.proto.query.YdbQuery;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public interface ScriptRpc {

    CompletableFuture<Operation<Result<OperationProtos.Operation>>> getOperation(
            String operationId, GrpcRequestSettings settings
    );

    CompletableFuture<Operation<Result<OperationProtos.Operation>>> executeScript(
            YdbQuery.ExecuteScriptRequest request, GrpcRequestSettings settings);

    CompletableFuture<Result<YdbQuery.FetchScriptResultsResponse>> fetchScriptResults(
            YdbQuery.FetchScriptResultsRequest request, GrpcRequestSettings settings);

    <R> Function<Result<R>, Operation<Status>> bindAsync(
            Function<R, OperationProtos.Operation> method);
}
