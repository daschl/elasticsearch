/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.elasticsearch.repositories;

import org.apache.lucene.index.IndexCommit;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.snapshots.SnapshotId;
import org.elasticsearch.common.component.LifecycleComponent;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.index.snapshots.IndexShardSnapshotStatus;
import org.elasticsearch.snapshots.SnapshotInfo;
import org.elasticsearch.snapshots.SnapshotShardFailure;

import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;

/**
 * Snapshot repository interface.
 * <p>
 * Responsible for index and cluster level operations. It's called only on master.
 * Shard-level operations are performed using {@link org.elasticsearch.index.snapshots.IndexShardRepository}
 * interface on data nodes.
 * <p>
 * Typical snapshot usage pattern:
 * <ul>
 * <li>Master calls {@link #initializeSnapshot(SnapshotId, List, org.elasticsearch.cluster.metadata.MetaData)}
 * with list of indices that will be included into the snapshot</li>
 * <li>Data nodes call {@link org.elasticsearch.index.snapshots.IndexShardRepository#snapshot(SnapshotId, ShardId, IndexCommit, IndexShardSnapshotStatus)} for each shard</li>
 * <li>When all shard calls return master calls {@link #finalizeSnapshot}
 * with possible list of failures</li>
 * </ul>
 */
public interface Repository extends LifecycleComponent {

    /**
     * Reads snapshot description from repository.
     *
     * @param snapshotId  snapshot id
     * @return information about snapshot
     */
    SnapshotInfo readSnapshot(SnapshotId snapshotId);

    /**
     * Returns global metadata associate with the snapshot.
     * <p>
     * The returned meta data contains global metadata as well as metadata for all indices listed in the indices parameter.
     *
     * @param snapshot snapshot
     * @param indices    list of indices
     * @return information about snapshot
     */
    MetaData readSnapshotMetaData(SnapshotInfo snapshot, List<String> indices) throws IOException;

    /**
     * Returns the list of snapshots currently stored in the repository that match the given predicate on the snapshot name.
     * To get all snapshots, the predicate filter should return true regardless of the input.
     *
     * @return snapshot list
     */
    List<SnapshotId> snapshots();

    /**
     * Starts snapshotting process
     *
     * @param snapshotId snapshot id
     * @param indices    list of indices to be snapshotted
     * @param metaData   cluster metadata
     */
    void initializeSnapshot(SnapshotId snapshotId, List<String> indices, MetaData metaData);

    /**
     * Finalizes snapshotting process
     * <p>
     * This method is called on master after all shards are snapshotted.
     *
     * @param snapshotId    snapshot id
     * @param failure       global failure reason or null
     * @param totalShards   total number of shards
     * @param shardFailures list of shard failures
     * @return snapshot description
     */
    SnapshotInfo finalizeSnapshot(SnapshotId snapshotId, List<String> indices, long startTime, String failure, int totalShards, List<SnapshotShardFailure> shardFailures);

    /**
     * Deletes snapshot
     *
     * @param snapshotId snapshot id
     */
    void deleteSnapshot(SnapshotId snapshotId);

    /**
     * Returns snapshot throttle time in nanoseconds
     */
    long snapshotThrottleTimeInNanos();

    /**
     * Returns restore throttle time in nanoseconds
     */
    long restoreThrottleTimeInNanos();


    /**
     * Verifies repository on the master node and returns the verification token.
     * <p>
     * If the verification token is not null, it's passed to all data nodes for verification. If it's null - no
     * additional verification is required
     *
     * @return verification token that should be passed to all Index Shard Repositories for additional verification or null
     */
    String startVerification();

    /**
     * Called at the end of repository verification process.
     * <p>
     * This method should perform all necessary cleanup of the temporary files created in the repository
     *
     * @param verificationToken verification request generated by {@link #startVerification} command
     */
    void endVerification(String verificationToken);

    /**
     * Returns true if the repository supports only read operations
     * @return true if the repository is read/only
     */
    boolean readOnly();

}
