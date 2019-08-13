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
package org.sonar.server.health;

import java.util.Arrays;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.Test;
import org.sonar.process.cluster.health.NodeDetails;
import org.sonar.process.cluster.health.NodeHealth;

import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.of;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.sonar.process.cluster.health.NodeHealth.Status.GREEN;
import static org.sonar.process.cluster.health.NodeHealth.Status.RED;
import static org.sonar.process.cluster.health.NodeHealth.Status.YELLOW;
import static org.sonar.server.health.HealthAssert.assertThat;

public class AppNodeClusterCheckTest {
  private final Random random = new Random();

  private AppNodeClusterCheck underTest = new AppNodeClusterCheck();

  @Test
  public void status_RED_when_no_application_node() {
    Set<NodeHealth> nodeHealths = nodeHealths().collect(toSet());

    Health check = underTest.check(nodeHealths);

    assertThat(check)
      .forInput(nodeHealths)
      .hasStatus(Health.Status.RED)
      .andCauses("No application node");
  }

  @Test
  public void status_RED_when_single_RED_application_node() {
    Set<NodeHealth> nodeHealths = nodeHealths(RED).collect(toSet());

    Health check = underTest.check(nodeHealths);

    assertThat(check)
      .forInput(nodeHealths)
      .hasStatus(Health.Status.RED)
      .andCauses("Status of all application nodes is RED",
        "There should be at least two application nodes");
  }

  @Test
  public void status_YELLOW_when_single_YELLOW_application_node() {
    Set<NodeHealth> nodeHealths = nodeHealths(YELLOW).collect(toSet());

    Health check = underTest.check(nodeHealths);

    assertThat(check)
      .forInput(nodeHealths)
      .hasStatus(Health.Status.YELLOW)
      .andCauses(
        "Status of all application nodes is YELLOW",
        "There should be at least two application nodes");
  }

  @Test
  public void status_YELLOW_when_single_GREEN_application_node() {
    Set<NodeHealth> nodeHealths = nodeHealths(GREEN).collect(toSet());

    Health check = underTest.check(nodeHealths);

    assertThat(check)
      .forInput(nodeHealths)
      .hasStatus(Health.Status.YELLOW)
      .andCauses("There should be at least two application nodes");
  }

  @Test
  public void status_RED_when_two_RED_application_nodes() {
    Set<NodeHealth> nodeHealths = nodeHealths(RED, RED).collect(toSet());

    Health check = underTest.check(nodeHealths);

    assertThat(check)
      .forInput(nodeHealths)
      .hasStatus(Health.Status.RED)
      .andCauses("Status of all application nodes is RED");
  }

  @Test
  public void status_YELLOW_when_two_YELLOW_application_nodes() {
    Set<NodeHealth> nodeHealths = nodeHealths(YELLOW, YELLOW).collect(toSet());

    Health check = underTest.check(nodeHealths);

    assertThat(check)
      .forInput(nodeHealths)
      .hasStatus(Health.Status.YELLOW)
      .andCauses("Status of all application nodes is YELLOW");
  }

  @Test
  public void status_YELLOW_when_one_RED_node_and_one_YELLOW_application_node() {
    Set<NodeHealth> nodeHealths = nodeHealths(RED, YELLOW).collect(toSet());

    Health check = underTest.check(nodeHealths);

    assertThat(check)
      .forInput(nodeHealths)
      .hasStatus(Health.Status.YELLOW)
      .andCauses(
        "At least one application node is RED",
        "At least one application node is YELLOW");
  }

  @Test
  public void status_YELLOW_when_one_RED_node_and_one_GREEN_application_node() {
    Set<NodeHealth> nodeHealths = nodeHealths(RED, GREEN).collect(toSet());

    Health check = underTest.check(nodeHealths);

    assertThat(check)
      .forInput(nodeHealths)
      .hasStatus(Health.Status.YELLOW)
      .andCauses("At least one application node is RED");
  }

  @Test
  public void status_YELLOW_when_one_YELLOW_node_and_one_GREEN_application_node() {
    Set<NodeHealth> nodeHealths = nodeHealths(YELLOW, GREEN).collect(toSet());

    Health check = underTest.check(nodeHealths);

    assertThat(check)
      .forInput(nodeHealths)
      .hasStatus(Health.Status.YELLOW)
      .andCauses("At least one application node is YELLOW");
  }

  @Test
  public void status_GREEN_when_two_GREEN_application_node() {
    Set<NodeHealth> nodeHealths = nodeHealths(GREEN, GREEN).collect(toSet());

    Health check = underTest.check(nodeHealths);

    assertThat(check)
      .forInput(nodeHealths)
      .hasStatus(Health.Status.GREEN)
      .andCauses();
  }

