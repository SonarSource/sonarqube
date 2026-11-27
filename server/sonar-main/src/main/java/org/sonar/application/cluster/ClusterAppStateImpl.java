/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.application.cluster;

import com.hazelcast.cluster.Member;
import com.hazelcast.cluster.MembershipAdapter;
import com.hazelcast.cluster.MembershipEvent;
import com.hazelcast.core.EntryAdapter;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.HazelcastInstanceNotActiveException;
import com.hazelcast.replicatedmap.ReplicatedMap;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.HealthStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.application.AppStateListener;
import org.sonar.application.cluster.health.HealthStateSharing;
import org.sonar.application.cluster.health.HealthStateSharingImpl;
import org.sonar.application.cluster.health.SearchNodeHealthProvider;
import org.sonar.application.config.AppSettings;
import org.sonar.application.config.ClusterSettings;
import org.sonar.application.es.EsConnector;
import org.sonar.process.MessageException;
import org.sonar.process.NetworkUtilsImpl;
import org.sonar.process.ProcessId;
import org.sonar.process.VirtualThreadTask;
import org.sonar.process.cluster.hz.DistributedReference;
import org.sonar.process.cluster.hz.HazelcastMember;

import static java.lang.String.format;
import static org.sonar.process.cluster.hz.HazelcastMember.Attribute.NODE_NAME;
import static org.sonar.process.cluster.hz.HazelcastMember.Attribute.PROCESS_KEY;
import static org.sonar.process.cluster.hz.HazelcastObjects.CLUSTER_NAME;
import static org.sonar.process.cluster.hz.HazelcastObjects.LEADER;
import static org.sonar.process.cluster.hz.HazelcastObjects.LEADER_NODE_NAME;
import static org.sonar.process.cluster.hz.HazelcastObjects.OPERATIONAL_PROCESSES;
import static org.sonar.process.cluster.hz.HazelcastObjects.SONARQUBE_VERSION;

