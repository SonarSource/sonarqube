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
package org.sonar.application.cluster;

import com.google.common.annotations.VisibleForTesting;
import com.hazelcast.cluster.memberselector.MemberSelectors;
import com.hazelcast.core.Member;
import com.hazelcast.core.MemberSelector;
import com.hazelcast.nio.Address;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.annotation.CheckForNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.application.config.AppSettings;
import org.sonar.process.ProcessId;
import org.sonar.process.cluster.hz.DistributedCallback;
import org.sonar.process.cluster.hz.HazelcastMember;
import org.sonar.process.cluster.hz.HazelcastMemberSelectors;

import static com.google.common.base.Preconditions.checkState;
import static com.hazelcast.cluster.memberselector.MemberSelectors.NON_LOCAL_MEMBER_SELECTOR;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_HZ_HOSTS;

public class AppNodesClusterHostsConsistency {
  private static final Logger LOG = LoggerFactory.getLogger(AppNodesClusterHostsConsistency.class);

  private static final AtomicReference<AppNodesClusterHostsConsistency> INSTANCE = new AtomicReference<>();

  private final AppSettings settings;
  private final HazelcastMember hzMember;
  private final Consumer<String> logger;

  private AppNodesClusterHostsConsistency(HazelcastMember hzMember, AppSettings settings, Consumer<String> logger) {
    this.hzMember = hzMember;
    this.settings = settings;
    this.logger = logger;
  }

  public static AppNodesClusterHostsConsistency setInstance(HazelcastMember hzMember, AppSettings settings) {
    return setInstance(hzMember, settings, LOG::warn);
  }

  @VisibleForTesting
  public static AppNodesClusterHostsConsistency setInstance(HazelcastMember hzMember, AppSettings settings, Consumer<String> logger) {
    AppNodesClusterHostsConsistency instance = new AppNodesClusterHostsConsistency(hzMember, settings, logger);
    checkState(INSTANCE.compareAndSet(null, instance), "Instance is already set");
    return instance;
  }

  @VisibleForTesting
  @CheckForNull
  protected static AppNodesClusterHostsConsistency clearInstance() {
    return INSTANCE.getAndSet(null);
  }

  public void check() {
    try {
      MemberSelector selector = MemberSelectors.and(NON_LOCAL_MEMBER_SELECTOR, HazelcastMemberSelectors.selectorForProcessIds(ProcessId.APP));
      hzMember.callAsync(AppNodesClusterHostsConsistency::getConfiguredClusterHosts, selector, new Callback());
    } catch (RejectedExecutionException e) {
      // no other node in the cluster yet, ignore
    }
  }

  private class Callback implements DistributedCallback<List<String>> {
    @Override
    public void onComplete(Map<Member, List<String>> hostsPerMember) {
      List<String> currentConfiguredHosts = getConfiguredClusterHosts();

      boolean anyDifference = hostsPerMember.values().stream()
        .filter(v -> !v.isEmpty())
        .anyMatch(hosts -> currentConfiguredHosts.size() != hosts.size() || !currentConfiguredHosts.containsAll(hosts));

      if (anyDifference) {
        StringBuilder builder = new StringBuilder().append("The configuration of the current node doesn't match the list of hosts configured in "
          + "the application nodes that have already joined the cluster:\n");
        logMemberSetting(builder, hzMember.getCluster().getLocalMember(), currentConfiguredHosts);

        for (Map.Entry<Member, List<String>> e : hostsPerMember.entrySet()) {
          if (e.getValue().isEmpty()) {
            continue;
          }
          logMemberSetting(builder, e.getKey(), e.getValue());
        }
        builder.append("Make sure the configuration is consistent among all application nodes before you restart any node");
        logger.accept(builder.toString());
      }
    }

    private String toString(Address address) {
      return address.getHost() + ":" + address.getPort();
    }

    private void logMemberSetting(StringBuilder builder, Member member, List<String> configuredHosts) {
      builder.append(toString(member.getAddress()));
      builder.append(" : ");
      builder.append(configuredHosts);
      if (member.localMember()) {
        builder.append(" (current)");
      }
      builder.append("\n");
    }
  }

  private static List<String> getConfiguredClusterHosts() {
    try {
      AppNodesClusterHostsConsistency instance = INSTANCE.get();
      if (instance != null) {
        return Arrays.asList(instance.settings.getProps().nonNullValue(CLUSTER_HZ_HOSTS.getKey()).split(","));
      }
      return Collections.emptyList();
    } catch (Exception e) {
      LOG.error("Failed to get configured cluster nodes", e);
      return Collections.emptyList();
    }
  }

}
