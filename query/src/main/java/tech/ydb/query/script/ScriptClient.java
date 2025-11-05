package tech.ydb.query.script;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.operation.Operation;
import tech.ydb.proto.query.YdbQuery;
import tech.ydb.query.script.result.FetchScriptResult;
import tech.ydb.query.script.result.ScriptResult;
import tech.ydb.query.script.settings.ExecuteScriptSettings;
import tech.ydb.query.script.settings.FetchScriptSettings;
import tech.ydb.query.script.settings.FindScriptSettings;
import tech.ydb.table.query.Params;


import java.util.concurrent.CompletableFuture;

public interface ScriptClient {


    CompletableFuture<FetchScriptResult> fetchScriptResults(
            FetchScriptSettings settings);

    CompletableFuture<Operation<Result<ScriptResult>>> findScript(String operationId, FindScriptSettings settings);


    Status startJoinScript(String query,
                           Params params,
                           ExecuteScriptSettings settings);

    CompletableFuture<Operation<Result<ScriptResult>>> startScript(String query,
                                                                    Params params,
                                                                    ExecuteScriptSettings settings);
}