public class ClusterAppStateImpl implements ClusterAppState {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterAppStateImpl.class);

  private final HazelcastMember hzMember;
  private final List<AppStateListener> listeners = new ArrayList<>();
  private final Map<ProcessId, Boolean> operationalLocalProcesses = new EnumMap<>(ProcessId.class);
  private final AtomicBoolean esPoolingThreadRunning = new AtomicBoolean(false);
  private final ReplicatedMap<ClusterProcess, Boolean> operationalProcesses;
  private final UUID operationalProcessListenerUUID;
  private final UUID nodeDisconnectedListenerUUID;
  private final EsConnector esConnector;

  private HealthStateSharing healthStateSharing = null;

  public ClusterAppStateImpl(AppSettings settings, HazelcastMember hzMember, EsConnector esConnector, AppNodesClusterHostsConsistency appNodesClusterHostsConsistency) {
    this.hzMember = hzMember;

    // Get or create the replicated map
    operationalProcesses = (ReplicatedMap) hzMember.getReplicatedMap(OPERATIONAL_PROCESSES);
    operationalProcessListenerUUID = operationalProcesses.addEntryListener(new OperationalProcessListener());
    nodeDisconnectedListenerUUID = hzMember.getCluster().addMembershipListener(new NodeDisconnectedListener());
    appNodesClusterHostsConsistency.check();
    if (ClusterSettings.isLocalElasticsearchEnabled(settings)) {
      this.healthStateSharing = new HealthStateSharingImpl(hzMember, new SearchNodeHealthProvider(settings.getProps(), this, NetworkUtilsImpl.INSTANCE));
      this.healthStateSharing.start();
    }

    this.esConnector = esConnector;
  }

  @Override
  public HazelcastMember getHazelcastMember() {
    return hzMember;
  }

  @Override
  public void addListener(AppStateListener listener) {
    listeners.add(listener);
  }

  @Override
  public boolean isOperational(ProcessId processId, boolean local) {
    if (local) {
      return operationalLocalProcesses.computeIfAbsent(processId, p -> false);
    }

    if (processId.equals(ProcessId.ELASTICSEARCH)) {
      boolean operational = isElasticSearchOperational();
      if (!operational) {
        asyncWaitForEsToBecomeOperational();
      }
      return operational;
    }

    for (Map.Entry<ClusterProcess, Boolean> entry : operationalProcesses.entrySet()) {
      if (entry.getKey().getProcessId().equals(processId) && Boolean.TRUE.equals(entry.getValue())) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void setOperational(ProcessId processId) {
    operationalLocalProcesses.put(processId, true);
    operationalProcesses.put(new ClusterProcess(hzMember.getUuid(), processId), Boolean.TRUE);
  }

  @Override
  public boolean tryToLockWebLeader() {
    DistributedReference<UUID> leader = hzMember.getAtomicReference(LEADER);
    boolean locked = leader.compareAndSet(null, hzMember.getUuid());
    if (locked) {
      hzMember.getAtomicReference(LEADER_NODE_NAME).set(localNodeName());
    }
    return locked;
  }

  @Override
  public void tryToReleaseWebLeaderLock() {
    tryToReleaseWebLeaderLock(hzMember.getUuid());
  }

  /**
   * Tries to release the lock of the cluster leader. It is safe to call this method even if one is not sure about the UUID of the leader.
   * If all nodes call this method then we can be confident that the lock is released.
   *
   * @param uuidOfLeader the UUID of the leader to release the lock. When the UUID is not the leader's uuid this method has no effect.
   * @return true when {@code uuidOfLeader} held the lock and this call released it
   */
  private boolean tryToReleaseWebLeaderLock(UUID uuidOfLeader) {
    DistributedReference<UUID> leader = hzMember.getAtomicReference(LEADER);
    DistributedReference<String> leaderNodeName = hzMember.getAtomicReference(LEADER_NODE_NAME);
    String expectedNodeName = leaderNodeName.get();
    boolean released = leader.compareAndSet(uuidOfLeader, null);
    if (released && expectedNodeName != null) {
      // Only clear the name still owned by this lock. A newer leader may have published its own name.
      leaderNodeName.compareAndSet(expectedNodeName, null);
    }
    return released;
  }

  @Override
  public void reset() {
    throw new IllegalStateException("state reset is not supported in cluster mode");
  }

  @Override
  public void registerSonarQubeVersion(String sonarqubeVersion) {
    DistributedReference<String> sqVersion = hzMember.getAtomicReference(SONARQUBE_VERSION);
    boolean wasSet = sqVersion.compareAndSet(null, sonarqubeVersion);

    if (!wasSet) {
      String clusterVersion = sqVersion.get();
      if (!sqVersion.get().equals(sonarqubeVersion)) {
        throw new IllegalStateException(
          format("The local version %s is not the same as the cluster %s", sonarqubeVersion, clusterVersion));
      }
    }
  }

  @Override
  public void registerClusterName(String clusterName) {
    DistributedReference<String> property = hzMember.getAtomicReference(CLUSTER_NAME);
    boolean wasSet = property.compareAndSet(null, clusterName);

    if (!wasSet) {
      String clusterValue = property.get();
      if (!property.get().equals(clusterName)) {
        throw new MessageException(
          format("This node has a cluster name [%s], which does not match [%s] from the cluster", clusterName, clusterValue));
      }
    }
  }

  @Override
  public Optional<String> getLeaderHostName() {
    UUID leaderUuid = (UUID) hzMember.getAtomicReference(LEADER).get();
    if (leaderUuid == null) {
      return Optional.empty();
    }
    Optional<Member> leader = memberByUuid(leaderUuid);
    if (leader.isPresent()) {
      return Optional.of(leader.get().getAddress().getHost());
    }
    // The app process that owns the lock has left, but its web process may still be running startup.
    String leaderNodeName = leaderNodeName();
    if (leaderNodeName == null) {
      return Optional.empty();
    }
    return webMember(leaderNodeName, leaderUuid)
      .map(member -> member.getAddress().getHost());
  }

  @Override
  public Optional<String> getLeaderNodeNameWithoutMainProcess() {
    UUID leaderUuid = (UUID) hzMember.getAtomicReference(LEADER).get();
    if (leaderUuid == null || memberByUuid(leaderUuid).isPresent()) {
      return Optional.empty();
    }
    String nodeName = leaderNodeName();
    if (nodeName == null || webMember(nodeName, leaderUuid).isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(nodeName);
  }

  @Override
  public void close() {
    esConnector.stop();

    if (hzMember != null) {
      if (healthStateSharing != null) {
        healthStateSharing.stop();
      }
      try {
        // Removing listeners
        operationalProcesses.removeEntryListener(operationalProcessListenerUUID);
        hzMember.getCluster().removeMembershipListener(nodeDisconnectedListenerUUID);

        // Removing the operationalProcess from the replicated map
        operationalProcesses.keySet().forEach(
          clusterNodeProcess -> {
            if (clusterNodeProcess.getNodeUuid().equals(hzMember.getUuid())) {
              operationalProcesses.remove(clusterNodeProcess);
            }
          });

        // Shutdown Hazelcast properly
        hzMember.close();
      } catch (HazelcastInstanceNotActiveException e) {
        // hazelcastCluster may be already closed by the shutdown hook
        LOGGER.debug("Unable to close Hazelcast cluster", e);
      }
    }
  }

  private boolean isElasticSearchOperational() {
    try {
      return esConnector.getClusterHealthStatus()
        .filter(t -> t == HealthStatus.Green || t == HealthStatus.Yellow)
        .isPresent();
    } catch (ElasticsearchException e) {
      LOGGER.warn("Cannot check at current time whether Elasticsearch is operational", e);
      return false;
    }
  }

  private void asyncWaitForEsToBecomeOperational() {
    if (esPoolingThreadRunning.compareAndSet(false, true)) {
      EsPoolingTask task = new EsPoolingTask();
      task.start();
    }
  }

  private class EsPoolingTask extends VirtualThreadTask {
    void start() {
      startVirtualThread("es-state-pooling");
    }

    @Override
    public void run() {
      while (true) {
        if (isElasticSearchOperational()) {
          esPoolingThreadRunning.set(false);
          listeners.forEach(l -> l.accept(ProcessId.ELASTICSEARCH));
          return;
        }

        try {
          Thread.sleep(5_000);
        } catch (InterruptedException e) {
          esPoolingThreadRunning.set(false);
          Thread.currentThread().interrupt();
          return;
        }
      }
    }
  }

  private class OperationalProcessListener extends EntryAdapter<ClusterProcess, Boolean> {
    @Override
    public void entryAdded(EntryEvent<ClusterProcess, Boolean> event) {
      if (Boolean.TRUE.equals(event.getValue())) {
        listeners.forEach(appStateListener -> appStateListener.accept(event.getKey().getProcessId()));
      }
    }

    @Override
    public void entryUpdated(EntryEvent<ClusterProcess, Boolean> event) {
      if (Boolean.TRUE.equals(event.getValue())) {
        listeners.forEach(appStateListener -> appStateListener.accept(event.getKey().getProcessId()));
      }
    }
  }

  private String localNodeName() {
    return hzMember.getCluster().getLocalMember().getAttribute(NODE_NAME.getKey());
  }

  private String leaderNodeName() {
    return (String) hzMember.getAtomicReference(LEADER_NODE_NAME).get();
  }

  private Optional<Member> memberByUuid(UUID uuid) {
    return hzMember.getCluster().getMembers().stream()
      .filter(member -> uuid.equals(member.getUuid()))
      .findFirst();
  }

  /**
   * Web process of {@code nodeName}, other than {@code excludedUuid}. The excluded id is the member that just left,
   * which Hazelcast can still report until the membership event is fully applied.
   */
  private Optional<Member> webMember(String nodeName, UUID excludedUuid) {
    return hzMember.getCluster().getMembers().stream()
      .filter(member -> !excludedUuid.equals(member.getUuid()))
      .filter(member -> nodeName.equals(member.getAttribute(NODE_NAME.getKey())))
      .filter(member -> ProcessId.WEB_SERVER.getKey().equals(member.getAttribute(PROCESS_KEY.getKey())))
      .findFirst();
  }

  private class NodeDisconnectedListener extends MembershipAdapter {
    @Override
    public void memberRemoved(MembershipEvent membershipEvent) {
      Member member = membershipEvent.getMember();
      releaseLeaderLock(member);
      clearOperationalProcess(member.getUuid());
      // Wake nodes still waiting on startup. Operational events never arrive for a leader that died first.
      listeners.forEach(AppStateListener::onClusterMemberLeft);
    }

    private void releaseLeaderLock(Member removedMember) {
      UUID leaderUuid = (UUID) hzMember.getAtomicReference(LEADER).get();
      if (leaderUuid == null) {
        return;
      }
      String nodeName = leaderNodeName();
      if (leaderUuid.equals(removedMember.getUuid())) {
        if (nodeName != null && webMember(nodeName, removedMember.getUuid()).isPresent()) {
          LOGGER.info("Keeping web leader lock of node {} because its web process is still in the cluster", nodeName);
          return;
        }
        if (tryToReleaseWebLeaderLock(leaderUuid)) {
          LOGGER.info("Released web leader lock held by {} because it left the cluster", removedMember.getAddress());
        }
        return;
      }
      boolean removedWebOfLeader = nodeName != null
        && nodeName.equals(removedMember.getAttribute(NODE_NAME.getKey()))
        && ProcessId.WEB_SERVER.getKey().equals(removedMember.getAttribute(PROCESS_KEY.getKey()))
        && memberByUuid(leaderUuid).isEmpty();
      if (removedWebOfLeader && tryToReleaseWebLeaderLock(leaderUuid)) {
        LOGGER.info("Released web leader lock of node {} because its web process left the cluster", nodeName);
      }
    }

    private void clearOperationalProcess(UUID uuid) {
      for (ClusterProcess clusterProcess : operationalProcesses.keySet()) {
        if (clusterProcess.getNodeUuid().equals(uuid)) {
          LOGGER.debug("Set node process off for [{}:{}] : ", clusterProcess.getNodeUuid(), clusterProcess.getProcessId());
          hzMember.getReplicatedMap(OPERATIONAL_PROCESSES).put(clusterProcess, Boolean.FALSE);
        }
      }
    }
  }
}
