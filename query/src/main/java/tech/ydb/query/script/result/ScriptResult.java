package tech.ydb.query.script.result;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.operation.Operation;
import tech.ydb.core.operation.OperationBinder;
import tech.ydb.core.operation.OperationTray;
import tech.ydb.proto.OperationProtos;

import java.util.concurrent.CompletableFuture;

public class ScriptResult {

    private volatile CompletableFuture<Result<OperationProtos.Operation>> futureResultOfScriptExecution;
    Operation<Result<OperationProtos.Operation>>  operation;

    public ScriptResult(Operation<Result<OperationProtos.Operation>> operation) {
        this.operation = operation;
    }

    public static ScriptResult ScriptResult1(Operation<Result<OperationProtos.Operation>> op) {
        return new ScriptResult(op);
    }

    public CompletableFuture<Result<OperationProtos.Operation>> getStatus() {
        if (futureResultOfScriptExecution == null) {
            synchronized (this) {
                if (futureResultOfScriptExecution == null) {
                    futureResultOfScriptExecution = OperationTray.fetchOperation(
                            operation, 1);
                }
            }
        }
        return futureResultOfScriptExecution;
    }

    public Status waitForResult() {
        return getStatus().join().getStatus();
    }
}
