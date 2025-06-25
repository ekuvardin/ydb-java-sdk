package tech.ydb.scheme;

import org.junit.AfterClass;
import org.junit.ClassRule;

import org.junit.Test;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.proto.scheme.SchemeOperationProtos;
import tech.ydb.scheme.description.ModifyPermissionsResponse;
import tech.ydb.scheme.description.PermissionDescription;
import tech.ydb.test.junit4.GrpcTransportRule;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class PermissionTest {

    @ClassRule
    public final static GrpcTransportRule transport = new GrpcTransportRule();

    private final static SchemeClient client = SchemeClient.newClient(transport).build();

    @AfterClass
    public static void close() {
        client.close();
    }

    @Test
    public void rootPathTest() throws ExecutionException, InterruptedException {
        String basePath = transport.getDatabase();
        String dirName = "test_dir";
        String path = basePath + "/" + dirName;

        Status status = client.makeDirectories(path).get();

        PermissionDescription description = new PermissionDescription();
        List<SchemeOperationProtos.PermissionsAction> actionList = new ArrayList<>();
        SchemeOperationProtos.PermissionsAction permissionAction = SchemeOperationProtos.PermissionsAction
                .newBuilder()
                .setGrant(SchemeOperationProtos.Permissions
                        .newBuilder()
                        .setSubject("John@staff")
                     //   .addPermissionNames("read")
                        .addPermissionNames("ydb.generic.use")
                        .build())
                .build();
        actionList.add(permissionAction);
        description.setActionList(actionList);
        Result<ModifyPermissionsResponse> responseResult = client.modifyPermissions(path, description).get();
        responseResult.getStatus();
        responseResult.getValue();
    }
}
