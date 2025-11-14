/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.v2.api.email.config.controller;

import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.sonar.server.common.email.config.EmailConfiguration;
import org.sonar.server.common.email.config.EmailConfigurationAuthMethod;
import org.sonar.server.common.email.config.EmailConfigurationSecurityProtocol;
import org.sonar.server.common.email.config.EmailConfigurationService;
import org.sonar.server.common.email.config.UpdateEmailConfigurationRequest;
import org.sonar.server.user.UserSession;
import org.sonar.server.v2.api.email.config.request.EmailConfigurationCreateRestRequest;
import org.sonar.server.v2.api.email.config.request.EmailConfigurationUpdateRestRequest;
import org.sonar.server.v2.api.email.config.resource.EmailConfigurationResource;
import org.sonar.server.v2.api.email.config.response.EmailConfigurationSearchRestResponse;
import org.sonar.server.v2.api.response.PageRestResponse;

import static org.sonar.server.common.email.config.EmailConfigurationService.UNIQUE_EMAIL_CONFIGURATION_ID;

public class DefaultEmailConfigurationController implements EmailConfigurationController {

  private final UserSession userSession;
  private final EmailConfigurationService emailConfigurationService;

  public DefaultEmailConfigurationController(UserSession userSession, EmailConfigurationService emailConfigurationService) {
    this.userSession = userSession;
    this.emailConfigurationService = emailConfigurationService;
  }

  @Override
  public EmailConfigurationResource createEmailConfiguration(EmailConfigurationCreateRestRequest createRequest) {
    userSession.checkIsSystemAdministrator();
    EmailConfiguration createdConfiguration = emailConfigurationService.createConfiguration(toEmailConfiguration(createRequest));
    return toEmailConfigurationResource(createdConfiguration);
  }

  private static EmailConfiguration toEmailConfiguration(EmailConfigurationCreateRestRequest createRestRequest) {
    return new EmailConfiguration(
      UNIQUE_EMAIL_CONFIGURATION_ID,
      createRestRequest.host(),
      createRestRequest.port(),
      toSecurityProtocol(createRestRequest.securityProtocol()),
      createRestRequest.fromAddress(),
      createRestRequest.fromName(),
      createRestRequest.subjectPrefix(),
      toAuthMethod(createRestRequest.authMethod()),
      createRestRequest.username(),
      createRestRequest.basicPassword(),
      createRestRequest.oauthAuthenticationHost(),
      createRestRequest.oauthClientId(),
      createRestRequest.oauthClientSecret(),
      createRestRequest.oauthTenant()
    );
  }

  @Override
  public EmailConfigurationResource getEmailConfiguration(String id) {
    userSession.checkIsSystemAdministrator();
    return getEmailConfigurationResource(id);
  }

  private EmailConfigurationResource getEmailConfigurationResource(String id) {
    return toEmailConfigurationResource(emailConfigurationService.getConfiguration(id));
  }

  @Override
  public EmailConfigurationSearchRestResponse searchEmailConfigurations() {
    userSession.checkIsSystemAdministrator();

    List<EmailConfigurationResource> emailConfigurationResources = emailConfigurationService.findConfigurations()
      .stream()
      .map(DefaultEmailConfigurationController::toEmailConfigurationResource)
      .toList();

    PageRestResponse pageRestResponse = new PageRestResponse(1, 1000, emailConfigurationResources.size());
    return new EmailConfigurationSearchRestResponse(emailConfigurationResources, pageRestResponse);
  }

  @Override
  public EmailConfigurationResource updateEmailConfiguration(String id, EmailConfigurationUpdateRestRequest updateRequest) {
    userSession.checkIsSystemAdministrator();

    UpdateEmailConfigurationRequest updateEmailConfigurationRequest = toUpdateEmailConfigurationRequest(id, updateRequest);
    return toEmailConfigurationResource(emailConfigurationService.updateConfiguration(updateEmailConfigurationRequest));
  }

  private static UpdateEmailConfigurationRequest toUpdateEmailConfigurationRequest(String id, EmailConfigurationUpdateRestRequest updateRequest) {
    return UpdateEmailConfigurationRequest.builder()
      .emailConfigurationId(id)
      .host(updateRequest.getHost().toNonNullUpdatedValue())
      .port(updateRequest.getPort().toNonNullUpdatedValue())
      .securityProtocol(updateRequest.getSecurityProtocol().map(DefaultEmailConfigurationController::toSecurityProtocol).toNonNullUpdatedValue())
      .fromAddress(updateRequest.getFromAddress().toNonNullUpdatedValue())
      .fromName(updateRequest.getFromName().toNonNullUpdatedValue())
      .subjectPrefix(updateRequest.getSubjectPrefix().toNonNullUpdatedValue())
      .authMethod(updateRequest.getAuthMethod().map(DefaultEmailConfigurationController::toAuthMethod).toNonNullUpdatedValue())
      .username(updateRequest.getUsername().toNonNullUpdatedValue())
      .basicPassword(updateRequest.getBasicPassword().toNonNullUpdatedValue())
      .oauthAuthenticationHost(updateRequest.getOauthAuthenticationHost().toNonNullUpdatedValue())
      .oauthClientId(updateRequest.getOauthClientId().toNonNullUpdatedValue())
      .oauthClientSecret(updateRequest.getOauthClientSecret().toNonNullUpdatedValue())
      .oauthTenant(updateRequest.getOauthTenant().toNonNullUpdatedValue())
      .build();
  }

  private static EmailConfigurationResource toEmailConfigurationResource(EmailConfiguration configuration) {
    return new EmailConfigurationResource(
      configuration.id(),
      configuration.host(),
      configuration.port(),
      toRestSecurityProtocol(configuration.securityProtocol()),
      configuration.fromAddress(),
      configuration.fromName(),
      configuration.subjectPrefix(),
      toRestAuthMethod(configuration.authMethod()),
      configuration.username(),
      StringUtils.isNotEmpty(configuration.basicPassword()),
      configuration.oauthAuthenticationHost(),
      StringUtils.isNotEmpty(configuration.oauthClientId()),
      StringUtils.isNotEmpty(configuration.oauthClientSecret()),
      configuration.oauthTenant()
    );
  }

  @Override
  public void deleteEmailConfiguration(String id) {
    userSession.checkIsSystemAdministrator();
    emailConfigurationService.deleteConfiguration(id);
  }

  private static EmailConfigurationSecurityProtocol toSecurityProtocol(org.sonar.server.v2.api.email.config.resource.EmailConfigurationSecurityProtocol restSecurityProtocol) {
    return EmailConfigurationSecurityProtocol.valueOf(restSecurityProtocol.name());
  }

  private static EmailConfigurationAuthMethod toAuthMethod(org.sonar.server.v2.api.email.config.resource.EmailConfigurationAuthMethod restAuthMethod) {
    return EmailConfigurationAuthMethod.valueOf(restAuthMethod.name());
  }

  private static org.sonar.server.v2.api.email.config.resource.EmailConfigurationSecurityProtocol toRestSecurityProtocol(EmailConfigurationSecurityProtocol securityProtocol) {
    return org.sonar.server.v2.api.email.config.resource.EmailConfigurationSecurityProtocol.valueOf(securityProtocol.name());
  }

  private static org.sonar.server.v2.api.email.config.resource.EmailConfigurationAuthMethod toRestAuthMethod(EmailConfigurationAuthMethod authMethod) {
    return org.sonar.server.v2.api.email.config.resource.EmailConfigurationAuthMethod.valueOf(authMethod.name());
  }
}
