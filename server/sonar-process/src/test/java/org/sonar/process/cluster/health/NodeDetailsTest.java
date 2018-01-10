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
import java.util.Random;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.process.cluster.health.NodeDetails.newNodeDetailsBuilder;

public class NodeDetailsTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private Random random = new Random();
  private NodeDetailsTestSupport testSupport = new NodeDetailsTestSupport(random);
  private NodeDetails.Type randomType = testSupport.randomType();
  private NodeDetails.Builder builderUnderTest = newNodeDetailsBuilder();

  @Test
  public void setType_throws_NPE_if_arg_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("type can't be null");

    builderUnderTest.setType(null);
  }

  @Test
  public void setName_throws_NPE_if_arg_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("name can't be null");

    builderUnderTest.setName(null);
  }

  @Test
  public void setName_throws_IAE_if_arg_is_empty() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("name can't be empty");

    builderUnderTest.setName("");
  }

  @Test
  public void setName_throws_IAE_if_arg_is_empty_after_trim() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("name can't be empty");

    builderUnderTest.setName("  ");
  }

  @Test
  public void setHost_throws_NPE_if_arg_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("host can't be null");

    builderUnderTest.setHost(null);
  }

  @Test
  public void setHost_throws_IAE_if_arg_is_empty() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("host can't be empty");

    builderUnderTest.setHost("");
  }

  @Test
  public void setHost_throws_IAE_if_arg_is_empty_after_trim() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("host can't be empty");

    builderUnderTest.setHost("  ");
  }

  @Test
  public void setPort_throws_IAE_if_arg_is_less_than_1() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("port must be > 0");

    builderUnderTest.setPort(-random.nextInt(5));
  }

  @Test
  public void setStarted_throws_IAE_if_arg_is_less_than_1() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("startedAt must be > 0");

    builderUnderTest.setStartedAt(-random.nextInt(5));
  }

  @Test
  public void build_throws_NPE_if_type_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("type can't be null");

    builderUnderTest.build();
  }

  @Test
  public void build_throws_NPE_if_name_is_null() {
    builderUnderTest
      .setType(randomType);

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("name can't be null");

    builderUnderTest.build();
  }

  @Test
  public void build_throws_NPE_if_host_is_null() {
    builderUnderTest
      .setType(randomType)
      .setName(randomAlphanumeric(2));

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("host can't be null");

    builderUnderTest.build();
  }

  @Test
  public void build_throws_IAE_if_setPort_not_called() {
    builderUnderTest
      .setType(randomType)
      .setName(randomAlphanumeric(2))
      .setHost(randomAlphanumeric(3));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("port must be > 0");

    builderUnderTest.build();
  }

  @Test
  public void build_throws_IAE_if_setStarted_not_called() {
    builderUnderTest
      .setType(randomType)
      .setName(randomAlphanumeric(2))
      .setHost(randomAlphanumeric(3))
      .setPort(1 + random.nextInt(33));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("startedAt must be > 0");

    builderUnderTest.build();
  }

  @Test
  public void equals_is_based_on_content() {
    NodeDetails.Builder builder = testSupport.randomNodeDetailsBuilder();

    NodeDetails underTest = builder.build();

    assertThat(underTest).isEqualTo(underTest);
    assertThat(builder.build())
      .isEqualTo(underTest)
      .isNotSameAs(underTest);
    assertThat(underTest).isNotEqualTo(null);
    assertThat(underTest).isNotEqualTo(new Object());
  }

  @Test
  public void hashcode_is_based_on_content() {
    NodeDetails.Builder builder = testSupport.randomNodeDetailsBuilder();

    NodeDetails underTest = builder.build();

    assertThat(builder.build().hashCode())
      .isEqualTo(underTest.hashCode());
  }

  @Test
  public void NodeDetails_is_Externalizable() throws IOException, ClassNotFoundException {
    NodeDetails source = testSupport.randomNodeDetails();
    byte[] byteArray = NodeDetailsTestSupport.serialize(source);

    NodeDetails underTest = (NodeDetails) new ObjectInputStream(new ByteArrayInputStream(byteArray)).readObject();

    assertThat(underTest).isEqualTo(source);
  }

  @Test
  public void verify_toString() {
    String name = randomAlphanumeric(3);
    String host = randomAlphanumeric(10);
    int port = 1 + random.nextInt(10);
    long startedAt = 1 + random.nextInt(666);

    NodeDetails underTest = builderUnderTest
      .setType(randomType)
      .setName(name)
      .setHost(host)
      .setPort(port)
      .setStartedAt(startedAt)
      .build();

    assertThat(underTest.toString())
      .isEqualTo("NodeDetails{type=" + randomType + ", name='" + name + "', host='" + host + "', port=" + port + ", startedAt=" + startedAt + "}");
  }

  @Test
  public void verify_getters() {
    String name = randomAlphanumeric(3);
    String host = randomAlphanumeric(10);
    int port = 1 + random.nextInt(10);
    long startedAt = 1 + random.nextInt(666);

    NodeDetails underTest = builderUnderTest
      .setType(randomType)
      .setName(name)
      .setHost(host)
      .setPort(port)
      .setStartedAt(startedAt)
      .build();

    assertThat(underTest.getType()).isEqualTo(randomType);
    assertThat(underTest.getName()).isEqualTo(name);
    assertThat(underTest.getHost()).isEqualTo(host);
    assertThat(underTest.getPort()).isEqualTo(port);
    assertThat(underTest.getStartedAt()).isEqualTo(startedAt);
  }
}
