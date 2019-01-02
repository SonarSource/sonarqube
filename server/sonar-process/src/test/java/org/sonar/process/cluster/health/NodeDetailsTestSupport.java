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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Random;
import java.util.stream.IntStream;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.sonar.process.cluster.health.NodeDetails.newNodeDetailsBuilder;
import static org.sonar.process.cluster.health.NodeHealth.newNodeHealthBuilder;

public class NodeDetailsTestSupport {
  private final Random random;

  public NodeDetailsTestSupport() {
    this(new Random());
  }

  NodeDetailsTestSupport(Random random) {
    this.random = random;
  }

  NodeHealth.Status randomStatus() {
    return NodeHealth.Status.values()[random.nextInt(NodeHealth.Status.values().length)];
  }

  NodeHealth randomNodeHealth() {
    return randomBuilder().build();
  }

  NodeHealth.Builder randomBuilder() {
    return randomBuilder(0);
  }

  NodeHealth.Builder randomBuilder(int minCauseCount) {
    NodeHealth.Builder builder = newNodeHealthBuilder()
      .setStatus(randomStatus())
      .setDetails(randomNodeDetails());
    IntStream.range(0, minCauseCount + random.nextInt(2)).mapToObj(i -> randomAlphanumeric(4)).forEach(builder::addCause);
    return builder;
  }

  NodeDetails randomNodeDetails() {
    return randomNodeDetailsBuilder()
      .build();
  }

  NodeDetails.Builder randomNodeDetailsBuilder() {
    return newNodeDetailsBuilder()
      .setType(randomType())
      .setName(randomAlphanumeric(3))
      .setHost(randomAlphanumeric(10))
      .setPort(1 + random.nextInt(10))
      .setStartedAt(1 + random.nextInt(666));
  }

  NodeDetails.Type randomType() {
    return NodeDetails.Type.values()[random.nextInt(NodeDetails.Type.values().length)];
  }

  static byte[] serialize(Object source) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(out)) {
      objectOutputStream.writeObject(source);
    }
    return out.toByteArray();
  }
}
