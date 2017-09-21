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
package org.sonar.process.cluster.hz;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.MemberAttributeConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.Hazelcast;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import org.sonar.process.NetworkUtilsImpl;
import org.sonar.process.ProcessId;
import org.sonar.process.ProcessProperties;
import org.sonar.process.cluster.NodeType;
import org.sonar.process.cluster.hz.HazelcastMember.Attribute;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

public class HazelcastMemberBuilder {

  private String clusterName;
  private String nodeName;
  private int port;
  private NodeType nodeType;
  private ProcessId processId;
  private String networkInterface;
  private List<String> members = new ArrayList<>();

  public HazelcastMemberBuilder setClusterName(String s) {
    this.clusterName = s;
    return this;
  }

  public HazelcastMemberBuilder setNodeName(String s) {
    this.nodeName = s;
    return this;
  }

  public HazelcastMemberBuilder setNodeType(NodeType t) {
    this.nodeType = t;
    return this;
  }

  public HazelcastMemberBuilder setProcessId(ProcessId p) {
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
    this.members = c.stream()
      .map(host -> host.contains(":") ? host : format("%s:%d", host, ProcessProperties.CLUSTER_NODE_PORT_DEFAULT_VALUE))
      .collect(Collectors.toList());
    return this;
  }

  public HazelcastMember build() {
    Config config = new Config();
    config.getGroupConfig().setName(requireNonNull(clusterName, "Cluster name is missing"));

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
    attributes.setStringAttribute(Attribute.HOSTNAME.getKey(), NetworkUtilsImpl.INSTANCE.getHostname());
    attributes.setStringAttribute(Attribute.IP_ADDRESSES.getKey(), NetworkUtilsImpl.INSTANCE.getIPAddresses());
    attributes.setStringAttribute(Attribute.NODE_NAME.getKey(), requireNonNull(nodeName, "Node name is missing"));
    attributes.setStringAttribute(Attribute.NODE_TYPE.getKey(), requireNonNull(nodeType, "Node type is missing").getValue());
    attributes.setStringAttribute(Attribute.PROCESS_KEY.getKey(), requireNonNull(processId, "Process key is missing").getKey());

    return new HazelcastMemberImpl(Hazelcast.newHazelcastInstance(config));
  }

}
