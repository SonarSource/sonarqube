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
import org.sonar.db.DbClient;
import org.sonar.server.common.group.service.GroupMembershipService;
import org.sonar.server.common.group.service.GroupService;
import org.sonar.server.common.health.CeStatusNodeCheck;
import org.sonar.server.common.health.DbConnectionNodeCheck;
import org.sonar.server.common.health.EsStatusNodeCheck;
import org.sonar.server.common.health.WebServerStatusNodeCheck;
import org.sonar.server.common.management.ManagedInstanceChecker;
import org.sonar.server.common.platform.LivenessChecker;
import org.sonar.server.common.platform.LivenessCheckerImpl;
import org.sonar.server.common.user.service.UserService;
import org.sonar.server.health.HealthChecker;
import org.sonar.server.platform.NodeInformation;
import org.sonar.server.user.SystemPasscode;
import org.sonar.server.user.UserSession;
import org.sonar.server.v2.api.group.controller.DefaultGroupController;
import org.sonar.server.v2.api.group.controller.GroupController;
import org.sonar.server.v2.api.membership.controller.DefaultGroupMembershipController;
import org.sonar.server.v2.api.membership.controller.GroupMembershipController;
import org.sonar.server.v2.api.system.controller.DefaultLivenessController;
import org.sonar.server.v2.api.system.controller.HealthController;
import org.sonar.server.v2.api.system.controller.LivenessController;
import org.sonar.server.v2.api.user.controller.DefaultUserController;
import org.sonar.server.v2.api.user.controller.UserController;
import org.sonar.server.v2.api.user.converter.UsersSearchRestResponseGenerator;
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
    return new DefaultLivenessController(livenessChecker, systemPasscode, userSession);
  }

  @Bean
  public HealthController healthController(HealthChecker healthChecker, SystemPasscode systemPasscode, NodeInformation nodeInformation,
    UserSession userSession) {
    return new HealthController(healthChecker, systemPasscode, nodeInformation, userSession);
  }

  @Bean
  public UsersSearchRestResponseGenerator usersSearchResponseGenerator(UserSession userSession) {
    return new UsersSearchRestResponseGenerator(userSession);
  }

  @Bean
  public UserController userController(
    UserSession userSession,
    UsersSearchRestResponseGenerator usersSearchResponseGenerator,
    UserService userService) {
    return new DefaultUserController(userSession, userService, usersSearchResponseGenerator);
  }

  @Bean
  public GroupController groupController(UserSession userSession, DbClient dbClient, GroupService groupService, ManagedInstanceChecker managedInstanceChecker) {
    return new DefaultGroupController(userSession, dbClient, groupService, managedInstanceChecker);
  }


  @Bean
  public GroupMembershipController groupMembershipsController(UserSession userSession,
    GroupMembershipService groupMembershipService, ManagedInstanceChecker managedInstanceChecker) {
    return new DefaultGroupMembershipController(userSession, groupMembershipService, managedInstanceChecker);
  }


}
