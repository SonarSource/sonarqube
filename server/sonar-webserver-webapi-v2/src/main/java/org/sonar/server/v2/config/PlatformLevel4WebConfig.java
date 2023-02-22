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

import javax.annotation.Nullable;
import org.sonar.server.health.CeStatusNodeCheck;
import org.sonar.server.health.DbConnectionNodeCheck;
import org.sonar.server.health.EsStatusNodeCheck;
import org.sonar.server.health.HealthChecker;
import org.sonar.server.health.WebServerStatusNodeCheck;
import org.sonar.server.platform.NodeInformation;
import org.sonar.server.platform.ws.LivenessChecker;
import org.sonar.server.platform.ws.LivenessCheckerImpl;
import org.sonar.server.user.SystemPasscode;
import org.sonar.server.user.UserSession;
import org.sonar.server.v2.controller.DefautLivenessController;
import org.sonar.server.v2.controller.HealthController;
import org.sonar.server.v2.controller.LivenessController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(CommonWebConfig.class)
public class PlatformLevel4WebConfig {

  @Bean
  public LivenessChecker livenessChecker(DbConnectionNodeCheck dbConnectionNodeCheck, WebServerStatusNodeCheck webServerStatusNodeCheck, CeStatusNodeCheck ceStatusNodeCheck,
    @Nullable EsStatusNodeCheck esStatusNodeCheck) {
    return new LivenessCheckerImpl(dbConnectionNodeCheck, webServerStatusNodeCheck, ceStatusNodeCheck, esStatusNodeCheck);
  }

  @Bean
  public LivenessController livenessController(LivenessChecker livenessChecker, UserSession userSession, SystemPasscode systemPasscode) {
    return new DefautLivenessController(livenessChecker, systemPasscode, userSession);
  }

  @Bean
  public HealthController healthController(HealthChecker healthChecker, SystemPasscode systemPasscode, NodeInformation nodeInformation,
    UserSession userSession) {
    return new HealthController(healthChecker, systemPasscode, nodeInformation, userSession);
  }
}
