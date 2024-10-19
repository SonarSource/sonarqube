/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.platform.monitoring.cluster;

import org.sonar.api.SonarRuntime;
import org.sonar.api.platform.Server;
import org.sonar.api.server.ServerSide;
import org.sonar.process.systeminfo.Global;
import org.sonar.process.systeminfo.SystemInfoSection;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;
import org.sonar.server.platform.ContainerSupport;
import org.sonar.server.platform.StatisticsSupport;
import org.sonar.server.platform.monitoring.CommonSystemInformation;

import static org.sonar.api.measures.CoreMetrics.NCLOC;
import static org.sonar.process.systeminfo.SystemInfoUtils.addIfNotEmpty;
import static org.sonar.process.systeminfo.SystemInfoUtils.setAttribute;

@ServerSide
public class GlobalSystemSection implements SystemInfoSection, Global {

  private final Server server;
  private final ContainerSupport containerSupport;
  private final StatisticsSupport statisticsSupport;
  private final SonarRuntime sonarRuntime;
  private final CommonSystemInformation commonSystemInformation;

  public GlobalSystemSection(Server server, ContainerSupport containerSupport, StatisticsSupport statisticsSupport, SonarRuntime sonarRuntime,
    CommonSystemInformation commonSystemInformation) {
    this.server = server;
    this.containerSupport = containerSupport;
    this.statisticsSupport = statisticsSupport;
    this.sonarRuntime = sonarRuntime;
    this.commonSystemInformation = commonSystemInformation;
  }

  @Override
  public ProtobufSystemInfo.Section toProtobuf() {
    ProtobufSystemInfo.Section.Builder protobuf = ProtobufSystemInfo.Section.newBuilder();
    protobuf.setName("System");

    setAttribute(protobuf, "Server ID", server.getId());
    setAttribute(protobuf, "Edition", sonarRuntime.getEdition().getLabel());
    setAttribute(protobuf, NCLOC.getName() ,statisticsSupport.getLinesOfCode());
    setAttribute(protobuf, "Container", containerSupport.isRunningInContainer());
    setAttribute(protobuf, "Running on OpenShift", containerSupport.isRunningOnHelmOpenshift());
    setAttribute(protobuf, "Helm autoscaling", containerSupport.isHelmAutoscalingEnabled());
    setAttribute(protobuf, "High Availability", true);
    setAttribute(protobuf, "External Users and Groups Provisioning",
      commonSystemInformation.getManagedInstanceProviderName());
    setAttribute(protobuf, "External User Authentication",
      commonSystemInformation.getExternalUserAuthentication());
    addIfNotEmpty(protobuf, "Accepted external identity providers",
      commonSystemInformation.getEnabledIdentityProviders());
    addIfNotEmpty(protobuf, "External identity providers whose users are allowed to sign themselves up",
      commonSystemInformation.getAllowsToSignUpEnabledIdentityProviders());
    setAttribute(protobuf, "Force authentication", commonSystemInformation.getForceAuthentication());
    return protobuf.build();
  }
}
