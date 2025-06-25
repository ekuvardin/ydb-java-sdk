package tech.ydb.scheme.description;

import tech.ydb.proto.OperationProtos;
import tech.ydb.proto.scheme.SchemeOperationProtos;

public class ModifyPermissionsResponse {

    private final OperationProtos.Operation operation;

    public ModifyPermissionsResponse(SchemeOperationProtos.ModifyPermissionsResponse response) {
        this.operation = response.getOperation();
    }

    public OperationProtos.Operation getOperation() {
        return operation;
    }
}
