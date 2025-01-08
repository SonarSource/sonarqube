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
package org.sonar.db.user;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.core.user.DefaultUser;

import static java.lang.String.CASE_INSENSITIVE_ORDER;
import static java.util.Comparator.comparing;

/**
 * @since 3.2
 */
public class UserDto implements UserId {
  /** Technical unique identifier, can't be null */
  private String uuid;
  private String login;
  private String name;
  private String email;
  private boolean active = true;
  private String externalId;
  private String externalLogin;
  private String externalIdentityProvider;
  // Hashed password that may be null in case of external authentication
  private String cryptedPassword;
  // Salt used for PBKDF2, null when bcrypt is used or for external authentication
  private String salt;
  // Hash method used to generate cryptedPassword, my be null in case of external authentication
  private String hashMethod;
  private String homepageType;
  private String homepageParameter;
  private boolean local = true;
  private boolean resetPassword = false;
  private List<String> scmAccounts = new ArrayList<>();

  /**
   * Date of the last time the user has accessed to the server.
   * Can be null when user has never been authenticated, or has not been authenticated since the creation of the column in SonarQube 7.7.
   */
  @Nullable
  private Long lastConnectionDate;

  /**
   * Date of the last time sonarlint connected to sonarqube WSs with this user's authentication.
   * Can be null when user has never been authenticated, or has not been authenticated since the creation of the column in SonarQube 8.8.
   */
  @Nullable
  private Long lastSonarlintConnectionDate;

  private Long createdAt;
  private Long updatedAt;

  public String getUuid() {
    return uuid;
  }

  public UserDto setUuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  /**
   * Spaces were authorized before SQ 5.4.
   * For versions 5.4+ it's not possible to create a login with a space character.
   */
  public String getLogin() {
    return login;
  }

  public UserDto setLogin(String login) {
    this.login = login;
    return this;
  }

  public String getName() {
    return name;
  }

  public UserDto setName(String name) {
    this.name = name;
    return this;
  }

  @CheckForNull
  public String getEmail() {
    return email;
  }

  public UserDto setEmail(@Nullable String email) {
    this.email = email;
    return this;
  }

  public boolean isActive() {
    return active;
  }

  public UserDto setActive(boolean b) {
    this.active = b;
    return this;
  }

  /**
   * Used by mybatis
   */
  private List<String> getScmAccounts() {
    return scmAccounts;
  }

  public List<String> getSortedScmAccounts() {
    // needs to be done when reading, as mybatis do not use the setter
    return scmAccounts.stream().sorted(comparing(s -> s, CASE_INSENSITIVE_ORDER)).toList();
  }

  public UserDto setScmAccounts(List<String> scmAccounts) {
    this.scmAccounts = scmAccounts;
    return this;
  }

  public String getExternalId() {
    return externalId;
  }

  public UserDto setExternalId(String externalId) {
    this.externalId = externalId;
    return this;
  }

  public String getExternalLogin() {
    return externalLogin;
  }

  public UserDto setExternalLogin(String externalLogin) {
    this.externalLogin = externalLogin;
    return this;
  }

  public String getExternalIdentityProvider() {
    return externalIdentityProvider;
  }

  public UserDto setExternalIdentityProvider(String externalIdentityProvider) {
    this.externalIdentityProvider = externalIdentityProvider;
    return this;
  }

  public boolean isLocal() {
    return local;
  }

  public UserDto setLocal(boolean local) {
    this.local = local;
    return this;
  }

  @CheckForNull
  public String getCryptedPassword() {
    return cryptedPassword;
  }

  public UserDto setCryptedPassword(@Nullable String cryptedPassword) {
    this.cryptedPassword = cryptedPassword;
    return this;
  }

  @CheckForNull
  public String getSalt() {
    return salt;
  }

  public UserDto setSalt(@Nullable String salt) {
    this.salt = salt;
    return this;
  }

  @CheckForNull
  public String getHashMethod() {
    return hashMethod;
  }

  public UserDto setHashMethod(@Nullable String hashMethod) {
    this.hashMethod = hashMethod;
    return this;
  }

  @CheckForNull
  public String getHomepageType() {
    return homepageType;
  }

  public UserDto setHomepageType(@Nullable String homepageType) {
    this.homepageType = homepageType;
    return this;
  }

  @CheckForNull
  public String getHomepageParameter() {
    return homepageParameter;
  }

  public UserDto setHomepageParameter(@Nullable String homepageParameter) {
    this.homepageParameter = homepageParameter;
    return this;
  }

  public boolean isResetPassword() {
    return resetPassword;
  }

  public UserDto setResetPassword(boolean resetPassword) {
    this.resetPassword = resetPassword;
    return this;
  }

  @CheckForNull
  public Long getLastConnectionDate() {
    return lastConnectionDate;
  }

  public UserDto setLastConnectionDate(@Nullable Long lastConnectionDate) {
    this.lastConnectionDate = lastConnectionDate;
    return this;
  }

  @CheckForNull
  public Long getLastSonarlintConnectionDate() {
    return lastSonarlintConnectionDate;
  }

  public UserDto setLastSonarlintConnectionDate(@Nullable Long lastSonarlintConnectionDate) {
    this.lastSonarlintConnectionDate = lastSonarlintConnectionDate;
    return this;
  }


  public Long getCreatedAt() {
    return createdAt;
  }

  UserDto setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public Long getUpdatedAt() {
    return updatedAt;
  }

  UserDto setUpdatedAt(long updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public DefaultUser toUser() {
    return new DefaultUser()
      .setLogin(login)
      .setName(name)
      .setEmail(email)
      .setActive(active);
  }
}
