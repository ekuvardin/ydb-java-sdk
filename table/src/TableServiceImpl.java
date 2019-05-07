package ru.yandex.ydb.table;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.DefaultThreadFactory;

import ru.yandex.ydb.core.rpc.RpcTransport;
import ru.yandex.ydb.table.rpc.OperationRpc;
import ru.yandex.ydb.table.rpc.SchemeRpc;
import ru.yandex.ydb.table.rpc.TableRpc;


/**
 * @author Sergey Polovko
 */
final class TableServiceImpl implements TableService {

    private final TableRpc tableRpc;
    private final SchemeRpc schemeRpc;
    private final OperationsTray operationsTray;
    @Nullable
    private final RpcTransport transport;
    private final SessionPool sessionPool;

    TableServiceImpl(
        TableRpc tableRpc,
        SchemeRpc schemeRpc,
        OperationRpc operationRpc,
        @Nullable RpcTransport transport,
        int maxSessionsPooled)
    {
        this.tableRpc = tableRpc;
        this.schemeRpc = schemeRpc;
        this.transport = transport;

        HashedWheelTimer timer = new HashedWheelTimer(
            new DefaultThreadFactory("operations-timer"),
            OperationsTray.INITIAL_DELAY_MILLIS,
            TimeUnit.MILLISECONDS);
        this.operationsTray = new OperationsTray(operationRpc, timer);
        this.sessionPool = new SessionPool(maxSessionsPooled);
    }

    @Override
    public TableClient newTableClient() {
        return new TableClientImpl(tableRpc, operationsTray, sessionPool);
    }

    @Override
    public SchemeClient newSchemeClient() {
        return new SchemeClientImpl(schemeRpc, operationsTray);
    }

    @Override
    public void close() {
        operationsTray.close();
        tableRpc.close();
        schemeRpc.close();
        if (transport != null) {
            transport.close();
        }
    }
}
