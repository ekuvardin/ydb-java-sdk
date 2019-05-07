package ru.yandex.ydb.table;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import ru.yandex.ydb.core.Result;
import ru.yandex.ydb.core.Status;
import ru.yandex.ydb.table.description.TableColumn;
import ru.yandex.ydb.table.description.TableDescription;
import ru.yandex.ydb.table.query.DataQuery;
import ru.yandex.ydb.table.query.DataQueryImpl;
import ru.yandex.ydb.table.query.DataQueryResult;
import ru.yandex.ydb.table.query.ExplainDataQueryResult;
import ru.yandex.ydb.table.query.Params;
import ru.yandex.ydb.table.rpc.TableRpc;
import ru.yandex.ydb.table.settings.AlterTableSettings;
import ru.yandex.ydb.table.settings.AutoPartitioningPolicy;
import ru.yandex.ydb.table.settings.BeginTxSettings;
import ru.yandex.ydb.table.settings.CloseSessionSettings;
import ru.yandex.ydb.table.settings.CopyTableSettings;
import ru.yandex.ydb.table.settings.CreateTableSettings;
import ru.yandex.ydb.table.settings.DescribeTableSettings;
import ru.yandex.ydb.table.settings.DropTableSettings;
import ru.yandex.ydb.table.settings.ExecuteDataQuerySettings;
import ru.yandex.ydb.table.settings.ExecuteSchemeQuerySettings;
import ru.yandex.ydb.table.settings.ExplainDataQuerySettings;
import ru.yandex.ydb.table.settings.PartitioningPolicy;
import ru.yandex.ydb.table.settings.PrepareDataQuerySettings;
import ru.yandex.ydb.table.settings.StoragePolicy;
import ru.yandex.ydb.table.transaction.Transaction;
import ru.yandex.ydb.table.transaction.TransactionImpl;
import ru.yandex.ydb.table.transaction.TransactionMode;
import ru.yandex.ydb.table.transaction.TxControl;
import ru.yandex.ydb.table.types.proto.ProtoType;


/**
 * @author Sergey Polovko
 */
class SessionImpl implements Session {

    enum State {
        STANDALONE,
        IDLE,
        BROKEN,
        IN_USE,
    }

    private static final AtomicReferenceFieldUpdater<SessionImpl, State> stateUpdater =
        AtomicReferenceFieldUpdater.newUpdater(SessionImpl.class, State.class, "state");

    private final String id;
    private final TableRpc tableRpc;
    private final OperationsTray operationsTray;

    private volatile State state = State.STANDALONE;

    SessionImpl(String id, TableRpc tableRpc, OperationsTray operationsTray) {
        this.id = id;
        this.tableRpc = tableRpc;
        this.operationsTray = operationsTray;
    }

    @Override
    public String getId() {
        return id;
    }

    public State getState() {
        return stateUpdater.get(this);
    }

    boolean switchState(State from, State to) {
        return stateUpdater.compareAndSet(this, from, to);
    }

    @Override
    public CompletableFuture<Status> createTable(
        String path,
        TableDescription tableDescriptions,
        CreateTableSettings settings)
    {
        YdbTable.CreateTableRequest.Builder request = YdbTable.CreateTableRequest.newBuilder()
            .setSessionId(id)
            .setPath(path)
            .addAllPrimaryKey(tableDescriptions.getPrimaryKeys());

        for (TableColumn column : tableDescriptions.getColumns()) {
            request.addColumns(YdbTable.ColumnMeta.newBuilder()
                .setName(column.getName())
                .setType(ProtoType.toPb(column.getType()))
                .build());
        }

        if (settings.getPresetName() != null) {
            request.getProfileBuilder()
                .setPresetName(settings.getPresetName());
        }

        if (settings.getExecutionPolicy() != null) {
            request.getProfileBuilder()
                .getExecutionPolicyBuilder()
                .setPresetName(settings.getExecutionPolicy());
        }

        if (settings.getCompactionPolicy() != null) {
            request.getProfileBuilder()
                .getCompactionPolicyBuilder()
                .setPresetName(settings.getExecutionPolicy());
        }

        {
            PartitioningPolicy policy = settings.getPartitioningPolicy();
            if (policy != null) {
                YdbTable.PartitioningPolicy.Builder policyProto = request.getProfileBuilder()
                    .getPartitioningPolicyBuilder();
                if (policy.getPresetName() != null) {
                    policyProto.setPresetName(policy.getPresetName());
                }
                if (policy.getAutoPartitioning() != null) {
                    policyProto.setAutoPartitioning(toPb(policy.getAutoPartitioning()));
                }
                if (policy.getUniformPartitions() > 0) {
                    policyProto.setUniformPartitions(policy.getUniformPartitions());
                }
            }
        }

        {
            StoragePolicy policy = settings.getStoragePolicy();
            if (policy != null) {
                YdbTable.StoragePolicy.Builder policyProto = request.getProfileBuilder()
                    .getStoragePolicyBuilder();
                if (policy.getPresetName() != null) {
                    policyProto.setPresetName(policy.getPresetName());
                }
                if (policy.getSysLog() != null) {
                    policyProto.getSyslogBuilder().setStorageKind(policy.getSysLog());
                }
                if (policy.getLog() != null) {
                    policyProto.getLogBuilder().setStorageKind(policy.getLog());
                }
                if (policy.getData() != null) {
                    policyProto.getDataBuilder().setStorageKind(policy.getData());
                }
                if (policy.getExternal() != null) {
                    policyProto.getExternalBuilder().setStorageKind(policy.getExternal());
                }
            }
        }

        return tableRpc.createTable(request.build())
            .thenCompose(response -> {
                if (!response.isSuccess()) {
                    return CompletableFuture.completedFuture(response.toStatus());
                }
                return operationsTray.waitStatus(response.expect("createTable()").getOperation());
            });
    }

