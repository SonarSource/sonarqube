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
package org.sonar.server.health;

import java.util.List;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.cluster.health.NodeHealth;
import org.sonar.cluster.health.SharedHealthState;
import org.sonar.server.platform.WebServer;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.copyOf;
import static org.sonar.server.health.Health.newHealthCheckBuilder;

/**
 * Implementation of {@link HealthChecker} based on {@link NodeHealthCheck} and {@link ClusterHealthCheck} instances
 * available in the container.
 */
public class HealthCheckerImpl implements HealthChecker {
  private final WebServer webServer;
  private final List<NodeHealthCheck> nodeHealthChecks;
  private final List<ClusterHealthCheck> clusterHealthChecks;
  @CheckForNull
  private final SharedHealthState sharedHealthState;

  /**
   * Constructor used by Pico in standalone mode and in safe mode.
   */
  public HealthCheckerImpl(WebServer webServer, NodeHealthCheck[] nodeHealthChecks) {
    this(webServer, nodeHealthChecks, new ClusterHealthCheck[0], null);
  }

  /**
   * Constructor used by Pico in cluster mode.
   */
  public HealthCheckerImpl(WebServer webServer, NodeHealthCheck[] nodeHealthChecks, ClusterHealthCheck[] clusterHealthChecks,
    @Nullable SharedHealthState sharedHealthState) {
    this.webServer = webServer;
    this.nodeHealthChecks = copyOf(nodeHealthChecks);
    this.clusterHealthChecks = copyOf(clusterHealthChecks);
    this.sharedHealthState = sharedHealthState;
  }

  @Override
  public Health checkNode() {
    return nodeHealthChecks.stream()
      .map(NodeHealthCheck::check)
      .reduce(Health.GREEN, HealthReducer.INSTANCE);
  }

  @Override
  public Health checkCluster() {
    checkState(!webServer.isStandalone(), "Clustering is not enabled");
    checkState(sharedHealthState != null, "HealthState instance can't be null when clustering is enabled");

    Set<NodeHealth> nodeHealths = sharedHealthState.readAll();
    return clusterHealthChecks.stream()
      .map(clusterHealthCheck -> clusterHealthCheck.check(nodeHealths))
      .reduce(Health.GREEN, HealthReducer.INSTANCE);
  }

  private enum HealthReducer implements BinaryOperator<Health> {
    INSTANCE;

    /**
     * According to Javadoc, {@link BinaryOperator} used in method
     * {@link java.util.stream.Stream#reduce(Object, BinaryOperator)} is supposed to be stateless.
     *
     * But as we are sure this {@link BinaryOperator} won't be used on a Stream with {@link Stream#parallel()}
     * feature on, we allow ourselves this optimisation.
     */
    private final Health.Builder builder = newHealthCheckBuilder();

    @Override
    public Health apply(Health left, Health right) {
      builder.clear();
      builder.setStatus(worseOf(left.getStatus(), right.getStatus()));
      left.getCauses().forEach(builder::addCause);
      right.getCauses().forEach(builder::addCause);
      return builder.build();
    }

    private static Health.Status worseOf(Health.Status left, Health.Status right) {
      if (left == right) {
        return left;
      }
      if (left.ordinal() > right.ordinal()) {
        return left;
      }
      return right;
    }
  }
}
