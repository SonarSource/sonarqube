/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.process.cluster.health;

import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.event.Level;
import org.sonar.process.LoggingRule;
import org.sonar.process.cluster.hz.HazelcastMember;

import static java.util.Collections.singleton;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.process.cluster.health.NodeDetails.newNodeDetailsBuilder;
import static org.sonar.process.cluster.health.NodeHealth.newNodeHealthBuilder;

public class SharedHealthStateImplTest {
  private static final String MAP_SQ_HEALTH_STATE = "sq_health_state";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public LoggingRule logging = new LoggingRule(SharedHealthStateImpl.class);

  private final Random random = new Random();
  private long clusterTime = 99 + Math.abs(random.nextInt(9621));
  private HazelcastMember hazelcastMember = mock(HazelcastMember.class);
  private SharedHealthStateImpl underTest = new SharedHealthStateImpl(hazelcastMember);

  @Test
  public void write_fails_with_NPE_if_arg_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("nodeHealth can't be null");

    underTest.writeMine(null);
  }

  @Test
  public void write_put_arg_into_map_sq_health_state_under_current_client_uuid() {
    NodeHealth nodeHealth = randomNodeHealth();
    Map<String, TimestampedNodeHealth> map = new HashMap<>();
    doReturn(map).when(hazelcastMember).getReplicatedMap(MAP_SQ_HEALTH_STATE);
    long clusterTime = random.nextLong();
    String uuid = randomAlphanumeric(5);
    when(hazelcastMember.getUuid()).thenReturn(uuid);
    when(hazelcastMember.getClusterTime()).thenReturn(clusterTime);

    underTest.writeMine(nodeHealth);

    assertThat(map.size()).isEqualTo(1);
    assertThat(map.get(uuid)).isEqualTo(new TimestampedNodeHealth(nodeHealth, clusterTime));
    assertThat(logging.getLogs()).isEmpty();
  }

  @Test
  public void write_logs_map_sq_health_state_content_and_NodeHealth_to_be_added_if_TRACE() {
    logging.setLevel(Level.TRACE);
    NodeHealth newNodeHealth = randomNodeHealth();
    Map<String, TimestampedNodeHealth> map = new HashMap<>();
    map.put(randomAlphanumeric(4), new TimestampedNodeHealth(randomNodeHealth(), random.nextLong()));
    doReturn(new HashMap<>(map)).when(hazelcastMember).getReplicatedMap(MAP_SQ_HEALTH_STATE);
    String uuid = randomAlphanumeric(5);
    when(hazelcastMember.getUuid()).thenReturn(uuid);

    underTest.writeMine(newNodeHealth);

    assertThat(logging.getLogs()).hasSize(1);
    assertThat(logging.hasLog(Level.TRACE, "Reading " + map + " and adding " + newNodeHealth)).isTrue();
  }

  @Test
  public void readAll_returns_all_NodeHealth_in_map_sq_health_state_for_existing_client_uuids_aged_less_than_30_seconds() {
    NodeHealth[] nodeHealths = IntStream.range(0, 1 + random.nextInt(6)).mapToObj(i -> randomNodeHealth()).toArray(NodeHealth[]::new);
    Map<String, TimestampedNodeHealth> allNodeHealths = new HashMap<>();
    Map<String, NodeHealth> expected = new HashMap<>();
    String randomUuidBase = randomAlphanumeric(5);
    for (int i = 0; i < nodeHealths.length; i++) {
      String memberUuid = randomUuidBase + i;
      TimestampedNodeHealth timestampedNodeHealth = new TimestampedNodeHealth(nodeHealths[i], clusterTime - random.nextInt(30 * 1000));
      allNodeHealths.put(memberUuid, timestampedNodeHealth);
      if (random.nextBoolean()) {
        expected.put(memberUuid, nodeHealths[i]);
      }
    }
    doReturn(allNodeHealths).when(hazelcastMember).getReplicatedMap(MAP_SQ_HEALTH_STATE);
    when(hazelcastMember.getMemberUuids()).thenReturn(expected.keySet());
    when(hazelcastMember.getClusterTime()).thenReturn(clusterTime);

    assertThat(underTest.readAll())
      .containsOnly(expected.values().stream().toArray(NodeHealth[]::new));
    assertThat(logging.getLogs()).isEmpty();
  }

  @Test
  public void readAll_ignores_NodeHealth_of_30_seconds_before_cluster_time() {
    NodeHealth nodeHealth = randomNodeHealth();
    Map<String, TimestampedNodeHealth> map = new HashMap<>();
    String memberUuid = randomAlphanumeric(5);
    TimestampedNodeHealth timestampedNodeHealth = new TimestampedNodeHealth(nodeHealth, clusterTime - 30 * 1000);
    map.put(memberUuid, timestampedNodeHealth);
    doReturn(map).when(hazelcastMember).getReplicatedMap(MAP_SQ_HEALTH_STATE);
    when(hazelcastMember.getMemberUuids()).thenReturn(map.keySet());
    when(hazelcastMember.getClusterTime()).thenReturn(clusterTime);

    assertThat(underTest.readAll()).isEmpty();
  }

  @Test
  public void readAll_ignores_NodeHealth_of_more_than_30_seconds_before_cluster_time() {
    NodeHealth nodeHealth = randomNodeHealth();
    Map<String, TimestampedNodeHealth> map = new HashMap<>();
    String memberUuid = randomAlphanumeric(5);
    TimestampedNodeHealth timestampedNodeHealth = new TimestampedNodeHealth(nodeHealth, clusterTime - 30 * 1000 - random.nextInt(99));
    map.put(memberUuid, timestampedNodeHealth);
    doReturn(map).when(hazelcastMember).getReplicatedMap(MAP_SQ_HEALTH_STATE);
    when(hazelcastMember.getMemberUuids()).thenReturn(map.keySet());
    when(hazelcastMember.getClusterTime()).thenReturn(clusterTime);

    assertThat(underTest.readAll()).isEmpty();
  }

  @Test
  public void readAll_logs_map_sq_health_state_content_and_the_content_effectively_returned_if_TRACE() {
    logging.setLevel(Level.TRACE);
    Map<String, TimestampedNodeHealth> map = new HashMap<>();
    String uuid = randomAlphanumeric(44);
    NodeHealth nodeHealth = randomNodeHealth();
    map.put(uuid, new TimestampedNodeHealth(nodeHealth, clusterTime - 1));
    when(hazelcastMember.getClusterTime()).thenReturn(clusterTime);
    when(hazelcastMember.getMemberUuids()).thenReturn(singleton(uuid));
    doReturn(map).when(hazelcastMember).getReplicatedMap(MAP_SQ_HEALTH_STATE);

    underTest.readAll();

    assertThat(logging.getLogs()).hasSize(1);
    assertThat(logging.hasLog(Level.TRACE, "Reading " + new HashMap<>(map) + " and keeping " + singleton(nodeHealth))).isTrue();
  }

  @Test
  public void readAll_logs_message_for_each_non_existing_member_ignored_if_TRACE() {
    logging.setLevel(Level.TRACE);
    Map<String, TimestampedNodeHealth> map = new HashMap<>();
    String memberUuid1 = randomAlphanumeric(44);
    String memberUuid2 = randomAlphanumeric(44);
    map.put(memberUuid1, new TimestampedNodeHealth(randomNodeHealth(), clusterTime - 1));
    map.put(memberUuid2, new TimestampedNodeHealth(randomNodeHealth(), clusterTime - 1));
    when(hazelcastMember.getClusterTime()).thenReturn(clusterTime);
    doReturn(map).when(hazelcastMember).getReplicatedMap(MAP_SQ_HEALTH_STATE);

    underTest.readAll();

    assertThat(logging.getLogs()).hasSize(3);
    assertThat(logging.getLogs(Level.TRACE))
      .containsOnly(
        "Reading " + new HashMap<>(map) + " and keeping []",
        "Ignoring NodeHealth of member " + memberUuid1 + " because it is not part of the cluster at the moment",
        "Ignoring NodeHealth of member " + memberUuid2 + " because it is not part of the cluster at the moment");
  }

  @Test
  public void readAll_logs_message_for_each_timed_out_NodeHealth_ignored_if_TRACE() {
    logging.setLevel(Level.TRACE);
    Map<String, TimestampedNodeHealth> map = new HashMap<>();
    String memberUuid1 = randomAlphanumeric(44);
    String memberUuid2 = randomAlphanumeric(44);
    map.put(memberUuid1, new TimestampedNodeHealth(randomNodeHealth(), clusterTime - 30 * 1000));
    map.put(memberUuid2, new TimestampedNodeHealth(randomNodeHealth(), clusterTime - 30 * 1000));
    doReturn(map).when(hazelcastMember).getReplicatedMap(MAP_SQ_HEALTH_STATE);
    when(hazelcastMember.getMemberUuids()).thenReturn(ImmutableSet.of(memberUuid1, memberUuid2));
    when(hazelcastMember.getClusterTime()).thenReturn(clusterTime);

    underTest.readAll();

    assertThat(logging.getLogs()).hasSize(3);
    assertThat(logging.getLogs(Level.TRACE))
      .containsOnly(
        "Reading " + new HashMap<>(map) + " and keeping []",
        "Ignoring NodeHealth of member " + memberUuid1 + " because it is too old",
        "Ignoring NodeHealth of member " + memberUuid2 + " because it is too old");
  }

  @Test
  public void clearMine_clears_entry_into_map_sq_health_state_under_current_client_uuid() {
    Map<String, TimestampedNodeHealth> map = mock(Map.class);
    doReturn(map).when(hazelcastMember).getReplicatedMap(MAP_SQ_HEALTH_STATE);
    String uuid = randomAlphanumeric(5);
    when(hazelcastMember.getUuid()).thenReturn(uuid);

    underTest.clearMine();

    verify(map).remove(uuid);
    verifyNoMoreInteractions(map);
    assertThat(logging.getLogs()).isEmpty();
  }

  @Test
  public void clearMine_logs_map_sq_health_state_and_current_client_uuid_if_TRACE() {
    logging.setLevel(Level.TRACE);
    Map<String, TimestampedNodeHealth> map = new HashMap<>();
    map.put(randomAlphanumeric(4), new TimestampedNodeHealth(randomNodeHealth(), random.nextLong()));
    doReturn(map).when(hazelcastMember).getReplicatedMap(MAP_SQ_HEALTH_STATE);
    String uuid = randomAlphanumeric(5);
    when(hazelcastMember.getUuid()).thenReturn(uuid);

    underTest.clearMine();

    assertThat(logging.getLogs()).hasSize(1);
    assertThat(logging.hasLog(Level.TRACE, "Reading " + map + " and clearing for " + uuid)).isTrue();
  }

  private NodeHealth randomNodeHealth() {
    return newNodeHealthBuilder()
      .setStatus(NodeHealth.Status.values()[random.nextInt(NodeHealth.Status.values().length)])
      .setDetails(newNodeDetailsBuilder()
        .setType(random.nextBoolean() ? NodeDetails.Type.SEARCH : NodeDetails.Type.APPLICATION)
        .setName(randomAlphanumeric(30))
        .setHost(randomAlphanumeric(10))
        .setPort(1 + random.nextInt(666))
        .setStartedAt(1 + random.nextInt(852))
        .build())
      .build();
  }
}
