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
package org.sonar.server.platform.monitoring;

import org.sonar.api.SonarRuntime;
import org.sonar.api.config.Configuration;
import org.sonar.api.platform.Server;
import org.sonar.process.systeminfo.BaseSectionMBean;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;
import org.sonar.server.log.ServerLogging;
import org.sonar.server.platform.ContainerSupport;
import org.sonar.server.platform.OfficialDistribution;
import org.sonar.server.platform.StatisticsSupport;

import static org.sonar.api.measures.CoreMetrics.NCLOC;
import static org.sonar.process.ProcessProperties.Property.PATH_DATA;
import static org.sonar.process.ProcessProperties.Property.PATH_HOME;
import static org.sonar.process.ProcessProperties.Property.PATH_TEMP;
import static org.sonar.process.systeminfo.SystemInfoUtils.addIfNotEmpty;
import static org.sonar.process.systeminfo.SystemInfoUtils.setAttribute;

public class StandaloneSystemSection extends BaseSectionMBean implements SystemSectionMBean {

  private final Configuration config;
  private final Server server;
  private final ServerLogging serverLogging;
  private final OfficialDistribution officialDistribution;
  private final ContainerSupport containerSupport;
  private final StatisticsSupport statisticsSupport;
  private final SonarRuntime sonarRuntime;
  private final CommonSystemInformation commonSystemInformation;

  public StandaloneSystemSection(Configuration config, Server server, ServerLogging serverLogging,
    OfficialDistribution officialDistribution, ContainerSupport containerSupport, StatisticsSupport statisticsSupport,
    SonarRuntime sonarRuntime, CommonSystemInformation commonSystemInformation) {
    this.config = config;
    this.server = server;
    this.serverLogging = serverLogging;
    this.officialDistribution = officialDistribution;
    this.containerSupport = containerSupport;
    this.statisticsSupport = statisticsSupport;
    this.sonarRuntime = sonarRuntime;
    this.commonSystemInformation = commonSystemInformation;
  }

  @Override
  public String getServerId() {
    return server.getId();
  }

  @Override
  public String getVersion() {
    return server.getVersion();
  }

  @Override
  public String getLogLevel() {
    return serverLogging.getRootLoggerLevel().name();
  }

  @Override
  public String name() {
    // JMX name
    return "SonarQube";
  }

  @Override
  public ProtobufSystemInfo.Section toProtobuf() {
    ProtobufSystemInfo.Section.Builder protobuf = ProtobufSystemInfo.Section.newBuilder();
    protobuf.setName("System");

    setAttribute(protobuf, "Server ID", server.getId());
    setAttribute(protobuf, "Version", getVersion());
    setAttribute(protobuf, "Edition", sonarRuntime.getEdition().getLabel());
    setAttribute(protobuf, NCLOC.getName(), statisticsSupport.getLinesOfCode());
    setAttribute(protobuf, "Container", containerSupport.isRunningInContainer());
    setAttribute(protobuf, "External Users and Groups Provisioning", commonSystemInformation.getManagedInstanceProviderName());
    setAttribute(protobuf, "External User Authentication", commonSystemInformation.getExternalUserAuthentication());
    addIfNotEmpty(protobuf, "Accepted external identity providers",
      commonSystemInformation.getEnabledIdentityProviders());
    addIfNotEmpty(protobuf, "External identity providers whose users are allowed to sign themselves up",
      commonSystemInformation.getAllowsToSignUpEnabledIdentityProviders());
    setAttribute(protobuf, "High Availability", false);
    setAttribute(protobuf, "Official Distribution", officialDistribution.check());
    setAttribute(protobuf, "Force authentication", commonSystemInformation.getForceAuthentication());
    setAttribute(protobuf, "Home Dir", config.get(PATH_HOME.getKey()).orElse(null));
    setAttribute(protobuf, "Data Dir", config.get(PATH_DATA.getKey()).orElse(null));
    setAttribute(protobuf, "Temp Dir", config.get(PATH_TEMP.getKey()).orElse(null));
    setAttribute(protobuf, "Processors", Runtime.getRuntime().availableProcessors());
    return protobuf.build();
  }
}
