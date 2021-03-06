package com.coreos.jetcd;

import com.coreos.jetcd.api.AlarmMember;
import com.coreos.jetcd.api.AlarmResponse;
import com.coreos.jetcd.api.DefragmentResponse;
import com.coreos.jetcd.api.StatusResponse;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Interface of maintenance talking to etcd.
 *
 * <p>An etcd cluster needs periodic maintenance to remain reliable. Depending
 * on an etcd application's needs, this maintenance can usually be
 * automated and performed without downtime or significantly degraded
 * performance.
 *
 * <p>All etcd maintenance manages storage resources consumed by the etcd
 * keyspace. Failure to adequately control the keyspace size is guarded by
 * storage space quotas; if an etcd member runs low on space, a quota will
 * trigger cluster-wide alarms which will put the system into a
 * limited-operation maintenance mode. To avoid running out of space for
 * writes to the keyspace, the etcd keyspace history must be compacted.
 * Storage space itself may be reclaimed by defragmenting etcd members.
 * Finally, periodic snapshot backups of etcd member state makes it possible
 * to recover any unintended logical data loss or corruption caused by
 * operational error.
 */
public interface Maintenance {

  /**
   * get all active keyspace alarm.
   */
  ListenableFuture<AlarmResponse> listAlarms();

  /**
   * disarms a given alarm.
   *
   * @param member the alarm
   * @return the response result
   */
  ListenableFuture<AlarmResponse> alarmDisarm(AlarmMember member);

  /**
   * defragment one member of the cluster.
   *
   * <p>After compacting the keyspace, the backend database may exhibit internal
   * fragmentation. Any internal fragmentation is space that is free to use
   * by the backend but still consumes storage space. The process of
   * defragmentation releases this storage space back to the file system.
   * Defragmentation is issued on a per-member so that cluster-wide latency
   * spikes may be avoided.
   *
   * <p>Defragment is an expensive operation. User should avoid defragmenting
   * multiple members at the same time.
   * To defragment multiple members in the cluster, user need to call defragment
   * multiple times with different endpoints.
   */
  ListenableFuture<DefragmentResponse> defragmentMember();

  /**
   * get the status of one member.
   */
  ListenableFuture<StatusResponse> statusMember();

  /**
   * retrieves backend snapshot.
   *
   * <p>-- ex: save backend snapshot to ./snapshot.db --
   * <pre>
   * {@code
   * // create snapshot.db file current folder.
   * String dir = Paths.get("").toAbsolutePath().toString();
   * File snapfile = new File(dir, "snapshot.db");
   *
   * // leverage try-with-resources
   * try (Snapshot snapshot = maintenance.snapshot();
   * FileOutputStream fop = newFileOutputStream(snapfile)) {
   * snapshot.write(fop);
   * } catch (Exception e) {
   * snapfile.delete();
   * }
   * }
   * </pre>
   *
   * @return a Snapshot for retrieving backend snapshot.
   */
  Snapshot snapshot();

  interface Snapshot extends Closeable {

    /**
     * Write backend snapshot to user provided OutputStream.
     *
     * <p>write can only be called once; multiple calls on write results
     * IOException thrown after first call.
     *
     * <p>this method blocks until farther snapshot data are available,
     * end of stream is detected, or an exception is thrown.
     *
     * @throws IOException if connection issue, Snapshot closed, and any I/O issues.
     */
    void write(OutputStream os) throws IOException;
  }

}