    private static YdbTable.PartitioningPolicy.AutoPartitioningPolicy toPb(AutoPartitioningPolicy policy) {
        switch (policy) {
            case AUTO_SPLIT: return YdbTable.PartitioningPolicy.AutoPartitioningPolicy.AUTO_SPLIT;
            case AUTO_SPLIT_MERGE: return YdbTable.PartitioningPolicy.AutoPartitioningPolicy.AUTO_SPLIT_MERGE;
            case DISABLED: return YdbTable.PartitioningPolicy.AutoPartitioningPolicy.DISABLED;
        }
        throw new IllegalArgumentException("unknown AutoPartitioningPolicy: " + policy);
    }

    @Override
    public CompletableFuture<Status> dropTable(String path, DropTableSettings settings) {
        YdbTable.DropTableRequest request = YdbTable.DropTableRequest.newBuilder()
            .setSessionId(id)
            .setPath(path)
            .build();

        return tableRpc.dropTable(request)
            .thenCompose(response -> {
                if (!response.isSuccess()) {
                    return CompletableFuture.completedFuture(response.toStatus());
                }
                return operationsTray.waitStatus(response.expect("dropTable()").getOperation());
            });
    }

    @Override
    public CompletableFuture<Status> alterTable(String path, AlterTableSettings settings) {
        YdbTable.AlterTableRequest.Builder builder = YdbTable.AlterTableRequest.newBuilder()
            .setSessionId(id)
            .setPath(path);

        settings.forEachAddColumn((name, type) -> {
            builder.addAddColumns(YdbTable.ColumnMeta.newBuilder()
                .setName(name)
                .setType(ProtoType.toPb(type))
                .build());
        });

        settings.forEachDropColumn(builder::addDropColumns);

        return tableRpc.alterTable(builder.build())
            .thenCompose(response -> {
                if (!response.isSuccess()) {
                    return CompletableFuture.completedFuture(response.toStatus());
                }
                return operationsTray.waitStatus(response.expect("alterTable()").getOperation());
            });
    }

    @Override
    public CompletableFuture<Status> copyTable(String src, String dst, CopyTableSettings settings) {
        YdbTable.CopyTableRequest request = YdbTable.CopyTableRequest.newBuilder()
            .setSessionId(id)
            .setSourcePath(src)
            .setDestinationPath(dst)
            .build();

        return tableRpc.copyTable(request)
            .thenCompose(response -> {
                if (!response.isSuccess()) {
                    return CompletableFuture.completedFuture(response.toStatus());
                }
                return operationsTray.waitStatus(response.expect("copyTable()").getOperation());
            });
    }

    @Override
    public CompletableFuture<Result<TableDescription>> describeTable(String path, DescribeTableSettings settings) {
        YdbTable.DescribeTableRequest request = YdbTable.DescribeTableRequest.newBuilder()
            .setSessionId(id)
            .setPath(path)
            .build();

        return tableRpc.describeTable(request)
            .thenCompose(response -> {
                if (!response.isSuccess()) {
                    return CompletableFuture.completedFuture(response.cast());
                }
                return operationsTray.waitResult(
                    response.expect("describeTable()").getOperation(),
                    YdbTable.DescribeTableResult.class,
                    SessionImpl::mapDescribeTable);
            });
    }

    private static TableDescription mapDescribeTable(YdbTable.DescribeTableResult result) {
        TableDescription.Builder description = TableDescription.newBuilder();
        for (int i = 0; i < result.getColumnsCount(); i++) {
            YdbTable.ColumnMeta column = result.getColumns(i);
            description.addNonnullColumn(column.getName(), ProtoType.fromPb(column.getType()));
        }
        description.setPrimaryKeys(result.getPrimaryKeyList());
        return description.build();
    }

    private static YdbTable.TransactionSettings txSettings(TransactionMode transactionMode) {
        YdbTable.TransactionSettings.Builder settings = YdbTable.TransactionSettings.newBuilder();
        if (transactionMode == TransactionMode.SERIALIZABLE_READ_WRITE) {
            settings.setSerializableReadWrite(YdbTable.SerializableModeSettings.getDefaultInstance());
        } else if (transactionMode == TransactionMode.ONLINE_READ_ONLY) {
            settings.setOnlineReadOnly(YdbTable.OnlineModeSettings.getDefaultInstance());
        } else if (transactionMode == TransactionMode.STALE_READ_ONLY) {
            settings.setStaleReadOnly(YdbTable.StaleModeSettings.getDefaultInstance());
        }
        return settings.build();
    }

