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
package org.sonar.server.user;

import java.util.Collection;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserTokenDto;

import static java.util.Objects.requireNonNull;

/**
 * Simple implementation of {@link UserSessionFactory}. It creates
 * instances of {@link UserSession} that don't manage groups nor
 * permissions. Only basic user information like {@link UserSession#isLoggedIn()}
 * and {@link UserSession#getLogin()} are available. The methods
 * relying on groups or permissions throw {@link UnsupportedOperationException}.
 */
public class TestUserSessionFactory implements UserSessionFactory {

  private TestUserSessionFactory() {
  }

  @Override
  public UserSession create(UserDto user) {
    return new TestUserSession(requireNonNull(user));
  }

  @Override
  public UserSession create(UserDto user, UserTokenDto userToken) {
    return new TestUserSession(requireNonNull(user));
  }

  @Override
  public GithubWebhookUserSession createGithubWebhookUserSession() {
    return new GithubWebhookUserSession();
  }

  @Override
  public UserSession createAnonymous() {
    return new TestUserSession(null);
  }

  public static TestUserSessionFactory standalone() {
    return new TestUserSessionFactory();
  }

  private static class TestUserSession extends AbstractUserSession {
    private final UserDto user;

    public TestUserSession(@Nullable UserDto user) {
      this.user = user;
    }

    @Override
    public String getLogin() {
      return user != null ? user.getLogin() : null;
    }

    @Override
    public String getUuid() {
      return user != null ? user.getUuid() : null;
    }

    @Override
    public String getName() {
      return user != null ? user.getName() : null;
    }

    @Override
    public Collection<GroupDto> getGroups() {
      throw notImplemented();
    }

    @Override
    public boolean shouldResetPassword() {
      return user != null && user.isResetPassword();
    }

    @Override
    public Optional<IdentityProvider> getIdentityProvider() {
      throw notImplemented();
    }

    @Override
    public Optional<ExternalIdentity> getExternalIdentity() {
      throw notImplemented();
    }

    @Override
    public boolean isLoggedIn() {
      return user != null;
    }

    @Override
    protected boolean hasPermissionImpl(GlobalPermission permission) {
      throw notImplemented();
    }

    @Override
    protected Optional<String> componentUuidToProjectUuid(String componentUuid) {
      throw notImplemented();
    }

    @Override
    protected boolean hasProjectUuidPermission(String permission, String projectUuid) {
      throw notImplemented();
    }

    @Override
    protected boolean hasChildProjectsPermission(String permission, String applicationUuid) {
      throw notImplemented();
    }

    @Override
    protected boolean hasPortfolioChildProjectsPermission(String permission, String portfolioUuid) { throw notImplemented(); }

    @Override
    public boolean isSystemAdministrator() {
      throw notImplemented();
    }

    @Override
    public boolean isActive() {
      throw notImplemented();
    }

    private static RuntimeException notImplemented() {
      return new UnsupportedOperationException("not implemented");
    }
  }
}
