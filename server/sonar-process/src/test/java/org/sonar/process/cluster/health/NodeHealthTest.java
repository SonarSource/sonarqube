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
package org.sonar.process.cluster.health;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.process.cluster.health.NodeHealth.newNodeHealthBuilder;

public class NodeHealthTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private Random random = new Random();
  private NodeDetailsTestSupport testSupport = new NodeDetailsTestSupport(random);
  private NodeHealth.Status randomStatus = testSupport.randomStatus();
  private NodeHealth.Builder builderUnderTest = newNodeHealthBuilder();

  @Test
  public void setStatus_throws_NPE_if_arg_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("status can't be null");

    builderUnderTest.setStatus(null);
  }

  @Test
  public void setDetails_throws_NPE_if_arg_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("details can't be null");

    builderUnderTest.setDetails(null);
  }

  @Test
  public void build_throws_NPE_if_status_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("status can't be null");

    builderUnderTest.build();
  }

  @Test
  public void build_throws_NPE_if_details_is_null() {
    builderUnderTest.setStatus(randomStatus);

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("details can't be null");

    builderUnderTest.build();
  }

  @Test
  public void clearClauses_clears_clauses_of_builder() {
    NodeHealth.Builder underTest = testSupport.randomBuilder();
    NodeHealth original = underTest
      .addCause(randomAlphanumeric(3))
      .build();

    underTest.clearCauses();

    NodeHealth second = underTest.build();
    assertThat(second.getStatus()).isEqualTo(original.getStatus());
    assertThat(second.getDetails()).isEqualTo(original.getDetails());
    assertThat(second.getCauses()).isEmpty();
  }

  @Test
  public void builder_can_be_reused() {
    NodeHealth.Builder builder = testSupport.randomBuilder(1);
    NodeHealth original = builder.build();
    NodeHealth second = builder.build();

    NodeHealth.Status newRandomStatus = NodeHealth.Status.values()[random.nextInt(NodeHealth.Status.values().length)];
    NodeDetails newNodeDetails = testSupport.randomNodeDetails();
    builder
      .clearCauses()
      .setStatus(newRandomStatus)
      .setDetails(newNodeDetails);
    String[] newCauses = IntStream.range(0, 1 + random.nextInt(2)).mapToObj(i -> randomAlphanumeric(4)).toArray(String[]::new);
    Arrays.stream(newCauses).forEach(builder::addCause);

    NodeHealth newNodeHealth = builder.build();

    assertThat(second).isEqualTo(original);
    assertThat(newNodeHealth.getStatus()).isEqualTo(newRandomStatus);
    assertThat(newNodeHealth.getDetails()).isEqualTo(newNodeDetails);
    assertThat(newNodeHealth.getCauses()).containsOnly(newCauses);
  }

  @Test
  public void equals_is_based_on_content() {
    NodeHealth.Builder builder = testSupport.randomBuilder();

    NodeHealth underTest = builder.build();

    assertThat(underTest).isEqualTo(underTest);
    assertThat(builder.build())
      .isEqualTo(underTest)
      .isNotSameAs(underTest);
    assertThat(underTest).isNotEqualTo(null);
    assertThat(underTest).isNotEqualTo(new Object());
  }

  @Test
  public void hashcode_is_based_on_content() {
    NodeHealth.Builder builder = testSupport.randomBuilder();

    NodeHealth underTest = builder.build();

    assertThat(builder.build().hashCode())
      .isEqualTo(underTest.hashCode());
  }

  @Test
  public void class_is_serializable_with_causes() throws IOException, ClassNotFoundException {
    NodeHealth source = testSupport.randomBuilder(1).build();
    byte[] bytes = NodeDetailsTestSupport.serialize(source);

    NodeHealth underTest = (NodeHealth) new ObjectInputStream(new ByteArrayInputStream(bytes)).readObject();

    assertThat(underTest).isEqualTo(source);
  }

  @Test
  public void class_is_serializable_without_causes() throws IOException, ClassNotFoundException {
    NodeHealth.Builder builder = newNodeHealthBuilder()
      .setStatus(randomStatus)
      .setDetails(testSupport.randomNodeDetails());
    NodeHealth source = builder.build();
    byte[] bytes = NodeDetailsTestSupport.serialize(source);

    NodeHealth underTest = (NodeHealth) new ObjectInputStream(new ByteArrayInputStream(bytes)).readObject();

    assertThat(underTest).isEqualTo(source);
  }

  @Test
  public void verify_toString() {
    NodeDetails nodeDetails = testSupport.randomNodeDetails();
    String cause = randomAlphanumeric(4);
    NodeHealth.Builder builder = builderUnderTest
      .setStatus(randomStatus)
      .setDetails(nodeDetails)
      .addCause(cause);

    NodeHealth underTest = builder.build();

    assertThat(underTest.toString())
      .isEqualTo("NodeHealth{status=" + randomStatus + ", causes=[" + cause + "], details=" + nodeDetails + "}");
  }

  @Test
  public void verify_getters() {
    NodeDetails nodeDetails = testSupport.randomNodeDetails();
    NodeHealth.Builder builder = builderUnderTest
      .setStatus(randomStatus)
      .setDetails(nodeDetails);
    String[] causes = IntStream.range(0, random.nextInt(10)).mapToObj(i -> randomAlphanumeric(4)).toArray(String[]::new);
    Arrays.stream(causes).forEach(builder::addCause);

    NodeHealth underTest = builder.build();

    assertThat(underTest.getStatus()).isEqualTo(randomStatus);
    assertThat(underTest.getDetails()).isEqualTo(nodeDetails);
    assertThat(underTest.getCauses()).containsOnly(causes);
  }

}
