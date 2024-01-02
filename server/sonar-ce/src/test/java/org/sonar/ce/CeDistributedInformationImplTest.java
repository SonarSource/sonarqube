/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.ce;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.hazelcast.core.HazelcastInstanceNotActiveException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.ce.taskprocessor.CeWorker;
import org.sonar.ce.taskprocessor.CeWorkerFactory;
import org.sonar.process.cluster.hz.HazelcastMember;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.process.cluster.hz.HazelcastObjects.WORKER_UUIDS;

public class CeDistributedInformationImplTest {
  @Rule
  public final LogTester logTester = new LogTester();

  private final UUID clientUUID1 = UUID.randomUUID();
  private final UUID clientUUID2 = UUID.randomUUID();
  private final UUID clientUUID3 = UUID.randomUUID();

  private final String w1 = UUID.randomUUID().toString();
  private final String w2 = UUID.randomUUID().toString();
  private final String w3 = UUID.randomUUID().toString();
  private final String w4 = UUID.randomUUID().toString();
  private final String w5 = UUID.randomUUID().toString();
  private final String w6 = UUID.randomUUID().toString();

  private final Map<UUID, Set<String>> workerMap = ImmutableMap.of(
    clientUUID1, ImmutableSet.of(w1, w2),
    clientUUID2, ImmutableSet.of(w3),
    clientUUID3, ImmutableSet.of(w4, w5, w6));

  private final HazelcastMember hzClientWrapper = mock(HazelcastMember.class);

  @Test
  public void getWorkerUUIDs_returns_union_of_workers_uuids_of_local_and_cluster_worker_uuids() {
    when(hzClientWrapper.getUuid()).thenReturn(clientUUID1);
    when(hzClientWrapper.getMemberUuids()).thenReturn(ImmutableSet.of(clientUUID1, clientUUID2, clientUUID3));
    when(hzClientWrapper.<UUID, Set<String>>getReplicatedMap(WORKER_UUIDS)).thenReturn(workerMap);

    CeDistributedInformation ceDistributedInformation = new CeDistributedInformationImpl(hzClientWrapper, mock(CeWorkerFactory.class));
    assertThat(ceDistributedInformation.getWorkerUUIDs()).containsExactlyInAnyOrder(w1, w2, w3, w4, w5, w6);
  }

  @Test
  public void getWorkerUUIDs_must_filter_absent_client() {
    when(hzClientWrapper.getUuid()).thenReturn(clientUUID1);
    when(hzClientWrapper.getMemberUuids()).thenReturn(ImmutableSet.of(clientUUID1, clientUUID2));
    when(hzClientWrapper.<UUID, Set<String>>getReplicatedMap(WORKER_UUIDS)).thenReturn(workerMap);

    CeDistributedInformation ceDistributedInformation = new CeDistributedInformationImpl(hzClientWrapper, mock(CeWorkerFactory.class));
    assertThat(ceDistributedInformation.getWorkerUUIDs()).containsExactlyInAnyOrder(w1, w2, w3);
  }

  @Test
  public void broadcastWorkerUUIDs_adds_local_workerUUIDs_to_shared_map_under_key_of_localendpoint_uuid() {
    Set<UUID> connectedClients = new HashSet<>();
    Map<UUID, Set<String>> modifiableWorkerMap = new HashMap<>();
    connectedClients.add(clientUUID1);
    connectedClients.add(clientUUID2);

    when(hzClientWrapper.getUuid()).thenReturn(clientUUID1);
    when(hzClientWrapper.getMemberUuids()).thenReturn(connectedClients);
    when(hzClientWrapper.<UUID, Set<String>>getReplicatedMap(WORKER_UUIDS)).thenReturn(modifiableWorkerMap);

    CeWorkerFactory ceWorkerFactory = mock(CeWorkerFactory.class);
    Set<CeWorker> ceWorkers = Stream.of("a10", "a11").map(uuid -> {
      CeWorker res = mock(CeWorker.class);
      when(res.getUUID()).thenReturn(uuid);
      return res;
    }).collect(Collectors.toSet());
    when(ceWorkerFactory.getWorkers()).thenReturn(ceWorkers);
    CeDistributedInformationImpl ceDistributedInformation = new CeDistributedInformationImpl(hzClientWrapper, ceWorkerFactory);

    try {
      ceDistributedInformation.broadcastWorkerUUIDs();
      assertThat(modifiableWorkerMap).containsExactly(
        entry(clientUUID1, Set.of("a10", "a11")));
    } finally {
      ceDistributedInformation.stop();
    }
  }

  @Test
  public void stop_must_remove_local_workerUUIDs() {
    Set<UUID> connectedClients = new HashSet<>();
    connectedClients.add(clientUUID1);
    connectedClients.add(clientUUID2);
    connectedClients.add(clientUUID3);
    Map<UUID, Set<String>> modifiableWorkerMap = new HashMap<>(workerMap);

    when(hzClientWrapper.getUuid()).thenReturn(clientUUID1);
    when(hzClientWrapper.getMemberUuids()).thenReturn(connectedClients);
    when(hzClientWrapper.<UUID, Set<String>>getReplicatedMap(WORKER_UUIDS)).thenReturn(modifiableWorkerMap);

    CeDistributedInformationImpl ceDistributedInformation = new CeDistributedInformationImpl(hzClientWrapper, mock(CeWorkerFactory.class));
    ceDistributedInformation.stop();
    assertThat(modifiableWorkerMap).containsExactlyInAnyOrderEntriesOf(
      Map.of(clientUUID2, Set.of(w3), clientUUID3, ImmutableSet.of(w4, w5, w6)));
  }

  @Test
  public void stop_whenThrowHazelcastInactiveException_shouldSilenceError() {
    logTester.setLevel(Level.DEBUG);
    when(hzClientWrapper.getReplicatedMap(any())).thenThrow(new HazelcastInstanceNotActiveException("Hazelcast is not active"));

    CeDistributedInformationImpl ceDistributedInformation = new CeDistributedInformationImpl(hzClientWrapper, mock(CeWorkerFactory.class));
    ceDistributedInformation.stop();

    assertThat(logTester.logs(Level.DEBUG)).contains("Hazelcast is not active anymore");
    assertThat(logTester.logs(Level.ERROR)).isEmpty();
  }

}
