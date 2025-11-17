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
package org.sonar.server.v2.config;

import org.sonar.server.rule.ActiveRuleService;
import org.sonar.server.user.UserSession;
import org.sonar.server.v2.api.analysis.controller.DefaultActiveRulesController;
import org.sonar.server.v2.api.analysis.controller.DefaultJresController;
import org.sonar.server.v2.api.analysis.controller.DefaultScannerEngineController;
import org.sonar.server.v2.api.analysis.controller.DefaultVersionController;
import org.sonar.server.v2.api.analysis.service.ActiveRulesHandlerImpl;
import org.sonar.server.v2.api.analysis.service.JresHandlerImpl;
import org.sonar.server.v2.api.analysis.service.ScannerEngineHandlerImpl;
import org.sonar.server.v2.api.azurebilling.controller.DefaultAzureBillingController;
import org.sonar.server.v2.api.azurebilling.environment.AzureEnvironment;
import org.sonar.server.v2.api.azurebilling.service.DefaultAzureBillingHandler;
import org.sonar.server.v2.api.dop.controller.DefaultDopSettingsController;
import org.sonar.server.v2.api.email.config.controller.DefaultEmailConfigurationController;
import org.sonar.server.v2.api.github.config.controller.DefaultGithubConfigurationController;
import org.sonar.server.v2.api.gitlab.config.controller.DefaultGitlabConfigurationController;
import org.sonar.server.v2.api.group.controller.DefaultGroupController;
import org.sonar.server.v2.api.membership.controller.DefaultGroupMembershipController;
import org.sonar.server.v2.api.mode.controller.DefaultModeController;
import org.sonar.server.v2.api.projectbindings.controller.DefaultProjectBindingsController;
import org.sonar.server.v2.api.projects.controller.DefaultBoundProjectsController;
import org.sonar.server.v2.api.rule.controller.DefaultRuleController;
import org.sonar.server.v2.api.rule.converter.RuleRestResponseGenerator;
import org.sonar.server.v2.api.system.controller.DatabaseMigrationsController;
import org.sonar.server.v2.api.system.controller.DefaultLivenessController;
import org.sonar.server.v2.api.system.controller.HealthController;
import org.sonar.server.v2.api.user.controller.DefaultUserController;
import org.sonar.server.v2.api.user.converter.UsersSearchRestResponseGenerator;
import org.sonar.server.v2.common.DeprecatedHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Configuration
@Import({
  ActiveRulesHandlerImpl.class,
  ActiveRuleService.class,
  ServerWebConfig.class,
  DatabaseMigrationsController.class,
  DefaultActiveRulesController.class,
  DefaultBoundProjectsController.class,
  DefaultDopSettingsController.class,
  DefaultEmailConfigurationController.class,
  DefaultGithubConfigurationController.class,
  DefaultGitlabConfigurationController.class,
  DefaultGroupController.class,
  DefaultGroupMembershipController.class,
  DefaultJresController.class,
  DefaultLivenessController.class,
  DefaultModeController.class,
  DefaultProjectBindingsController.class,
  DefaultRuleController.class,
  DefaultScannerEngineController.class,
  DefaultUserController.class,
  DefaultVersionController.class,
  HealthController.class,
  JresHandlerImpl.class,
  ScannerEngineHandlerImpl.class,
  UsersSearchRestResponseGenerator.class,
  RuleRestResponseGenerator.class,
  AzureEnvironment.class,
  DefaultAzureBillingHandler.class,
  DefaultAzureBillingController.class
})
public class PlatformLevel4WebConfig {

  @Primary
  @Bean("org.sonar.server.v2.config.PlatformLevel4WebConfig.requestMappingHandlerMapping")
  public RequestMappingHandlerMapping requestMappingHandlerMapping(UserSession userSession) {
    RequestMappingHandlerMapping handlerMapping = new RequestMappingHandlerMapping();
    handlerMapping.setInterceptors(new DeprecatedHandler(userSession));
    return handlerMapping;
  }

}
