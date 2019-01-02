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

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sonar.process.cluster.health.NodeHealth;

import static com.google.common.collect.ImmutableSet.copyOf;
import static java.util.Objects.requireNonNull;

public class ClusterHealth {
  private final Health health;
  private final Set<NodeHealth> nodes;

  public ClusterHealth(Health health, Set<NodeHealth> nodes) {
    this.health = requireNonNull(health, "health can't be null");
    this.nodes = copyOf(requireNonNull(nodes, "nodes can't be null"));
  }

  public Health getHealth() {
    return health;
  }

  public Set<NodeHealth> getNodes() {
    return nodes;
  }

  public Optional<NodeHealth> getNodeHealth(String nodeName) {
    return nodes.stream()
      .filter(node -> nodeName.equals(node.getDetails().getName()))
      .findFirst();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ClusterHealth that = (ClusterHealth) o;
    return Objects.equals(health, that.health) &&
      Objects.equals(nodes, that.nodes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(health, nodes);
  }

  @Override
  public String toString() {
    return "ClusterHealth{" +
      "health=" + health +
      ", nodes=" + nodes +
      '}';
  }
}
