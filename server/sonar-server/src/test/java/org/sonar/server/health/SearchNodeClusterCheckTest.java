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

public class SearchNodeClusterCheckTest {
  private final Random random = new Random();

  private SearchNodeClusterCheck underTest = new SearchNodeClusterCheck();

  @Test
  public void status_RED_when_no_search_node() {
    Set<NodeHealth> nodeHealths = nodeHealths().collect(toSet());

    Health check = underTest.check(nodeHealths);

    assertThat(check)
      .forInput(nodeHealths)
      .hasStatus(Health.Status.RED)
      .andCauses("No search node");
  }

  @Test
  public void status_RED_when_single_RED_search_node() {
    Set<NodeHealth> nodeHealths = nodeHealths(RED).collect(toSet());

    Health check = underTest.check(nodeHealths);

    assertThat(check)
      .forInput(nodeHealths)
      .hasStatus(Health.Status.RED)
      .andCauses("Status of all search nodes is RED",
        "There should be at least three search nodes");
  }

  @Test
  public void status_RED_when_single_YELLOW_search_node() {
    Set<NodeHealth> nodeHealths = nodeHealths(YELLOW).collect(toSet());

    Health check = underTest.check(nodeHealths);

    assertThat(check)
      .forInput(nodeHealths)
      .hasStatus(Health.Status.RED)
      .andCauses("Status of all search nodes is YELLOW",
        "There should be at least three search nodes");
  }

  @Test
  public void status_RED_when_single_GREEN_search_node() {
    Set<NodeHealth> nodeHealths = nodeHealths(GREEN).collect(toSet());

    Health check = underTest.check(nodeHealths);

    assertThat(check)
      .forInput(nodeHealths)
      .hasStatus(Health.Status.RED)
      .andCauses("There should be at least three search nodes");
  }

  @Test
  public void status_RED_when_two_RED_search_nodes() {
    Set<NodeHealth> nodeHealths = nodeHealths(RED, RED).collect(toSet());

    Health check = underTest.check(nodeHealths);

    assertThat(check)
      .forInput(nodeHealths)
      .hasStatus(Health.Status.RED)
      .andCauses("Status of all search nodes is RED",
        "There should be at least three search nodes");
  }

  @Test
  public void status_YELLOW_when_two_YELLOW_search_nodes() {
    Set<NodeHealth> nodeHealths = nodeHealths(YELLOW, YELLOW).collect(toSet());

    Health check = underTest.check(nodeHealths);

    assertThat(check)
      .forInput(nodeHealths)
      .hasStatus(Health.Status.YELLOW)
      .andCauses("Status of all search nodes is YELLOW",
        "There should be at least three search nodes");
  }

  @Test
  public void status_YELLOW_when_two_GREEN_search_nodes() {
    Set<NodeHealth> nodeHealths = nodeHealths(GREEN, GREEN).collect(toSet());

    Health check = underTest.check(nodeHealths);

    assertThat(check)
      .forInput(nodeHealths)
      .hasStatus(Health.Status.YELLOW)
      .andCauses("There should be at least three search nodes");
  }

  @Test
  public void status_YELLOW_when_one_GREEN_and_one_YELLOW_search_nodes() {
    Set<NodeHealth> nodeHealths = nodeHealths(GREEN, YELLOW).collect(toSet());

    Health check = underTest.check(nodeHealths);

    assertThat(check)
      .forInput(nodeHealths)
      .hasStatus(Health.Status.YELLOW)
      .andCauses("At least one search node is YELLOW",
        "There should be at least three search nodes");
  }

  @Test
  public void status_RED_when_one_GREEN_and_one_RED_search_nodes() {
    Set<NodeHealth> nodeHealths = nodeHealths(GREEN, RED).collect(toSet());

    Health check = underTest.check(nodeHealths);

    assertThat(check)
      .forInput(nodeHealths)
      .hasStatus(Health.Status.RED)
      .andCauses("At least one search node is RED",
        "There should be at least three search nodes");
  }

  @Test
  public void status_RED_when_one_YELLOW_and_one_RED_search_nodes() {
    Set<NodeHealth> nodeHealths = nodeHealths(YELLOW, RED).collect(toSet());

    Health check = underTest.check(nodeHealths);

    assertThat(check)
      .forInput(nodeHealths)
      .hasStatus(Health.Status.RED)
      .andCauses("At least one search node is RED",
        "At least one search node is YELLOW",
        "There should be at least three search nodes");
  }

