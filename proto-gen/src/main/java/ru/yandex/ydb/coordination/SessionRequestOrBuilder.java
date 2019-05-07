// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: kikimr/public/api/protos/ydb_coordination.proto

package ru.yandex.ydb.coordination;

public interface SessionRequestOrBuilder extends
    // @@protoc_insertion_point(interface_extends:Ydb.Coordination.SessionRequest)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>.Ydb.Coordination.SessionRequest.PingPong ping = 1;</code>
   */
  boolean hasPing();
  /**
   * <code>.Ydb.Coordination.SessionRequest.PingPong ping = 1;</code>
   */
  ru.yandex.ydb.coordination.SessionRequest.PingPong getPing();
  /**
   * <code>.Ydb.Coordination.SessionRequest.PingPong ping = 1;</code>
   */
  ru.yandex.ydb.coordination.SessionRequest.PingPongOrBuilder getPingOrBuilder();

  /**
   * <code>.Ydb.Coordination.SessionRequest.PingPong pong = 2;</code>
   */
  boolean hasPong();
  /**
   * <code>.Ydb.Coordination.SessionRequest.PingPong pong = 2;</code>
   */
  ru.yandex.ydb.coordination.SessionRequest.PingPong getPong();
  /**
   * <code>.Ydb.Coordination.SessionRequest.PingPong pong = 2;</code>
   */
  ru.yandex.ydb.coordination.SessionRequest.PingPongOrBuilder getPongOrBuilder();

  /**
   * <code>.Ydb.Coordination.SessionRequest.SessionStart session_start = 3;</code>
   */
  boolean hasSessionStart();
  /**
   * <code>.Ydb.Coordination.SessionRequest.SessionStart session_start = 3;</code>
   */
  ru.yandex.ydb.coordination.SessionRequest.SessionStart getSessionStart();
  /**
   * <code>.Ydb.Coordination.SessionRequest.SessionStart session_start = 3;</code>
   */
  ru.yandex.ydb.coordination.SessionRequest.SessionStartOrBuilder getSessionStartOrBuilder();

  /**
   * <code>.Ydb.Coordination.SessionRequest.SessionStop session_stop = 4;</code>
   */
  boolean hasSessionStop();
  /**
   * <code>.Ydb.Coordination.SessionRequest.SessionStop session_stop = 4;</code>
   */
  ru.yandex.ydb.coordination.SessionRequest.SessionStop getSessionStop();
  /**
   * <code>.Ydb.Coordination.SessionRequest.SessionStop session_stop = 4;</code>
   */
  ru.yandex.ydb.coordination.SessionRequest.SessionStopOrBuilder getSessionStopOrBuilder();

  /**
   * <code>.Ydb.Coordination.SessionRequest.ClientReady client_ready = 5;</code>
   */
  boolean hasClientReady();
  /**
   * <code>.Ydb.Coordination.SessionRequest.ClientReady client_ready = 5;</code>
   */
  ru.yandex.ydb.coordination.SessionRequest.ClientReady getClientReady();
  /**
   * <code>.Ydb.Coordination.SessionRequest.ClientReady client_ready = 5;</code>
   */
  ru.yandex.ydb.coordination.SessionRequest.ClientReadyOrBuilder getClientReadyOrBuilder();

  /**
   * <code>.Ydb.Coordination.SessionRequest.UpdateJobStatus update_job_status = 6;</code>
   */
  boolean hasUpdateJobStatus();
  /**
   * <code>.Ydb.Coordination.SessionRequest.UpdateJobStatus update_job_status = 6;</code>
   */
  ru.yandex.ydb.coordination.SessionRequest.UpdateJobStatus getUpdateJobStatus();
  /**
   * <code>.Ydb.Coordination.SessionRequest.UpdateJobStatus update_job_status = 6;</code>
   */
  ru.yandex.ydb.coordination.SessionRequest.UpdateJobStatusOrBuilder getUpdateJobStatusOrBuilder();

  /**
   * <code>.Ydb.Coordination.SessionRequest.AcquireSemaphore acquire_semaphore = 7;</code>
   */
  boolean hasAcquireSemaphore();
  /**
   * <code>.Ydb.Coordination.SessionRequest.AcquireSemaphore acquire_semaphore = 7;</code>
   */
  ru.yandex.ydb.coordination.SessionRequest.AcquireSemaphore getAcquireSemaphore();
  /**
   * <code>.Ydb.Coordination.SessionRequest.AcquireSemaphore acquire_semaphore = 7;</code>
   */
  ru.yandex.ydb.coordination.SessionRequest.AcquireSemaphoreOrBuilder getAcquireSemaphoreOrBuilder();

  /**
   * <code>.Ydb.Coordination.SessionRequest.ReleaseSemaphore release_semaphore = 8;</code>
   */
  boolean hasReleaseSemaphore();
  /**
   * <code>.Ydb.Coordination.SessionRequest.ReleaseSemaphore release_semaphore = 8;</code>
   */
  ru.yandex.ydb.coordination.SessionRequest.ReleaseSemaphore getReleaseSemaphore();
  /**
   * <code>.Ydb.Coordination.SessionRequest.ReleaseSemaphore release_semaphore = 8;</code>
   */
  ru.yandex.ydb.coordination.SessionRequest.ReleaseSemaphoreOrBuilder getReleaseSemaphoreOrBuilder();

  /**
   * <code>.Ydb.Coordination.SessionRequest.DescribeSemaphore describe_semaphore = 9;</code>
   */
  boolean hasDescribeSemaphore();
  /**
   * <code>.Ydb.Coordination.SessionRequest.DescribeSemaphore describe_semaphore = 9;</code>
   */
  ru.yandex.ydb.coordination.SessionRequest.DescribeSemaphore getDescribeSemaphore();
  /**
   * <code>.Ydb.Coordination.SessionRequest.DescribeSemaphore describe_semaphore = 9;</code>
   */
  ru.yandex.ydb.coordination.SessionRequest.DescribeSemaphoreOrBuilder getDescribeSemaphoreOrBuilder();

  /**
   * <code>.Ydb.Coordination.SessionRequest.CreateSemaphore create_semaphore = 10;</code>
   */
  boolean hasCreateSemaphore();
  /**
   * <code>.Ydb.Coordination.SessionRequest.CreateSemaphore create_semaphore = 10;</code>
   */
  ru.yandex.ydb.coordination.SessionRequest.CreateSemaphore getCreateSemaphore();
  /**
   * <code>.Ydb.Coordination.SessionRequest.CreateSemaphore create_semaphore = 10;</code>
   */
  ru.yandex.ydb.coordination.SessionRequest.CreateSemaphoreOrBuilder getCreateSemaphoreOrBuilder();

  /**
   * <code>.Ydb.Coordination.SessionRequest.UpdateSemaphore update_semaphore = 11;</code>
   */
  boolean hasUpdateSemaphore();
  /**
   * <code>.Ydb.Coordination.SessionRequest.UpdateSemaphore update_semaphore = 11;</code>
   */
  ru.yandex.ydb.coordination.SessionRequest.UpdateSemaphore getUpdateSemaphore();
  /**
   * <code>.Ydb.Coordination.SessionRequest.UpdateSemaphore update_semaphore = 11;</code>
   */
  ru.yandex.ydb.coordination.SessionRequest.UpdateSemaphoreOrBuilder getUpdateSemaphoreOrBuilder();

  /**
   * <code>.Ydb.Coordination.SessionRequest.DeleteSemaphore delete_semaphore = 12;</code>
   */
  boolean hasDeleteSemaphore();
  /**
   * <code>.Ydb.Coordination.SessionRequest.DeleteSemaphore delete_semaphore = 12;</code>
   */
  ru.yandex.ydb.coordination.SessionRequest.DeleteSemaphore getDeleteSemaphore();
  /**
   * <code>.Ydb.Coordination.SessionRequest.DeleteSemaphore delete_semaphore = 12;</code>
   */
  ru.yandex.ydb.coordination.SessionRequest.DeleteSemaphoreOrBuilder getDeleteSemaphoreOrBuilder();

  /**
   * <code>.Ydb.Coordination.SessionRequest.CreateTask create_task = 13;</code>
   */
  boolean hasCreateTask();
  /**
   * <code>.Ydb.Coordination.SessionRequest.CreateTask create_task = 13;</code>
   */
  ru.yandex.ydb.coordination.SessionRequest.CreateTask getCreateTask();
  /**
   * <code>.Ydb.Coordination.SessionRequest.CreateTask create_task = 13;</code>
   */
  ru.yandex.ydb.coordination.SessionRequest.CreateTaskOrBuilder getCreateTaskOrBuilder();

  /**
   * <code>.Ydb.Coordination.SessionRequest.UpdateTask update_task = 14;</code>
   */
  boolean hasUpdateTask();
  /**
   * <code>.Ydb.Coordination.SessionRequest.UpdateTask update_task = 14;</code>
   */
  ru.yandex.ydb.coordination.SessionRequest.UpdateTask getUpdateTask();
  /**
   * <code>.Ydb.Coordination.SessionRequest.UpdateTask update_task = 14;</code>
   */
  ru.yandex.ydb.coordination.SessionRequest.UpdateTaskOrBuilder getUpdateTaskOrBuilder();

  /**
   * <code>.Ydb.Coordination.SessionRequest.DeleteTask delete_task = 15;</code>
   */
  boolean hasDeleteTask();
  /**
   * <code>.Ydb.Coordination.SessionRequest.DeleteTask delete_task = 15;</code>
   */
  ru.yandex.ydb.coordination.SessionRequest.DeleteTask getDeleteTask();
  /**
   * <code>.Ydb.Coordination.SessionRequest.DeleteTask delete_task = 15;</code>
   */
  ru.yandex.ydb.coordination.SessionRequest.DeleteTaskOrBuilder getDeleteTaskOrBuilder();

  public ru.yandex.ydb.coordination.SessionRequest.RequestCase getRequestCase();
}
