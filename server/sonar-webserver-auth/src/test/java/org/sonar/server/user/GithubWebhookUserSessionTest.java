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
package org.sonar.server.user;

import java.util.Arrays;
import org.junit.Test;
import org.sonar.db.permission.GlobalPermission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.sonar.server.user.GithubWebhookUserSession.GITHUB_WEBHOOK_USER_NAME;

public class GithubWebhookUserSessionTest {

  GithubWebhookUserSession githubWebhookUserSession = new GithubWebhookUserSession();

  @Test
  public void getLogin() {
    assertThat(githubWebhookUserSession.getLogin()).isEqualTo(GITHUB_WEBHOOK_USER_NAME);
  }

  @Test
  public void getUuid() {
    assertThatIllegalStateException().isThrownBy(() -> githubWebhookUserSession.getUuid());
  }

  @Test
  public void getName() {
    assertThat(githubWebhookUserSession.getName()).isEqualTo(GITHUB_WEBHOOK_USER_NAME);
  }

  @Test
  public void getGroups() {
    assertThat(githubWebhookUserSession.getGroups()).isEmpty();
  }

  @Test
  public void shouldResetPassword() {
    assertThat(githubWebhookUserSession.shouldResetPassword()).isFalse();
  }

  @Test
  public void getIdentityProvider() {
    assertThat(githubWebhookUserSession.getIdentityProvider()).isEmpty();
  }

  @Test
  public void getExternalIdentity() {
    assertThat(githubWebhookUserSession.getExternalIdentity()).isEmpty();
  }

  @Test
  public void isLoggedIn() {
    assertThat(githubWebhookUserSession.isLoggedIn()).isTrue();
  }

  @Test
  public void isSystemAdministrator() {
    assertThat(githubWebhookUserSession.isSystemAdministrator()).isFalse();
  }

  @Test
  public void isActive() {
    assertThat(githubWebhookUserSession.isActive()).isTrue();
  }

  @Test
  public void hasPermissionImpl() {
    Arrays.stream(GlobalPermission.values())
      .forEach(globalPermission ->
        assertThat(githubWebhookUserSession.hasPermissionImpl(globalPermission)).isFalse()
      );
  }

  @Test
  public void componentUuidToProjectUuid() {
    assertThat(githubWebhookUserSession.componentUuidToEntityUuid("test")).isEmpty();
  }

  @Test
  public void hasProjectUuidPermission() {
    assertThat(githubWebhookUserSession.hasEntityUuidPermission("perm", "project")).isFalse();
  }

  @Test
  public void hasChildProjectsPermission() {
    assertThat(githubWebhookUserSession.hasChildProjectsPermission("perm", "project")).isFalse();
  }

  @Test
  public void hasPortfolioChildProjectsPermission() {
    assertThat(githubWebhookUserSession.hasPortfolioChildProjectsPermission("perm", "project")).isFalse();
  }

  @Test
  public void hasComponentUuidPermission_returnsAlwaysTrue() {
    assertThat(githubWebhookUserSession.hasComponentUuidPermission("perm", "project")).isTrue();
  }

  @Test
  public void isAuthenticatedGuiSession_isAlwaysFalse() {
    assertThat(githubWebhookUserSession.isAuthenticatedBrowserSession()).isFalse();
  }
}
