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
package org.sonar.server.health;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.Test;
import org.sonar.process.cluster.health.NodeDetails;
import org.sonar.process.cluster.health.NodeHealth;

import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.concat;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.process.cluster.health.NodeHealth.newNodeHealthBuilder;

public class ClusterHealthTest {

  private final Random random = new Random();

  @Test
  public void constructor_fails_with_NPE_if_Health_is_null() {
    assertThatThrownBy(() -> new ClusterHealth(null, Collections.emptySet()))
      .isInstanceOf(NullPointerException.class)
      .hasMessageContaining("health can't be null");
  }

  @Test
  public void constructor_fails_with_NPE_if_NodeHealth_is_null() {
    assertThatThrownBy(() -> new ClusterHealth(Health.GREEN, null))
      .isInstanceOf(NullPointerException.class)
      .hasMessageContaining("nodes can't be null");
  }

  @Test
  public void verify_getters() {
    Health health = randomHealth();
    Set<NodeHealth> nodeHealths = randomNodeHealths();
    ClusterHealth underTest = new ClusterHealth(health, nodeHealths);

    assertThat(underTest.getHealth()).isSameAs(health);
    assertThat(underTest.getNodes()).isEqualTo(nodeHealths);
  }

  @Test
  public void equals_is_based_on_content() {
    Health health = randomHealth();
    Set<NodeHealth> nodeHealths = randomNodeHealths();
    ClusterHealth underTest = new ClusterHealth(health, nodeHealths);

    assertThat(underTest)
      .isEqualTo(underTest)
      .isEqualTo(new ClusterHealth(health, nodeHealths))
      .isNotEqualTo(new Object())
      .isNotNull()
      .isNotEqualTo(new ClusterHealth(
        Health.builder()
          .setStatus(health.getStatus())
          .addCause("foo_bar")
          .build(),
        randomNodeHealths()))
      .isNotEqualTo(new ClusterHealth(
        health,
        concat(nodeHealths.stream(), Stream.of(randomNodeHealth())).collect(toSet())));
  }

  @Test
  public void hashcode_is_based_on_content() {
    Health health = randomHealth();
    Set<NodeHealth> nodeHealths = randomNodeHealths();
    ClusterHealth underTest = new ClusterHealth(health, nodeHealths);

    assertThat(underTest).hasSameHashCodeAs(underTest);
  }

  @Test
  public void verify_toString() {
    Health health = randomHealth();
    Set<NodeHealth> nodeHealths = randomNodeHealths();

    ClusterHealth underTest = new ClusterHealth(health, nodeHealths);

    assertThat(underTest).hasToString("ClusterHealth{health=" + health + ", nodes=" + nodeHealths + "}");
  }

  @Test
  public void test_getNodeHealth() {
    Health health = randomHealth();
    Set<NodeHealth> nodeHealths = new HashSet<>(Arrays.asList(newNodeHealth("foo"), newNodeHealth("bar")));

    ClusterHealth underTest = new ClusterHealth(health, nodeHealths);

    assertThat(underTest.getNodeHealth("does_not_exist")).isEmpty();
    assertThat(underTest.getNodeHealth("bar")).isPresent();
  }

  private Health randomHealth() {
    Health.Builder healthBuilder = Health.builder();
    healthBuilder.setStatus(Health.Status.values()[random.nextInt(Health.Status.values().length)]);
    IntStream.range(0, random.nextInt(3)).mapToObj(i -> randomAlphanumeric(3)).forEach(healthBuilder::addCause);
    return healthBuilder.build();
  }

  private Set<NodeHealth> randomNodeHealths() {
    return IntStream.range(0, random.nextInt(4)).mapToObj(i -> randomNodeHealth()).collect(toSet());
  }

  private NodeHealth randomNodeHealth() {
    return newNodeHealthBuilder()
      .setStatus(NodeHealth.Status.values()[random.nextInt(NodeHealth.Status.values().length)])
      .setDetails(
        NodeDetails.newNodeDetailsBuilder()
          .setType(random.nextBoolean() ? NodeDetails.Type.SEARCH : NodeDetails.Type.APPLICATION)
          .setName(randomAlphanumeric(3))
          .setHost(randomAlphanumeric(4))
          .setPort(1 + random.nextInt(344))
          .setStartedAt(1 + random.nextInt(999))
          .build())
      .build();
  }

  private static NodeHealth newNodeHealth(String nodeName) {
    return newNodeHealthBuilder()
      .setStatus(NodeHealth.Status.YELLOW)
      .setDetails(randomNodeDetails(nodeName))
      .build();
  }

  private static NodeDetails randomNodeDetails(String nodeName) {
    return NodeDetails.newNodeDetailsBuilder()
      .setType(NodeDetails.Type.APPLICATION)
      .setName(nodeName)
      .setHost(randomAlphanumeric(4))
      .setPort(3000)
      .setStartedAt(1_000L)
      .build();
  }
}