  @Test
  public void status_RED_when_three_RED_search_nodes() {
    Set<NodeHealth> nodeHealths = nodeHealths(RED, RED, RED).collect(toSet());

    Health check = underTest.check(nodeHealths);

    assertThat(check)
      .forInput(nodeHealths)
      .hasStatus(Health.Status.RED)
      .andCauses("Status of all search nodes is RED");
  }

  @Test
  public void status_YELLOW_when_three_YELLOW_search_nodes() {
    Set<NodeHealth> nodeHealths = nodeHealths(YELLOW, YELLOW, YELLOW).collect(toSet());

    Health check = underTest.check(nodeHealths);

    assertThat(check)
      .forInput(nodeHealths)
      .hasStatus(Health.Status.YELLOW)
      .andCauses("Status of all search nodes is YELLOW");
  }

  @Test
  public void status_GREEN_when_three_GREEN_search_nodes() {
    Set<NodeHealth> nodeHealths = nodeHealths(GREEN, GREEN, GREEN).collect(toSet());

    Health check = underTest.check(nodeHealths);

    assertThat(check)
      .forInput(nodeHealths)
      .hasStatus(Health.Status.GREEN)
      .andCauses();
  }

  @Test
  public void status_RED_when_two_RED_and_one_YELLOW_search_nodes() {
    Set<NodeHealth> nodeHealths = nodeHealths(RED, RED, YELLOW).collect(toSet());

    Health check = underTest.check(nodeHealths);

    assertThat(check)
      .forInput(nodeHealths)
      .hasStatus(Health.Status.RED)
      .andCauses("At least one search node is RED",
        "At least one search node is YELLOW");
  }

  @Test
  public void status_YELLOW_when_two_YELLOW_and_one_RED_search_nodes() {
    Set<NodeHealth> nodeHealths = nodeHealths(RED, YELLOW, YELLOW).collect(toSet());

    Health check = underTest.check(nodeHealths);

    assertThat(check)
      .forInput(nodeHealths)
      .hasStatus(Health.Status.YELLOW)
      .andCauses("At least one search node is RED",
        "At least one search node is YELLOW");
  }

  @Test
  public void status_YELLOW_when_two_GREEN_and_one_RED_search_nodes() {
    Set<NodeHealth> nodeHealths = nodeHealths(GREEN, GREEN, RED).collect(toSet());

    Health check = underTest.check(nodeHealths);

    assertThat(check)
      .forInput(nodeHealths)
      .hasStatus(Health.Status.YELLOW)
      .andCauses("At least one search node is RED");
  }

  @Test
  public void status_YELLOW_when_two_GREEN_and_one_YELLOW_search_nodes() {
    Set<NodeHealth> nodeHealths = nodeHealths(GREEN, GREEN, YELLOW).collect(toSet());

    Health check = underTest.check(nodeHealths);

    assertThat(check)
      .forInput(nodeHealths)
      .hasStatus(Health.Status.YELLOW)
      .andCauses("At least one search node is YELLOW");
  }

  @Test
  public void status_YELLOW_when_two_GREEN_and_two_RED_search_nodes() {
    Set<NodeHealth> nodeHealths = nodeHealths(GREEN, GREEN, RED, RED).collect(toSet());

    Health check = underTest.check(nodeHealths);

    assertThat(check)
      .forInput(nodeHealths)
      .hasStatus(Health.Status.YELLOW)
      .andCauses("There should be an odd number of search nodes",
        "At least one search node is RED");
  }

  @Test
  public void status_YELLOW_when_two_GREEN_and_two_YELLOW_search_nodes() {
    Set<NodeHealth> nodeHealths = nodeHealths(GREEN, GREEN, YELLOW, YELLOW).collect(toSet());

    Health check = underTest.check(nodeHealths);

    assertThat(check)
      .forInput(nodeHealths)
      .hasStatus(Health.Status.YELLOW)
      .andCauses("There should be an odd number of search nodes",
        "At least one search node is YELLOW");
  }

  @Test
  public void status_YELLOW_when_two_GREEN_and_one_YELLOW_and_one_RED_search_nodes() {
    Set<NodeHealth> nodeHealths = nodeHealths(GREEN, GREEN, YELLOW, RED).collect(toSet());

    Health check = underTest.check(nodeHealths);

    assertThat(check)
      .forInput(nodeHealths)
      .hasStatus(Health.Status.YELLOW)
      .andCauses("There should be an odd number of search nodes",
        "At least one search node is RED",
        "At least one search node is YELLOW");
  }

