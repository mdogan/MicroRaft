/*
 * Copyright (c) 2020, MicroRaft.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.microraft.examples;

import io.microraft.Ordered;
import io.microraft.QueryPolicy;
import io.microraft.RaftConfig;
import io.microraft.RaftNode;
import io.microraft.impl.local.LocalRaftGroup;
import io.microraft.impl.local.SimpleStateMachine;
import io.microraft.impl.util.BaseTest;
import io.microraft.model.message.AppendEntriesSuccessResponse;
import io.microraft.report.RaftNodeReport;
import org.junit.After;
import org.junit.Test;

import java.util.List;

import static io.microraft.impl.util.AssertionUtils.eventually;
import static org.assertj.core.api.Assertions.assertThat;

/*

   TO RUN THIS CODE SAMPLE ON YOUR MACHINE:

 $ git clone git@github.com:metanet/MicroRaft.git
 $ cd MicroRaft && ./mvnw clean test -Dtest=io.microraft.examples.RaftLeaderFailureTest -DfailIfNoTests=false -Pcode-sample

 */
public class RaftLeaderFailureTest
        extends BaseTest {

    private LocalRaftGroup group;

    @After
    public void tearDown() {
        if (group != null) {
            group.destroy();
        }
    }

    @Test
    public void testRaftLeaderFailure() {
        RaftConfig config = RaftConfig.newBuilder().setLeaderHeartbeatTimeoutSecs(1).setLeaderHeartbeatTimeoutSecs(5).build();
        group = LocalRaftGroup.newBuilder(3).setConfig(config).enableNewTermOperation().start();
        RaftNode leader = group.waitUntilLeaderElected();

        // the leader can replicate log entries to the followers, but it won't
        // get any response back since we are blocking responses here, so even
        // though it replicates our operation, it won't be able to commit it
        // and send us the response
        for (RaftNode follower : group.getNodesExcept(leader.getLocalEndpoint())) {
            group.dropMessagesTo(follower.getLocalEndpoint(), leader.getLocalEndpoint(), AppendEntriesSuccessResponse.class);
        }

        String value = "value";
        leader.replicate(SimpleStateMachine.apply(value));

        // wait until the followers get the log entry by checking their log
        // indices repeatedly
        eventually(() -> {
            RaftNodeReport leaderReport = leader.getReport().join().getResult();
            long leaderLastLogIndex = leaderReport.getLog().getLastLogOrSnapshotIndex();
            for (RaftNode follower : group.getNodesExcept(leader.getLocalEndpoint())) {
                RaftNodeReport followerReport = follower.getReport().join().getResult();
                long followerLastLogIndex = followerReport.getLog().getLastLogOrSnapshotIndex();
                assertThat(followerLastLogIndex).isEqualTo(leaderLastLogIndex);
            }
        });

        // now the followers have our operation. let's kill the leader
        // now we don't know what happened to our first operation
        group.terminateNode(leader.getLocalEndpoint());

        // we will get a new leader in a second
        RaftNode newLeader = group.waitUntilLeaderElected();

        // we replicate our operation again
        newLeader.replicate(SimpleStateMachine.apply(value));

        Ordered<List<String>> queryResult = newLeader.<List<String>>query(SimpleStateMachine.queryAll(), QueryPolicy.LEADER_LOCAL,
                                                                          0).join();

        // it turns out that our operation is committed twice
        assertThat(queryResult.getResult()).hasSize(2);
    }

}