  @Test
  public void status_GREEN_when_two_GREEN_application_node_and_any_number_of_other_is_GREEN() {
    Set<NodeHealth> nodeHealths = of(
      // at least 1 extra GREEN
      of(appNodeHealth(GREEN)),
      // 0 to 10 GREEN
      randomNumberOfAppNodeHealthOfAnyStatus(GREEN),
      // 2 GREEN
      nodeHealths(GREEN, GREEN))
        .flatMap(s -> s)
        .collect(toSet());

    Health check = underTest.check(nodeHealths);

    assertThat(check)
      .forInput(nodeHealths)
      .hasStatus(Health.Status.GREEN)
      .andCauses();
  }

  @Test
  public void status_YELLOW_when_two_GREEN_application_node_and_any_number_of_other_is_YELLOW_or_GREEN() {
    Set<NodeHealth> nodeHealths = of(
      // at least 1 YELLOW
      of(appNodeHealth(YELLOW)),
      // 0 to 10 YELLOW/GREEN
      randomNumberOfAppNodeHealthOfAnyStatus(GREEN, YELLOW),
      // 2 GREEN
      nodeHealths(GREEN, GREEN))
        .flatMap(s -> s)
        .collect(toSet());
    Health check = underTest.check(nodeHealths);

    assertThat(check)
      .forInput(nodeHealths)
      .hasStatus(Health.Status.YELLOW)
      .andCauses("At least one application node is YELLOW");
  }

  @Test
  public void status_YELLOW_when_two_GREEN_application_node_and_any_number_of_other_is_RED_or_GREEN() {
    Set<NodeHealth> nodeHealths = of(
      // at least 1 RED
      of(appNodeHealth(RED)),
      // 0 to 10 RED/GREEN
      randomNumberOfAppNodeHealthOfAnyStatus(GREEN, RED),
      // 2 GREEN
      nodeHealths(GREEN, GREEN))
        .flatMap(s -> s)
        .collect(toSet());
    Health check = underTest.check(nodeHealths);

    assertThat(check)
      .forInput(nodeHealths)
      .hasStatus(Health.Status.YELLOW)
      .andCauses("At least one application node is RED");
  }

  @Test
  public void status_YELLOW_when_two_GREEN_application_node_and_any_number_of_other_is_either_RED_or_YELLOW() {
    Set<NodeHealth> nodeHealths = of(
      // at least 1 RED
      of(appNodeHealth(RED)),
      // at least 1 YELLOW
      of(appNodeHealth(YELLOW)),
      // 0 to 10 RED/YELLOW/GREEN
      randomNumberOfAppNodeHealthOfAnyStatus(RED, YELLOW, GREEN),
      // 2 GREEN
      nodeHealths(GREEN, GREEN))
        .flatMap(s -> s)
        .collect(toSet());

    Health check = underTest.check(nodeHealths);

    assertThat(check)
      .forInput(nodeHealths)
      .hasStatus(Health.Status.YELLOW)
      .andCauses(
        "At least one application node is YELLOW",
        "At least one application node is RED");
  }

  /**
   * Between 0 and 10 NodeHealth of Application node with any of the specified statuses.
   */
  private Stream<NodeHealth> randomNumberOfAppNodeHealthOfAnyStatus(NodeHealth.Status... randomStatuses) {
    return IntStream.range(0, random.nextInt(10))
      .mapToObj(i -> appNodeHealth(randomStatuses[random.nextInt(randomStatuses.length)]));
  }

  private Stream<NodeHealth> nodeHealths(NodeHealth.Status... appNodeStatuses) {
    return of(
      // random number of Search nodes with random status
      IntStream.range(0, random.nextInt(3))
        .mapToObj(i -> appNodeHealth(NodeDetails.Type.SEARCH, NodeHealth.Status.values()[random.nextInt(NodeHealth.Status.values().length)])),
      Arrays.stream(appNodeStatuses).map(this::appNodeHealth))
        .flatMap(s -> s);
  }

  private NodeHealth appNodeHealth(NodeHealth.Status status) {
    return appNodeHealth(NodeDetails.Type.APPLICATION, status);
  }

  private NodeHealth appNodeHealth(NodeDetails.Type type, NodeHealth.Status status) {
    return NodeHealth.newNodeHealthBuilder()
      .setStatus(status)
      .setDetails(NodeDetails.newNodeDetailsBuilder()
        .setType(type)
        .setHost(randomAlphanumeric(32))
        .setName(randomAlphanumeric(32))
        .setPort(1 + random.nextInt(88))
        .setStartedAt(1 + random.nextInt(54))
        .build())
      .build();
  }

}
