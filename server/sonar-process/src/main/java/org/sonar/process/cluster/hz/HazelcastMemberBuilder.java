/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.process.cluster.hz;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.MemberAttributeConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.internal.util.AddressUtil;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.process.ProcessId;
import org.sonar.process.cluster.hz.HazelcastMember.Attribute;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NODE_HZ_PORT;

public class HazelcastMemberBuilder {

  private static final Logger LOG = Loggers.get(HazelcastMemberBuilder.class);
  private String nodeName;
  private int port;
  private ProcessId processId;
  private String networkInterface;
  private List<String> members = new ArrayList<>();
  private final InetAdressResolver inetAdressResolver;

  public HazelcastMemberBuilder(InetAdressResolver inetAdressResolver) {
    this.inetAdressResolver = inetAdressResolver;
  }

  public HazelcastMemberBuilder setNodeName(String s) {
    this.nodeName = s;
    return this;
  }

  public HazelcastMemberBuilder setProcessId(ProcessId p) {
    if (p == ProcessId.ELASTICSEARCH) {
      throw new IllegalArgumentException("Hazelcast must not be enabled on Elasticsearch node");
    }
    this.processId = p;
    return this;
  }

  public HazelcastMemberBuilder setPort(int i) {
    this.port = i;
    return this;
  }

  public HazelcastMemberBuilder setNetworkInterface(String s) {
    this.networkInterface = s;
    return this;
  }

  @CheckForNull
  List<String> getMembers() {
    return members;
  }

  /**
   * Adds references to cluster members. If port is missing, then default
   * port is automatically added.
   */
  public HazelcastMemberBuilder setMembers(Collection<String> c) {
    this.members.addAll(c.stream().map(this::extractMembers).flatMap(Collection::stream).collect(Collectors.toList()));
    return this;
  }

  private List<String> extractMembers(String host) {
    LOG.debug("Trying to add host: " + host);
    String hostStripped = host.split(":")[0];
    if (AddressUtil.isIpAddress(hostStripped)) {
      LOG.debug("Found ip based host config for host: " + host);
      return Collections.singletonList(host.contains(":") ? host : format("%s:%s", host, CLUSTER_NODE_HZ_PORT.getDefaultValue()));
    } else {
      List<String> membersToAdd = new ArrayList<>();
      for (String memberIp : getAllByName(hostStripped)) {
        String prefix = memberIp.split("/")[1];
        LOG.debug("Found IP for: " + hostStripped + " : " + prefix);
        String memberPort = host.contains(":") ? host.split(":")[1] : CLUSTER_NODE_HZ_PORT.getDefaultValue();
        String member = prefix + ":" + memberPort;
        membersToAdd.add(member);
      }
      return membersToAdd;
    }
  }

  List<String> getAllByName(String hostname) {
    LOG.debug("Trying to resolve Hostname: " + hostname);
    try {
      return inetAdressResolver.getAllByName(hostname);
    } catch (UnknownHostException e) {
      LOG.error("Host could not be found\n" + e.getMessage());
    }
    return new ArrayList<>();
  }

  public HazelcastMember build() {
    Config config = new Config();
    // do not use the value defined by property sonar.cluster.name.
    // Hazelcast does not fail when joining a cluster with different name.
    // Apparently this behavior exists since Hazelcast 3.8.2 (see note
    // at http://docs.hazelcast.org/docs/3.8.6/manual/html-single/index.html#creating-cluster-groups)
    config.setClusterName("SonarQube");

    // Configure network
    NetworkConfig netConfig = config.getNetworkConfig();
    netConfig
      .setPort(port)
      .setPortAutoIncrement(false)
      .setReuseAddress(true);
    netConfig.getInterfaces()
      .setEnabled(true)
      .setInterfaces(singletonList(requireNonNull(networkInterface, "Network interface is missing")));

    // Only allowing TCP/IP configuration
    JoinConfig joinConfig = netConfig.getJoin();
    joinConfig.getAwsConfig().setEnabled(false);
    joinConfig.getMulticastConfig().setEnabled(false);
    joinConfig.getTcpIpConfig().setEnabled(true);
    joinConfig.getTcpIpConfig().setMembers(requireNonNull(members, "Members are missing"));
    // We are not using the partition group of Hazelcast, so disabling it
    config.getPartitionGroupConfig().setEnabled(false);

    // Tweak HazelCast configuration
    config
      // Increase the number of tries
      .setProperty("hazelcast.tcp.join.port.try.count", "10")
      // Don't bind on all interfaces
      .setProperty("hazelcast.socket.bind.any", "false")
      // Don't phone home
      .setProperty("hazelcast.phone.home.enabled", "false")
      // Use slf4j for logging
      .setProperty("hazelcast.logging.type", "slf4j");

    MemberAttributeConfig attributes = config.getMemberAttributeConfig();
    attributes.setAttribute(Attribute.NODE_NAME.getKey(), requireNonNull(nodeName, "Node name is missing"));
    attributes.setAttribute(Attribute.PROCESS_KEY.getKey(), requireNonNull(processId, "Process key is missing").getKey());

    return new HazelcastMemberImpl(Hazelcast.newHazelcastInstance(config));
  }

}
