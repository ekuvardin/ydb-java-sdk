// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: kikimr/public/api/grpc/ydb_operation_v1.proto

package ru.yandex.ydb.operation.v1;

public final class YdbOperationV1 {
  private YdbOperationV1() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n-kikimr/public/api/grpc/ydb_operation_v" +
      "1.proto\022\020Ydb.Operation.V1\032,kikimr/public" +
      "/api/protos/ydb_operation.proto2m\n\020Opera" +
      "tionService\022Y\n\014GetOperation\022#.Ydb.Operat" +
      "ions.GetOperationRequest\032$.Ydb.Operation" +
      "s.GetOperationResponseB\034\n\032ru.yandex.ydb." +
      "operation.v1b\006proto3"
    };
    com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
        new com.google.protobuf.Descriptors.FileDescriptor.    InternalDescriptorAssigner() {
          public com.google.protobuf.ExtensionRegistry assignDescriptors(
              com.google.protobuf.Descriptors.FileDescriptor root) {
            descriptor = root;
            return null;
          }
        };
    com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
          ru.yandex.ydb.OperationProtos.getDescriptor(),
        }, assigner);
    ru.yandex.ydb.OperationProtos.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}
