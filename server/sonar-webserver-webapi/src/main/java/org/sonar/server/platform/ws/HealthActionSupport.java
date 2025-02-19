/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.platform.ws;

import com.google.common.io.Resources;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.Comparator;
import org.apache.commons.lang3.StringUtils;
import org.sonar.api.server.ws.WebService;
import org.sonar.process.ProcessProperties;
import org.sonar.process.cluster.health.NodeDetails;
import org.sonar.process.cluster.health.NodeHealth;
import org.sonar.server.health.ClusterHealth;
import org.sonar.server.health.Health;
import org.sonar.server.health.HealthChecker;
import org.sonarqube.ws.System;

import static org.sonar.api.utils.DateUtils.formatDateTime;

public class HealthActionSupport {
  private static final Comparator<NodeHealth> NODE_HEALTH_COMPARATOR = Comparator.<NodeHealth>comparingInt(s -> s.getDetails().getType().ordinal())
    .thenComparing(a -> a.getDetails().getName())
    .thenComparing(a -> a.getDetails().getHost())
    .thenComparing(a -> a.getDetails().getPort());
  private final HealthChecker healthChecker;

  public HealthActionSupport(HealthChecker healthChecker) {
    this.healthChecker = healthChecker;
  }

  void define(WebService.NewController controller, SystemWsAction handler) {
    controller.createAction("health")
      .setDescription("Provide health status of SonarQube." +
        "<p>Although global health is calculated based on both application and search nodes, detailed information is returned only for application nodes.</p>" +
        "<p> " +
        " <ul>" +
        " <li>GREEN: SonarQube is fully operational</li>" +
        " <li>YELLOW: SonarQube is usable, but it needs attention in order to be fully operational</li>" +
        " <li>RED: SonarQube is not operational</li>" +
        " </ul>" +
        "</p><br>" +
        "Requires the 'Administer System' permission or " +
        "system passcode (see " + ProcessProperties.Property.WEB_SYSTEM_PASS_CODE + " in sonar.properties).<br>" +
        "When SonarQube is in safe mode (waiting or running a database upgrade), only the authentication with a system passcode is supported.")
      .setSince("6.6")
      .setResponseExample(Resources.getResource(this.getClass(), "example-health.json"))
      .setHandler(handler);
  }

  System.HealthResponse checkNodeHealth() {
    Health check = healthChecker.checkNode();
    System.HealthResponse.Builder responseBuilder = System.HealthResponse.newBuilder()
      .setHealth(System.Health.valueOf(check.getStatus().name()));
    System.Cause.Builder causeBuilder = System.Cause.newBuilder();
    check.getCauses().forEach(str -> responseBuilder.addCauses(causeBuilder.clear().setMessage(str).build()));

    return responseBuilder.build();
  }

  System.HealthResponse checkClusterHealth() {
    ClusterHealth check = healthChecker.checkCluster();
    return toResponse(check);
  }

  private static System.HealthResponse toResponse(ClusterHealth check) {
    System.HealthResponse.Builder responseBuilder = System.HealthResponse.newBuilder();
    System.Node.Builder nodeBuilder = System.Node.newBuilder();
    System.Cause.Builder causeBuilder = System.Cause.newBuilder();

    Health health = check.getHealth();
    responseBuilder.setHealth(System.Health.valueOf(health.getStatus().name()));
    health.getCauses().forEach(str -> responseBuilder.addCauses(toCause(str, causeBuilder)));

    System.Nodes.Builder nodesBuilder = System.Nodes.newBuilder();
    check.getNodes().stream()
      .sorted(NODE_HEALTH_COMPARATOR)
      .map(node -> toNode(node, nodeBuilder, causeBuilder))
      .forEach(nodesBuilder::addNodes);
    responseBuilder.setNodes(nodesBuilder.build());

    return responseBuilder.build();
  }

  private static System.Node toNode(NodeHealth nodeHealth, System.Node.Builder nodeBuilder, System.Cause.Builder causeBuilder) {
    nodeBuilder.clear();
    nodeBuilder.setHealth(System.Health.valueOf(nodeHealth.getStatus().name()));
    nodeHealth.getCauses().forEach(str -> nodeBuilder.addCauses(toCause(str, causeBuilder)));
    NodeDetails details = nodeHealth.getDetails();
    nodeBuilder
      .setType(System.NodeType.valueOf(details.getType().name()))
      .setName(details.getName())
      .setHost(resolveIp(details.getHost()))
      .setPort(details.getPort())
      .setStartedAt(formatDateTime(details.getStartedAt()));
    return nodeBuilder.build();
  }

  private static String resolveIp(String host) {
    if (!StringUtils.isEmpty(host)) {
      try {
        InetAddress inetAddress = InetAddress.getByName(host);
        if (inetAddress instanceof Inet6Address) {
          return "[" + host + "]";
        }
      } catch (Exception e) {
        return host;
      }
    }
    return host;
  }

  private static System.Cause toCause(String str, System.Cause.Builder causeBuilder) {
    return causeBuilder.clear().setMessage(str).build();
  }
}
