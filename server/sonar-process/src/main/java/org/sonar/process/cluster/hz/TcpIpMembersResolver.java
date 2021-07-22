/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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

import com.hazelcast.internal.util.AddressUtil;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import static java.lang.String.format;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NODE_HZ_PORT;

class TcpIpMembersResolver implements MembersResolver {
  private static final Logger LOG = Loggers.get(TcpIpMembersResolver.class);

  @Override
  public List<String> resolveMembers(List<String> membersToResolve) {
    return membersToResolve.stream().map(this::extractMembers).flatMap(Collection::stream).collect(Collectors.toList());
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

  private List<String> getAllByName(String hostname) {
    LOG.debug("Trying to resolve Hostname: " + hostname);
    try {
      return Arrays.stream(InetAddress.getAllByName(hostname)).map(InetAddress::toString).collect(Collectors.toList());
    } catch (UnknownHostException e) {
      LOG.error("Host could not be found: " + e.getMessage());
    }
    return new ArrayList<>();
  }

}
