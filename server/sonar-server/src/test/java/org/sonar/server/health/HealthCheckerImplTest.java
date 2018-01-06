/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.health;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.process.cluster.health.NodeDetails;
import org.sonar.process.cluster.health.NodeHealth;
import org.sonar.process.cluster.health.SharedHealthState;
import org.sonar.server.platform.WebServer;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.process.cluster.health.NodeDetails.newNodeDetailsBuilder;
import static org.sonar.process.cluster.health.NodeHealth.newNodeHealthBuilder;
import static org.sonar.server.health.Health.newHealthCheckBuilder;
import static org.sonar.server.health.Health.Status.GREEN;
import static org.sonar.server.health.Health.Status.RED;
import static org.sonar.server.health.Health.Status.YELLOW;

public class HealthCheckerImplTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private final WebServer webServer = mock(WebServer.class);
  private final SharedHealthState sharedHealthState = mock(SharedHealthState.class);
  private final Random random = new Random();

  @Test
  public void check_returns_green_status_without_any_cause_when_there_is_no_NodeHealthCheck() {
    HealthCheckerImpl underTest = new HealthCheckerImpl(webServer, new NodeHealthCheck[0]);

    assertThat(underTest.checkNode()).isEqualTo(Health.GREEN);
  }

  @Test
  public void checkNode_returns_GREEN_status_if_only_GREEN_statuses_returned_by_NodeHealthCheck() {
    List<Health.Status> statuses = IntStream.range(1, 1 + random.nextInt(20)).mapToObj(i -> GREEN).collect(Collectors.toList());
    HealthCheckerImpl underTest = newNodeHealthCheckerImpl(statuses.stream());

    assertThat(underTest.checkNode().getStatus())
      .describedAs("%s should have been computed from %s statuses", GREEN, statuses)
      .isEqualTo(GREEN);
  }

  @Test
  public void checkNode_returns_YELLOW_status_if_only_GREEN_and_at_least_one_YELLOW_statuses_returned_by_NodeHealthCheck() {
    List<Health.Status> statuses = new ArrayList<>();
    Stream.concat(
      IntStream.range(0, 1 + random.nextInt(20)).mapToObj(i -> YELLOW), // at least 1 YELLOW
      IntStream.range(0, random.nextInt(20)).mapToObj(i -> GREEN)).forEach(statuses::add); // between 0 and 19 GREEN
    Collections.shuffle(statuses);
    HealthCheckerImpl underTest = newNodeHealthCheckerImpl(statuses.stream());

    assertThat(underTest.checkNode().getStatus())
      .describedAs("%s should have been computed from %s statuses", YELLOW, statuses)
      .isEqualTo(YELLOW);
  }

  @Test
  public void checkNode_returns_RED_status_if_at_least_one_RED_status_returned_by_NodeHealthCheck() {
    List<Health.Status> statuses = new ArrayList<>();
    Stream.of(
      IntStream.range(0, 1 + random.nextInt(20)).mapToObj(i -> RED), // at least 1 RED
      IntStream.range(0, random.nextInt(20)).mapToObj(i -> YELLOW), // between 0 and 19 YELLOW
      IntStream.range(0, random.nextInt(20)).mapToObj(i -> GREEN) // between 0 and 19 GREEN
    ).flatMap(s -> s)
      .forEach(statuses::add);
    Collections.shuffle(statuses);
    HealthCheckerImpl underTest = newNodeHealthCheckerImpl(statuses.stream());

    assertThat(underTest.checkNode().getStatus())
      .describedAs("%s should have been computed from %s statuses", RED, statuses)
      .isEqualTo(RED);
  }

  @Test
  public void checkNode_returns_causes_of_all_NodeHealthCheck_whichever_their_status() {
    NodeHealthCheck[] nodeHealthChecks = IntStream.range(0, 1 + random.nextInt(20))
      .mapToObj(s -> new HardcodedHealthNodeCheck(IntStream.range(0, random.nextInt(3)).mapToObj(i -> randomAlphanumeric(3)).toArray(String[]::new)))
      .map(NodeHealthCheck.class::cast)
      .toArray(NodeHealthCheck[]::new);
    String[] expected = Arrays.stream(nodeHealthChecks).map(NodeHealthCheck::check).flatMap(s -> s.getCauses().stream()).toArray(String[]::new);

    HealthCheckerImpl underTest = new HealthCheckerImpl(webServer, nodeHealthChecks);

    assertThat(underTest.checkNode().getCauses()).containsOnly(expected);
  }

  @Test
  public void checkCluster_fails_with_ISE_in_standalone() {
    when(webServer.isStandalone()).thenReturn(true);
    HealthCheckerImpl underTest = new HealthCheckerImpl(webServer, new NodeHealthCheck[0], new ClusterHealthCheck[0], sharedHealthState);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Clustering is not enabled");

    underTest.checkCluster();
  }

  @Test
  public void checkCluster_fails_with_ISE_in_clustering_and_HealthState_is_null() {
    when(webServer.isStandalone()).thenReturn(false);
    HealthCheckerImpl underTest = new HealthCheckerImpl(webServer, new NodeHealthCheck[0], new ClusterHealthCheck[0], null);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("HealthState instance can't be null when clustering is enabled");

    underTest.checkCluster();
  }

  @Test
  public void checkCluster_returns_GREEN_when_there_is_no_ClusterHealthCheck() {
    when(webServer.isStandalone()).thenReturn(false);
    HealthCheckerImpl underTest = new HealthCheckerImpl(webServer, new NodeHealthCheck[0], new ClusterHealthCheck[0], sharedHealthState);

    assertThat(underTest.checkCluster().getHealth()).isEqualTo(Health.GREEN);
  }

  @Test
  public void checkCluster_returns_GREEN_status_if_only_GREEN_statuses_returned_by_ClusterHealthChecks() {
    when(webServer.isStandalone()).thenReturn(false);
    List<Health.Status> statuses = IntStream.range(1, 1 + random.nextInt(20)).mapToObj(i -> GREEN).collect(Collectors.toList());
    HealthCheckerImpl underTest = newClusterHealthCheckerImpl(statuses.stream());

    assertThat(underTest.checkCluster().getHealth().getStatus())
      .describedAs("%s should have been computed from %s statuses", GREEN, statuses)
      .isEqualTo(GREEN);
  }

  @Test
  public void checkCluster_returns_YELLOW_status_if_only_GREEN_and_at_least_one_YELLOW_statuses_returned_by_ClusterHealthChecks() {
    when(webServer.isStandalone()).thenReturn(false);
    List<Health.Status> statuses = new ArrayList<>();
    Stream.concat(
      IntStream.range(0, 1 + random.nextInt(20)).mapToObj(i -> YELLOW), // at least 1 YELLOW
      IntStream.range(0, random.nextInt(20)).mapToObj(i -> GREEN)).forEach(statuses::add); // between 0 and 19 GREEN
    Collections.shuffle(statuses);
    HealthCheckerImpl underTest = newClusterHealthCheckerImpl(statuses.stream());

    assertThat(underTest.checkCluster().getHealth().getStatus())
      .describedAs("%s should have been computed from %s statuses", YELLOW, statuses)
      .isEqualTo(YELLOW);
  }

  @Test
  public void checkCluster_returns_RED_status_if_at_least_one_RED_status_returned_by_ClusterHealthChecks() {
    when(webServer.isStandalone()).thenReturn(false);
    List<Health.Status> statuses = new ArrayList<>();
    Stream.of(
      IntStream.range(0, 1 + random.nextInt(20)).mapToObj(i -> RED), // at least 1 RED
      IntStream.range(0, random.nextInt(20)).mapToObj(i -> YELLOW), // between 0 and 19 YELLOW
      IntStream.range(0, random.nextInt(20)).mapToObj(i -> GREEN) // between 0 and 19 GREEN
    ).flatMap(s -> s)
      .forEach(statuses::add);
    Collections.shuffle(statuses);
    HealthCheckerImpl underTest = newClusterHealthCheckerImpl(statuses.stream());

    assertThat(underTest.checkCluster().getHealth().getStatus())
      .describedAs("%s should have been computed from %s statuses", RED, statuses)
      .isEqualTo(RED);
  }

  @Test
  public void checkCluster_returns_causes_of_all_ClusterHealthChecks_whichever_their_status() {
    when(webServer.isStandalone()).thenReturn(false);
    List<String[]> causesGroups = IntStream.range(0, 1 + random.nextInt(20))
      .mapToObj(s -> IntStream.range(0, random.nextInt(3)).mapToObj(i -> randomAlphanumeric(3)).toArray(String[]::new))
      .collect(Collectors.toList());
    ClusterHealthCheck[] clusterHealthChecks = causesGroups.stream()
      .map(HardcodedHealthClusterCheck::new)
      .map(ClusterHealthCheck.class::cast)
      .toArray(ClusterHealthCheck[]::new);
    String[] expectedCauses = causesGroups.stream().flatMap(Arrays::stream).collect(Collectors.toSet()).stream().toArray(String[]::new);

    HealthCheckerImpl underTest = new HealthCheckerImpl(webServer, new NodeHealthCheck[0], clusterHealthChecks, sharedHealthState);

    assertThat(underTest.checkCluster().getHealth().getCauses()).containsOnly(expectedCauses);
  }

  @Test
  public void checkCluster_passes_set_of_NodeHealth_returns_by_HealthState_to_all_ClusterHealthChecks() {
    when(webServer.isStandalone()).thenReturn(false);
    ClusterHealthCheck[] mockedClusterHealthChecks = IntStream.range(0, 1 + random.nextInt(3))
      .mapToObj(i -> mock(ClusterHealthCheck.class))
      .toArray(ClusterHealthCheck[]::new);
    Set<NodeHealth> nodeHealths = IntStream.range(0, 1 + random.nextInt(4)).mapToObj(i -> randomNodeHealth()).collect(Collectors.toSet());
    when(sharedHealthState.readAll()).thenReturn(nodeHealths);
    for (ClusterHealthCheck mockedClusterHealthCheck : mockedClusterHealthChecks) {
      when(mockedClusterHealthCheck.check(same(nodeHealths))).thenReturn(Health.GREEN);
    }

    HealthCheckerImpl underTest = new HealthCheckerImpl(webServer, new NodeHealthCheck[0], mockedClusterHealthChecks, sharedHealthState);
    underTest.checkCluster();

    for (ClusterHealthCheck mockedClusterHealthCheck : mockedClusterHealthChecks) {
      verify(mockedClusterHealthCheck).check(same(nodeHealths));
    }
  }

  @Test
  public void checkCluster_returns_NodeHealths_returned_by_HealthState() {
    when(webServer.isStandalone()).thenReturn(false);
    Set<NodeHealth> nodeHealths = IntStream.range(0, 1 + random.nextInt(4)).mapToObj(i -> randomNodeHealth()).collect(Collectors.toSet());
    when(sharedHealthState.readAll()).thenReturn(nodeHealths);

    HealthCheckerImpl underTest = new HealthCheckerImpl(webServer, new NodeHealthCheck[0], new ClusterHealthCheck[0], sharedHealthState);

    ClusterHealth clusterHealth = underTest.checkCluster();
    assertThat(clusterHealth.getNodes()).isEqualTo(nodeHealths);
  }

  private NodeHealth randomNodeHealth() {
    return newNodeHealthBuilder()
      .setStatus(NodeHealth.Status.values()[random.nextInt(NodeHealth.Status.values().length)])
      .setDetails(newNodeDetailsBuilder()
        .setType(random.nextBoolean() ? NodeDetails.Type.APPLICATION : NodeDetails.Type.SEARCH)
        .setName(randomAlphanumeric(10))
        .setHost(randomAlphanumeric(5))
        .setPort(1 + random.nextInt(333))
        .setStartedAt(1 + random.nextInt(444))
        .build())
      .build();
  }

  private HealthCheckerImpl newNodeHealthCheckerImpl(Stream<Health.Status> statuses) {
    Stream<HardcodedHealthNodeCheck> staticHealthCheckStream = statuses.map(HardcodedHealthNodeCheck::new);
    return new HealthCheckerImpl(
      webServer,
      staticHealthCheckStream.map(NodeHealthCheck.class::cast).toArray(NodeHealthCheck[]::new));
  }

  private HealthCheckerImpl newClusterHealthCheckerImpl(Stream<Health.Status> statuses) {
    Stream<HardcodedHealthClusterCheck> staticHealthCheckStream = statuses.map(HardcodedHealthClusterCheck::new);
    return new HealthCheckerImpl(
      webServer,
      new NodeHealthCheck[0],
      staticHealthCheckStream.map(ClusterHealthCheck.class::cast).toArray(ClusterHealthCheck[]::new),
      sharedHealthState);
  }

  private class HardcodedHealthNodeCheck implements NodeHealthCheck {
    private final Health health;

    public HardcodedHealthNodeCheck(Health.Status status) {
      this.health = newHealthCheckBuilder().setStatus(status).build();
    }

    public HardcodedHealthNodeCheck(String... causes) {
      Health.Builder builder = newHealthCheckBuilder().setStatus(Health.Status.values()[random.nextInt(3)]);
      Stream.of(causes).forEach(builder::addCause);
      this.health = builder.build();
    }

    @Override
    public Health check() {
      return health;
    }
  }

  private class HardcodedHealthClusterCheck implements ClusterHealthCheck {
    private final Health health;

    public HardcodedHealthClusterCheck(Health.Status status) {
      this.health = newHealthCheckBuilder().setStatus(status).build();
    }

    public HardcodedHealthClusterCheck(String... causes) {
      Health.Builder builder = newHealthCheckBuilder().setStatus(Health.Status.values()[random.nextInt(3)]);
      Stream.of(causes).forEach(builder::addCause);
      this.health = builder.build();
    }

    @Override
    public Health check(Set<NodeHealth> nodeHealths) {
      return health;
    }
  }
}
