package tech.ydb.scheme.description;

import java.util.List;

import tech.ydb.proto.scheme.SchemeOperationProtos;

public class PermissionDescription {

    boolean isClear;
    List<SchemeOperationProtos.PermissionsAction> actionList;

    public boolean isClear() {
        return isClear;
    }

    public void setClear(boolean clear) {
        isClear = clear;
    }

    public List<SchemeOperationProtos.PermissionsAction> getActionList() {
        return actionList;
    }

    public void setActionList(List<SchemeOperationProtos.PermissionsAction> actionList) {
        this.actionList = actionList;
    }
}