  @Test
  public void status_YELLOW_when_one_GREEN_one_YELLOW_and_two_RED_search_nodes() {
    Set<NodeHealth> nodeHealths = nodeHealths(GREEN, YELLOW, RED, RED).collect(toSet());

    Health check = underTest.check(nodeHealths);

    assertThat(check)
      .forInput(nodeHealths)
      .hasStatus(Health.Status.YELLOW)
      .andCauses("There should be an odd number of search nodes",
        "At least one search node is RED",
        "At least one search node is YELLOW");
  }

  @Test
  public void status_YELLOW_when_three_YELLOW_and_one_GREEN_search_nodes() {
    Set<NodeHealth> nodeHealths = nodeHealths(YELLOW, YELLOW, YELLOW, GREEN).collect(toSet());

    Health check = underTest.check(nodeHealths);

    assertThat(check)
      .forInput(nodeHealths)
      .hasStatus(Health.Status.YELLOW)
      .andCauses("There should be an odd number of search nodes",
        "At least one search node is YELLOW");
  }

  @Test
  public void status_YELLOW_when_three_YELLOW_and_one_RED_search_nodes() {
    Set<NodeHealth> nodeHealths = nodeHealths(YELLOW, YELLOW, YELLOW, RED).collect(toSet());

    Health check = underTest.check(nodeHealths);

    assertThat(check)
      .forInput(nodeHealths)
      .hasStatus(Health.Status.YELLOW)
      .andCauses("There should be an odd number of search nodes",
        "At least one search node is RED",
        "At least one search node is YELLOW");
  }

  @Test
  public void status_GREEN_when_three_GREEN_search_nodes_and_even_number_of_GREEN() {
    Set<NodeHealth> nodeHealths = of(
      // 0, 2, 4 or 6 GREEN
      evenNumberOfAppNodeHealthOfAnyStatus(GREEN),
      // 3 GREEN
      nodeHealths(GREEN, GREEN, GREEN))
        .flatMap(s -> s)
        .collect(toSet());

    Health check = underTest.check(nodeHealths);

    assertThat(check)
      .forInput(nodeHealths)
      .hasStatus(Health.Status.GREEN)
      .andCauses();
  }

  @Test
  public void status_YELLOW_when_three_GREEN_search_nodes_and_even_number_of_GREEN_and_YELLOW() {
    Set<NodeHealth> nodeHealths = of(
      of(searchNodeHealth(GREEN), searchNodeHealth(YELLOW)),
      // 0, 2, 4 or 6 GREEN
      evenNumberOfAppNodeHealthOfAnyStatus(GREEN),
      // 3 GREEN
      nodeHealths(GREEN, GREEN, GREEN))
        .flatMap(s -> s)
        .collect(toSet());

    Health check = underTest.check(nodeHealths);

    assertThat(check)
      .forInput(nodeHealths)
      .hasStatus(Health.Status.YELLOW)
      .andCauses("At least one search node is YELLOW");
  }

  @Test
  public void status_YELLOW_when_three_GREEN_search_nodes_and_even_number_of_GREEN_and_RED() {
    Set<NodeHealth> nodeHealths = of(
      // at least one GREEN and one RED
      of(searchNodeHealth(GREEN), searchNodeHealth(RED)),
      // 0, 2, 4 or 6 GREEN or RED
      evenNumberOfAppNodeHealthOfAnyStatus(GREEN, RED),
      // 3 GREEN
      nodeHealths(GREEN, GREEN, GREEN))
        .flatMap(s -> s)
        .collect(toSet());

    Health check = underTest.check(nodeHealths);

    assertThat(check)
      .forInput(nodeHealths)
      .hasStatus(Health.Status.YELLOW)
      .andCauses("At least one search node is RED");
  }

  @Test
  public void status_YELLOW_when_three_GREEN_search_nodes_and_even_number_of_YELLOW_and_RED() {
    Set<NodeHealth> nodeHealths = of(
      // at least one YELLOW and one RED
      of(searchNodeHealth(YELLOW), searchNodeHealth(RED)),
      // 0, 2, 4 or 6 GREEN, YELLOW or RED
      evenNumberOfAppNodeHealthOfAnyStatus(YELLOW, RED, GREEN),
      // 3 GREEN
      nodeHealths(GREEN, GREEN, GREEN))
        .flatMap(s -> s)
        .collect(toSet());

    Health check = underTest.check(nodeHealths);

    assertThat(check)
      .forInput(nodeHealths)
      .hasStatus(Health.Status.YELLOW)
      .andCauses("At least one search node is RED",
        "At least one search node is YELLOW");
  }

