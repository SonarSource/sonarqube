/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.cluster.health;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.cluster.localclient.HazelcastClient;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.sonar.cluster.health.NodeDetails.newNodeDetailsBuilder;
import static org.sonar.cluster.health.NodeHealth.newNodeHealthBuilder;

public class SharedHealthStateImplTest {
  private static final String MAP_SQ_HEALTH_STATE = "sq_health_state";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public LogTester logTester = new LogTester();

  private final Random random = new Random();
  private HazelcastClient hazelcastClient = Mockito.mock(HazelcastClient.class);
  private SharedHealthStateImpl underTest = new SharedHealthStateImpl(hazelcastClient);

  @Test
  public void write_fails_with_NPE_if_arg_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("nodeHealth can't be null");

    underTest.writeMine(null);
  }

  @Test
  public void write_put_arg_into_map_sq_health_state_under_current_client_uuid() {
    NodeHealth nodeHealth = randomNodeHealth();
    Map<String, NodeHealth> map = new HashMap<>();
    doReturn(map).when(hazelcastClient).getReplicatedMap(MAP_SQ_HEALTH_STATE);
    String uuid = randomAlphanumeric(5);
    when(hazelcastClient.getClientUUID()).thenReturn(uuid);

    underTest.writeMine(nodeHealth);

    assertThat(map.size()).isEqualTo(1);
    assertThat(map.get(uuid)).isSameAs(nodeHealth);
    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  public void write_logs_map_sq_health_state_content_and_NodeHealth_to_be_added_if_TRACE() {
    logTester.setLevel(LoggerLevel.TRACE);
    NodeHealth newNodeHealth = randomNodeHealth();
    Map<String, NodeHealth> map = new HashMap<>();
    map.put(randomAlphanumeric(4), randomNodeHealth());
    doReturn(new HashMap<>(map)).when(hazelcastClient).getReplicatedMap(MAP_SQ_HEALTH_STATE);
    String uuid = randomAlphanumeric(5);
    when(hazelcastClient.getClientUUID()).thenReturn(uuid);

    underTest.writeMine(newNodeHealth);

    assertThat(logTester.logs()).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.TRACE).iterator().next()).isEqualTo("Reading " + map + " and adding " + newNodeHealth);

  }

  @Test
  public void readAll_returns_all_NodeHealth_in_map_sq_health_state() {
    NodeHealth[] expected = IntStream.range(0, 1 + random.nextInt(6)).mapToObj(i -> randomNodeHealth()).toArray(NodeHealth[]::new);
    Map<String, NodeHealth> map = new HashMap<>();
    String randomUuidBase = randomAlphanumeric(5);
    for (int i = 0; i < expected.length; i++) {
      map.put(randomUuidBase + i, expected[i]);
    }
    doReturn(map).when(hazelcastClient).getReplicatedMap(MAP_SQ_HEALTH_STATE);

    assertThat(underTest.readAll()).containsOnly(expected);
    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  public void readAll_logs_map_sq_health_state_content_if_TRACE() {
    logTester.setLevel(LoggerLevel.TRACE);
    Map<String, NodeHealth> map = new HashMap<>();
    map.put(randomAlphanumeric(44), randomNodeHealth());
    doReturn(map).when(hazelcastClient).getReplicatedMap(MAP_SQ_HEALTH_STATE);

    underTest.readAll();

    assertThat(logTester.logs()).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.TRACE).iterator().next()).isEqualTo("Reading " + map);
  }

  private NodeHealth randomNodeHealth() {
    return newNodeHealthBuilder()
      .setStatus(NodeHealth.Status.values()[random.nextInt(NodeHealth.Status.values().length)])
      .setDetails(newNodeDetailsBuilder()
        .setType(random.nextBoolean() ? NodeDetails.Type.SEARCH : NodeDetails.Type.APPLICATION)
        .setName(randomAlphanumeric(30))
        .setHost(randomAlphanumeric(10))
        .setPort(1 + random.nextInt(666))
        .setStarted(1 + random.nextInt(852))
        .build())
      .setDate(1 + random.nextInt(999))
      .build();
  }
}
