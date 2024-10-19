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
package org.sonar.server.v2.config;

import javax.annotation.Nullable;
import org.sonar.api.platform.Server;
import org.sonar.api.resources.Languages;
import org.sonar.db.Database;
import org.sonar.db.DbClient;
import org.sonar.server.common.email.config.EmailConfigurationService;
import org.sonar.server.common.github.config.GithubConfigurationService;
import org.sonar.server.common.gitlab.config.GitlabConfigurationService;
import org.sonar.server.common.group.service.GroupMembershipService;
import org.sonar.server.common.group.service.GroupService;
import org.sonar.server.common.health.CeStatusNodeCheck;
import org.sonar.server.common.health.DbConnectionNodeCheck;
import org.sonar.server.common.health.EsStatusNodeCheck;
import org.sonar.server.common.health.WebServerStatusNodeCheck;
import org.sonar.server.common.management.ManagedInstanceChecker;
import org.sonar.server.common.platform.LivenessChecker;
import org.sonar.server.common.platform.LivenessCheckerImpl;
import org.sonar.server.common.project.ImportProjectService;
import org.sonar.server.common.projectbindings.service.ProjectBindingsService;
import org.sonar.server.common.rule.service.RuleService;
import org.sonar.server.common.text.MacroInterpreter;
import org.sonar.server.common.user.service.UserService;
import org.sonar.server.health.HealthChecker;
import org.sonar.server.platform.NodeInformation;
import org.sonar.server.platform.ServerFileSystem;
import org.sonar.server.platform.db.migration.DatabaseMigrationState;
import org.sonar.server.platform.db.migration.version.DatabaseVersion;
import org.sonar.server.rule.RuleDescriptionFormatter;
import org.sonar.server.user.SystemPasscode;
import org.sonar.server.user.UserSession;
import org.sonar.server.v2.api.analysis.controller.DefaultJresController;
import org.sonar.server.v2.api.analysis.controller.DefaultScannerEngineController;
import org.sonar.server.v2.api.analysis.controller.DefaultVersionController;
import org.sonar.server.v2.api.analysis.controller.JresController;
import org.sonar.server.v2.api.analysis.controller.ScannerEngineController;
import org.sonar.server.v2.api.analysis.controller.VersionController;
import org.sonar.server.v2.api.analysis.service.JresHandler;
import org.sonar.server.v2.api.analysis.service.JresHandlerImpl;
import org.sonar.server.v2.api.analysis.service.ScannerEngineHandler;
import org.sonar.server.v2.api.analysis.service.ScannerEngineHandlerImpl;
import org.sonar.server.v2.api.dop.controller.DefaultDopSettingsController;
import org.sonar.server.v2.api.dop.controller.DopSettingsController;
import org.sonar.server.v2.api.email.config.controller.DefaultEmailConfigurationController;
import org.sonar.server.v2.api.email.config.controller.EmailConfigurationController;
import org.sonar.server.v2.api.github.config.controller.DefaultGithubConfigurationController;
import org.sonar.server.v2.api.github.config.controller.GithubConfigurationController;
import org.sonar.server.v2.api.gitlab.config.controller.DefaultGitlabConfigurationController;
import org.sonar.server.v2.api.gitlab.config.controller.GitlabConfigurationController;
import org.sonar.server.v2.api.group.controller.DefaultGroupController;
import org.sonar.server.v2.api.group.controller.GroupController;
import org.sonar.server.v2.api.membership.controller.DefaultGroupMembershipController;
import org.sonar.server.v2.api.membership.controller.GroupMembershipController;
import org.sonar.server.v2.api.projectbindings.controller.DefaultProjectBindingsController;
import org.sonar.server.v2.api.projectbindings.controller.ProjectBindingsController;
import org.sonar.server.v2.api.projects.controller.BoundProjectsController;
import org.sonar.server.v2.api.projects.controller.DefaultBoundProjectsController;
import org.sonar.server.v2.api.rule.controller.DefaultRuleController;
import org.sonar.server.v2.api.rule.controller.RuleController;
import org.sonar.server.v2.api.rule.converter.RuleRestResponseGenerator;
import org.sonar.server.v2.api.system.controller.DatabaseMigrationsController;
import org.sonar.server.v2.api.system.controller.DefaultLivenessController;
import org.sonar.server.v2.api.system.controller.HealthController;
import org.sonar.server.v2.api.system.controller.LivenessController;
import org.sonar.server.v2.api.user.controller.DefaultUserController;
import org.sonar.server.v2.api.user.controller.UserController;
import org.sonar.server.v2.api.user.converter.UsersSearchRestResponseGenerator;
import org.sonar.server.v2.common.DeprecatedHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

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
  public DatabaseMigrationsController databaseMigrationsController(DatabaseVersion databaseVersion, DatabaseMigrationState databaseMigrationState,
    Database database) {
    return new DatabaseMigrationsController(databaseVersion, databaseMigrationState, database);
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

  @Bean
  public RuleRestResponseGenerator ruleRestResponseGenerator(Languages languages, MacroInterpreter macroInterpreter, RuleDescriptionFormatter ruleDescriptionFormatter) {
    return new RuleRestResponseGenerator(languages, macroInterpreter, ruleDescriptionFormatter);
  }

  @Bean
  public RuleController ruleController(UserSession userSession, RuleService ruleService, RuleRestResponseGenerator ruleRestResponseGenerator) {
    return new DefaultRuleController(userSession, ruleService, ruleRestResponseGenerator);
  }

  @Primary
  @Bean("org.sonar.server.v2.config.PlatformLevel4WebConfig.requestMappingHandlerMapping")
  public RequestMappingHandlerMapping requestMappingHandlerMapping(UserSession userSession) {
    RequestMappingHandlerMapping handlerMapping = new RequestMappingHandlerMapping();
    handlerMapping.setInterceptors(new DeprecatedHandler(userSession));
    return handlerMapping;
  }

  @Bean
  public GitlabConfigurationController gitlabConfigurationController(UserSession userSession, GitlabConfigurationService gitlabConfigurationService) {
    return new DefaultGitlabConfigurationController(userSession, gitlabConfigurationService);
  }

  @Bean
  public GithubConfigurationController githubConfigurationController(UserSession userSession, GithubConfigurationService githubConfigurationService) {
    return new DefaultGithubConfigurationController(userSession, githubConfigurationService);
  }

  @Bean
  public BoundProjectsController importedProjectsController(UserSession userSession, ImportProjectService importProjectService) {
    return new DefaultBoundProjectsController(userSession, importProjectService);
  }

  @Bean
  public DopSettingsController dopSettingsController(UserSession userSession, DbClient dbClient) {
    return new DefaultDopSettingsController(userSession, dbClient);
  }

  @Bean
  public ProjectBindingsController projectBindingsController(UserSession userSession, ProjectBindingsService projectBindingsService) {
    return new DefaultProjectBindingsController(userSession, projectBindingsService);
  }

  @Bean
  public VersionController versionController(Server server) {
    return new DefaultVersionController(server);
  }

  @Bean
  public JresHandler jresHandler() {
    return new JresHandlerImpl();
  }

  @Bean
  public JresController jresController(JresHandler jresHandler) {
    return new DefaultJresController(jresHandler);
  }

  @Bean
  public ScannerEngineHandler scannerEngineHandler(ServerFileSystem serverFileSystem) {
    return new ScannerEngineHandlerImpl(serverFileSystem);
  }

  @Bean
  public ScannerEngineController scannerEngineController(ScannerEngineHandler scannerEngineHandler) {
    return new DefaultScannerEngineController(scannerEngineHandler);
  }

  @Bean
  public EmailConfigurationController emailConfigurationController(UserSession userSession, EmailConfigurationService emailConfigurationService) {
    return new DefaultEmailConfigurationController(userSession, emailConfigurationService);
  }

}