  @Test
  public void status_YELLOW_when_three_GREEN_search_nodes_and_odd_number_of_GREEN() {
    Set<NodeHealth> nodeHealths = of(
      // 1, 3, 5, or 7 GREEN
      oddNumberOfAppNodeHealthOfAnyStatus(GREEN),
      // 3 GREEN
      nodeHealths(GREEN, GREEN, GREEN))
        .flatMap(s -> s)
        .collect(toSet());

    Health check = underTest.check(nodeHealths);

    assertThat(check)
      .forInput(nodeHealths)
      .hasStatus(Health.Status.YELLOW)
      .andCauses("There should be an odd number of search nodes");
  }

  @Test
  public void status_YELLOW_when_three_GREEN_search_nodes_and_odd_number_of_YELLOW() {
    Set<NodeHealth> nodeHealths = of(
      // 1, 3, 5, or 7 YELLOW
      oddNumberOfAppNodeHealthOfAnyStatus(YELLOW),
      // 3 GREEN
      nodeHealths(GREEN, GREEN, GREEN))
        .flatMap(s -> s)
        .collect(toSet());

    Health check = underTest.check(nodeHealths);

    assertThat(check)
      .forInput(nodeHealths)
      .hasStatus(Health.Status.YELLOW)
      .andCauses("There should be an odd number of search nodes",
        "At least one search node is YELLOW");
  }

  @Test
  public void status_YELLOW_when_three_GREEN_search_nodes_and_odd_number_of_RED() {
    Set<NodeHealth> nodeHealths = of(
      // 1, 3, 5, or 7 RED
      oddNumberOfAppNodeHealthOfAnyStatus(RED),
      // 3 GREEN
      nodeHealths(GREEN, GREEN, GREEN))
        .flatMap(s -> s)
        .collect(toSet());

    Health check = underTest.check(nodeHealths);

    assertThat(check)
      .forInput(nodeHealths)
      .hasStatus(Health.Status.YELLOW)
      .andCauses("There should be an odd number of search nodes",
        "At least one search node is RED");
  }

  @Test
  public void status_YELLOW_when_three_GREEN_search_nodes_and_odd_number_of_RED_and_YELLOW() {
    Set<NodeHealth> nodeHealths = of(
      // at least one YELLOW and one RED
      of(searchNodeHealth(YELLOW), searchNodeHealth(RED)),
      // 1, 3, 5, or 7 GREEN RED or YELLOW
      oddNumberOfAppNodeHealthOfAnyStatus(RED, YELLOW, GREEN),
      // 3 GREEN
      nodeHealths(GREEN, GREEN, GREEN))
        .flatMap(s -> s)
        .collect(toSet());

    Health check = underTest.check(nodeHealths);

    assertThat(check)
      .forInput(nodeHealths)
      .hasStatus(Health.Status.YELLOW)
      .andCauses("There should be an odd number of search nodes",
        "At least one search node is RED",
        "At least one search node is YELLOW");
  }

  private Stream<NodeHealth> nodeHealths(NodeHealth.Status... searchNodeStatuses) {
    return of(
      // random number of Application nodes with random status
      IntStream.range(0, random.nextInt(3))
        .mapToObj(i -> nodeHealth(NodeDetails.Type.APPLICATION, NodeHealth.Status.values()[random.nextInt(NodeHealth.Status.values().length)])),
      Arrays.stream(searchNodeStatuses).map(this::searchNodeHealth))
        .flatMap(s -> s);
  }

  private NodeHealth searchNodeHealth(NodeHealth.Status status) {
    return nodeHealth(NodeDetails.Type.SEARCH, status);
  }

  private NodeHealth nodeHealth(NodeDetails.Type type, NodeHealth.Status status) {
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

  /**
   * Between 0, 2, 4 or 6 NodeHealth of Application node with any of the specified statuses.
   */
  private Stream<NodeHealth> evenNumberOfAppNodeHealthOfAnyStatus(NodeHealth.Status... randomStatuses) {
    return IntStream.range(0, 2 * random.nextInt(3))
      .mapToObj(i -> searchNodeHealth(randomStatuses[random.nextInt(randomStatuses.length)]));
  }

  /**
   * Between 1, 3, 5, or 7 NodeHealth of Application node with any of the specified statuses.
   */
  private Stream<NodeHealth> oddNumberOfAppNodeHealthOfAnyStatus(NodeHealth.Status... randomStatuses) {
    return IntStream.range(0, 2 * random.nextInt(3) + 1)
      .mapToObj(i -> searchNodeHealth(randomStatuses[random.nextInt(randomStatuses.length)]));
  }

}
