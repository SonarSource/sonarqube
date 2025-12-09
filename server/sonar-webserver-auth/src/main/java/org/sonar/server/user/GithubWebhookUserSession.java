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
package org.sonar.server.user;

import java.util.Collection;
import java.util.Optional;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.user.GroupDto;

import static java.util.Collections.emptySet;

public class GithubWebhookUserSession extends AbstractUserSession {

  public static final String GITHUB_WEBHOOK_USER_NAME = "github-webhook";

  @Override
  public String getLogin() {
    return GITHUB_WEBHOOK_USER_NAME;
  }

  @Override
  public String getUuid() {
    throw new IllegalStateException("GithubWebhookUserSession does not contain a uuid.");
  }

  @Override
  public String getName() {
    return GITHUB_WEBHOOK_USER_NAME;
  }

  @Override
  public Collection<GroupDto> getGroups() {
    return emptySet();
  }

  @Override
  public boolean shouldResetPassword() {
    return false;
  }

  @Override
  public Optional<IdentityProvider> getIdentityProvider() {
    return Optional.empty();
  }

  @Override
  public Optional<ExternalIdentity> getExternalIdentity() {
    return Optional.empty();
  }

  @Override
  public boolean isLoggedIn() {
    return true;
  }

  @Override
  public boolean isSystemAdministrator() {
    return false;
  }

  @Override
  public boolean isActive() {
    return true;
  }

  @Override
  public boolean isAuthenticatedBrowserSession() {
    return false;
  }

  @Override
  protected boolean hasPermissionImpl(GlobalPermission permission) {
    return false;
  }

  @Override
  protected Optional<String> componentUuidToEntityUuid(String componentUuid) {
    return Optional.empty();
  }

  @Override
  protected boolean hasEntityUuidPermission(ProjectPermission permission, String entityUuid) {
    return false;
  }

  @Override
  protected boolean hasChildProjectsPermission(ProjectPermission permission, String applicationUuid) {
    return false;
  }

  @Override
  protected boolean hasPortfolioChildProjectsPermission(ProjectPermission permission, String portfolioUuid) {
    return false;
  }

  @Override
  public boolean hasComponentUuidPermission(ProjectPermission permission, String componentUuid) {
    return true;
  }

}
