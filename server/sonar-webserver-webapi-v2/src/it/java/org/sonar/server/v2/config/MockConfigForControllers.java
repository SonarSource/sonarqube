/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.v2.config;

import org.sonar.db.DbClient;
import org.sonar.server.health.CeStatusNodeCheck;
import org.sonar.server.health.DbConnectionNodeCheck;
import org.sonar.server.health.EsStatusNodeCheck;
import org.sonar.server.health.HealthChecker;
import org.sonar.server.health.WebServerStatusNodeCheck;
import org.sonar.server.management.ManagedInstanceChecker;
import org.sonar.server.platform.NodeInformation;
import org.sonar.server.platform.ws.LivenessChecker;
import org.sonar.server.user.SystemPasscode;
import org.sonar.server.user.UserSession;
import org.sonar.server.user.UserUpdater;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.mockito.Mockito.mock;

@Configuration
public class MockConfigForControllers {

  @Bean
  public DbClient dbClient() {
    return mock(DbClient.class);
  }

  @Bean
  public DbConnectionNodeCheck dbConnectionNodeCheck() {
    return mock(DbConnectionNodeCheck.class);
  }

  @Bean
  public WebServerStatusNodeCheck webServerStatusNodeCheck() {
    return mock(WebServerStatusNodeCheck.class);
  }

  @Bean
  public CeStatusNodeCheck ceStatusNodeCheck() {
    return mock(CeStatusNodeCheck.class);
  }

  @Bean
  public EsStatusNodeCheck esStatusNodeCheck() {
    return mock(EsStatusNodeCheck.class);
  }

  @Bean
  public LivenessChecker livenessChecker() {
    return mock(LivenessChecker.class);
  }

  @Bean
  public HealthChecker healthChecker() {
    return mock(HealthChecker.class);
  }

  @Bean
  public SystemPasscode systemPasscode() {
    return mock(SystemPasscode.class);
  }

  @Bean
  public NodeInformation nodeInformation() {
    return mock(NodeInformation.class);
  }

  @Bean
  public UserSession userSession() {
    return mock(UserSession.class);
  }

  @Bean
  UserUpdater userUpdater() {
    return mock(UserUpdater.class);
  }

  @Bean
  ManagedInstanceChecker managedInstanceChecker() {
    return mock(ManagedInstanceChecker.class);
  }
}
