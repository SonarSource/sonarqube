/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.authentication;

import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.server.authentication.IdentityProvider;
import org.sonar.api.server.authentication.UserIdentity;
import org.sonar.server.authentication.event.AuthenticationEvent;

import static java.util.Objects.requireNonNull;

class UserRegistration {

  /**
   * Strategy to be executed when the email of the user is already used by another user
   */
  enum ExistingEmailStrategy {
    /**
     * Authentication is allowed, the email is moved from other user to current user
     */
    ALLOW,
    /**
     * Authentication process is stopped, the user is redirected to a page explaining that the email is already used
     */
    WARN,
    /**
     * Forbid authentication of the user
     */
    FORBID
  }

  private final UserIdentity userIdentity;
  private final IdentityProvider provider;
  private final AuthenticationEvent.Source source;
  private final ExistingEmailStrategy existingEmailStrategy;
  private final Set<String> organizationAlmIds;

  UserRegistration(Builder builder) {
    this.userIdentity = builder.userIdentity;
    this.provider = builder.provider;
    this.source = builder.source;
    this.existingEmailStrategy = builder.existingEmailStrategy;
    this.organizationAlmIds = builder.organizationAlmIds;
  }

  public UserIdentity getUserIdentity() {
    return userIdentity;
  }

  public IdentityProvider getProvider() {
    return provider;
  }

  public AuthenticationEvent.Source getSource() {
    return source;
  }

  public ExistingEmailStrategy getExistingEmailStrategy() {
    return existingEmailStrategy;
  }

  @CheckForNull
  public Set<String> getOrganizationAlmIds() {
    return organizationAlmIds;
  }

  static UserRegistration.Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private UserIdentity userIdentity;
    private IdentityProvider provider;
    private AuthenticationEvent.Source source;
    private ExistingEmailStrategy existingEmailStrategy;
    private Set<String> organizationAlmIds;

    public Builder setUserIdentity(UserIdentity userIdentity) {
      this.userIdentity = userIdentity;
      return this;
    }

    public Builder setProvider(IdentityProvider provider) {
      this.provider = provider;
      return this;
    }

    public Builder setSource(AuthenticationEvent.Source source) {
      this.source = source;
      return this;
    }

    /**
     * Strategy to be executed when the email of the user is already used by another user
     */
    public Builder setExistingEmailStrategy(ExistingEmailStrategy existingEmailStrategy) {
      this.existingEmailStrategy = existingEmailStrategy;
      return this;
    }

    /**
     * List of ALM organization the user is member of.
     * When set to null, it means that no organization membership synchronization should be done.
     */
    public Builder setOrganizationAlmIds(@Nullable Set<String> organizationAlmIds) {
      this.organizationAlmIds = organizationAlmIds;
      return this;
    }

    public UserRegistration build() {
      requireNonNull(userIdentity, "userIdentity must be set");
      requireNonNull(provider, "identityProvider must be set");
      requireNonNull(source, "Source must be set");
      requireNonNull(existingEmailStrategy, "existingEmailStrategy must be set ");
      return new UserRegistration(this);
    }
  }
}