    @Override
    public CompletableFuture<Result<DataQueryResult>> executeDataQuery(
        String query, TxControl txControl, Params params, ExecuteDataQuerySettings settings)
    {
        YdbTable.ExecuteDataQueryRequest request = YdbTable.ExecuteDataQueryRequest.newBuilder()
            .setSessionId(id)
            .setTxControl(txControl.toPb())
            .setQuery(YdbTable.Query.newBuilder().setYqlText(query))
            .putAllParameters(params.toPb())
            .build();

        return tableRpc.executeDataQuery(request)
            .thenCompose(response -> {
                if (!response.isSuccess()) {
                    return CompletableFuture.completedFuture(response.cast());
                }
                return operationsTray.waitResult(
                    response.expect("executeDataQuery()").getOperation(),
                    YdbTable.ExecuteQueryResult.class,
                    SessionImpl::mapExecuteDataQuery);
            });
    }

    private static DataQueryResult mapExecuteDataQuery(YdbTable.ExecuteQueryResult result) {
        YdbTable.TransactionMeta txMeta = result.getTxMeta();
        return new DataQueryResult(txMeta.getId(), result.getResultSetsList());
    }

    @Override
    public CompletableFuture<Result<DataQuery>> prepareDataQuery(String query, PrepareDataQuerySettings settings) {
        YdbTable.PrepareDataQueryRequest request = YdbTable.PrepareDataQueryRequest.newBuilder()
            .setSessionId(id)
            .setYqlText(query)
            .build();

        return tableRpc.prepareDataQuery(request)
            .thenCompose(response -> {
                if (!response.isSuccess()) {
                    return CompletableFuture.completedFuture(response.cast());
                }
                return operationsTray.waitResult(
                    response.expect("prepareDataQuery()").getOperation(),
                    YdbTable.PrepareQueryResult.class,
                    result -> new DataQueryImpl(
                        id, tableRpc, operationsTray,
                        result.getQueryId(), result.getParametersTypesMap()));
            });
    }

    @Override
    public CompletableFuture<Status> executeSchemeQuery(String query, ExecuteSchemeQuerySettings settings) {
        YdbTable.ExecuteSchemeQueryRequest request = YdbTable.ExecuteSchemeQueryRequest.newBuilder()
            .setSessionId(id)
            .setYqlText(query)
            .build();

        return tableRpc.executeSchemeQuery(request)
            .thenCompose(response -> {
                if (!response.isSuccess()) {
                    return CompletableFuture.completedFuture(response.toStatus());
                }
                return operationsTray.waitStatus(response.expect("executeSchemaQuery()").getOperation());
            });
    }

    @Override
    public CompletableFuture<Result<ExplainDataQueryResult>> explainDataQuery(String query, ExplainDataQuerySettings settings) {
        YdbTable.ExplainDataQueryRequest request = YdbTable.ExplainDataQueryRequest.newBuilder()
            .setSessionId(id)
            .setYqlText(query)
            .build();

        return tableRpc.explainDataQuery(request)
            .thenCompose(response -> {
                if (!response.isSuccess()) {
                    return CompletableFuture.completedFuture(response.cast());
                }
                return operationsTray.waitResult(
                    response.expect("explainDataQuery()").getOperation(),
                    YdbTable.ExplainQueryResult.class,
                    result -> new ExplainDataQueryResult(result.getQueryAst(), result.getQueryPlan()));
            });
    }

    @Override
    public CompletableFuture<Result<Transaction>> beginTransaction(TransactionMode transactionMode, BeginTxSettings settings) {
        YdbTable.BeginTransactionRequest request = YdbTable.BeginTransactionRequest.newBuilder()
            .setSessionId(id)
            .setTxSettings(txSettings(transactionMode))
            .build();

        return tableRpc.beginTransaction(request)
            .thenCompose(response -> {
                if (!response.isSuccess()) {
                    return CompletableFuture.completedFuture(response.cast());
                }
                return operationsTray.waitResult(
                    response.expect("beginTransaction()").getOperation(),
                    YdbTable.BeginTransactionResult.class,
                    result -> new TransactionImpl(id, result.getTxMeta().getId(), tableRpc, operationsTray));
            });
    }

    @Override
    public CompletableFuture<Status> close(CloseSessionSettings settings) {
        YdbTable.DeleteSessionRequest request = YdbTable.DeleteSessionRequest.newBuilder()
            .setSessionId(id)
            .build();

        return tableRpc.deleteSession(request)
            .thenCompose(response -> {
                if (!response.isSuccess()) {
                    return CompletableFuture.completedFuture(response.toStatus());
                }
                return operationsTray.waitStatus(response.expect("deleteSession()").getOperation());
            });
    }
}
