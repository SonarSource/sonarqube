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
package org.sonar.api.server.authentication;

import java.util.HashSet;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.user.UserGroupValidation;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

/**
 * User information provided by the Identity Provider to be register into the platform.
 *
 * @since 5.4
 */
@Immutable
public final class UserIdentity {

  @Nullable
  private final String id;
  private final String providerLogin;
  @Nullable
  private final String login;
  private final String name;
  @Nullable
  private final String email;
  private final boolean groupsProvided;
  private final Set<String> groups;

  private UserIdentity(Builder builder) {
    this.id = builder.id;
    this.providerLogin = builder.providerLogin;
    this.login = builder.login;
    this.name = builder.name;
    this.email = builder.email;
    this.groupsProvided = builder.groupsProvided;
    this.groups = builder.groups;
  }

  /**
   * Optional unique ID for the related {@link IdentityProvider}.
   * If two {@link IdentityProvider} define two users with the same ID, then users are considered as identical.
   *
   * When the ID is not provided, the provider login {@link #getProviderLogin()} is used.
   *
   * @since 7.2
   */
  @CheckForNull
  public String getProviderId() {
    return id;
  }

  /**
   * Non-blank user login for the related {@link IdentityProvider}.
   */
  public String getProviderLogin() {
    return providerLogin;
  }

  /**
   * User login, unique for the SonarQube platform.
   * If two {@link IdentityProvider} define two users with the same login, then users are considered as identical.
   *
   * Since 7.4, a unique login will be generated if result is null and the user referenced by {@link #getProviderId()}
   * or {@link #getProviderLogin()} does not already exist.
   */
  @CheckForNull
  public String getLogin() {
    return login;
  }

  /**
   * Non-blank display name. Uniqueness is not mandatory, even it's recommended for easier search of users
   * in webapp.
   */
  public String getName() {
    return name;
  }

  /**
   * Optional non-blank email. If defined, then it must be unique among all the users defined by all
   * {@link IdentityProvider}. If not unique, then authentication will fail.
   */
  @CheckForNull
  public String getEmail() {
    return email;
  }

  /**
   * Return true if groups should be synchronized for this user.
   *
   * @since 5.5
   */
  public boolean shouldSyncGroups() {
    return groupsProvided;
  }

  /**
   * List of group membership of the user. Only existing groups in SonarQube will be synchronized.
   *
   * @since 5.5
   */
  public Set<String> getGroups() {
    return groups;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String id;
    private String providerLogin;
    private String login;
    private String name;
    private String email;
    private boolean groupsProvided = false;
    private Set<String> groups = new HashSet<>();

    private Builder() {
    }

    /**
     * @see UserIdentity#getProviderId()
     * @since 7.2
     */
    public Builder setProviderId(@Nullable String id) {
      this.id = id;
      return this;
    }

    /**
     * @see UserIdentity#getProviderLogin()
     */
    public Builder setProviderLogin(String providerLogin) {
      this.providerLogin = providerLogin;
      return this;
    }

    /**
     * @see UserIdentity#getLogin()
     */
    public Builder setLogin(@Nullable String login) {
      this.login = login;
      return this;
    }

    /**
     * @see UserIdentity#getName()
     */
    public Builder setName(String name) {
      this.name = name;
      return this;
    }

    /**
     * @see UserIdentity#getEmail()
     */
    public Builder setEmail(@Nullable String email) {
      this.email = email;
      return this;
    }

    /**
     * Set group membership of the user. This method should only be used when synchronization of groups should be done.
     * <ul>
     *   <li>When groups are not empty, group membership is synchronized when user logs in :
     *   <ul>
     *     <li>User won't belong anymore to a group that is not in the list (even the default group defined in 'sonar-users')</li>
     *     <li>User will belong only to groups that exist in SonarQube</li>
     *     <li>Groups that don't exist in SonarQube are silently ignored</li>
     *   </ul>
     *   <li>When groups are empty, user won't belong to any group</li>
     * </ul>
     *
     * @throws NullPointerException when groups is null
     * @since 5.5
     */
    public Builder setGroups(Set<String> groups) {
      requireNonNull(groups, "Groups cannot be null, please don't use this method if groups should not be synchronized.");
      groups.forEach(UserGroupValidation::validateGroupName);
      this.groupsProvided = true;
      this.groups = groups;
      return this;
    }

    public UserIdentity build() {
      validateId(id);
      validateProviderLogin(providerLogin);
      validateLogin(login);
      validateName(name);
      validateEmail(email);
      return new UserIdentity(this);
    }

    private static void validateId(@Nullable String id) {
      checkArgument(id == null || id.length() <= 255, "ID is too big (255 characters max)");
    }

    private static void validateProviderLogin(String providerLogin) {
      checkArgument(isNotBlank(providerLogin), "Provider login must not be blank");
      checkArgument(providerLogin.length() <= 255, "Provider login size is incorrect (maximum 255 characters)");
    }

    private static void validateLogin(@Nullable String login) {
      checkArgument(isBlank(login) || (login.length() <= 255 && login.length() >= 2), "User login size is incorrect (Between 2 and 255 characters)");
    }

    private static void validateName(String name) {
      checkArgument(isNotBlank(name), "User name must not be blank");
      checkArgument(name.length() <= 200, "User name size is too big (200 characters max)");
    }

    private static void validateEmail(@Nullable String email) {
      checkArgument(email == null || email.length() <= 100, "User email size is too big (100 characters max)");
    }
  }
}
