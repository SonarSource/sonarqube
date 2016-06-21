/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import static java.util.Objects.requireNonNull;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.core.user.DefaultUser;

/**
 * @since 3.2
 */
public class UserDto {
  public static final char SCM_ACCOUNTS_SEPARATOR = '\n';

  private Long id;
  private String login;
  private String name;
  private String email;
  private boolean active = true;
  private String scmAccounts;
  private String externalIdentity;
  private String externalIdentityProvider;
  private String cryptedPassword;
  private String salt;
  private Long createdAt;
  private Long updatedAt;
  private boolean local = true;

  public Long getId() {
    return id;
  }

  public UserDto setId(Long id) {
    this.id = id;
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

  @CheckForNull
  public String getScmAccounts() {
    return scmAccounts;
  }

  public List<String> getScmAccountsAsList() {
    return decodeScmAccounts(scmAccounts);
  }

  /**
   * List of SCM accounts separated by '|'
   */
  public UserDto setScmAccounts(@Nullable String s) {
    this.scmAccounts = s;
    return this;
  }

  public UserDto setScmAccounts(@Nullable List list) {
    this.scmAccounts = encodeScmAccounts(list);
    return this;
  }

  @CheckForNull
  public static String encodeScmAccounts(@Nullable List<String> scmAccounts) {
    if (scmAccounts != null && !scmAccounts.isEmpty()) {
      return String.format("%s%s%s", SCM_ACCOUNTS_SEPARATOR, StringUtils.join(scmAccounts, SCM_ACCOUNTS_SEPARATOR), SCM_ACCOUNTS_SEPARATOR);
    }
    return null;
  }

  public static List<String> decodeScmAccounts(@Nullable String dbValue) {
    if (dbValue == null) {
      return new ArrayList<>();
    } else {
      return Lists.newArrayList(Splitter.on(SCM_ACCOUNTS_SEPARATOR).omitEmptyStrings().split(dbValue));
    }
  }

  public String getExternalIdentity() {
    return externalIdentity;
  }

  public UserDto setExternalIdentity(String authorithy) {
    this.externalIdentity = authorithy;
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

  public Long getCreatedAt() {
    return createdAt;
  }

  public UserDto setCreatedAt(Long createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public Long getUpdatedAt() {
    return updatedAt;
  }

  public UserDto setUpdatedAt(Long updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public static String encryptPassword(String password, String salt) {
    requireNonNull(password, "Password cannot be empty");
    requireNonNull(salt, "Salt cannot be empty");
    return DigestUtils.sha1Hex("--" + salt + "--" + password + "--");
  }

  public DefaultUser toUser() {
    return new DefaultUser()
      .setLogin(login)
      .setName(name)
      .setEmail(email)
      .setActive(active);
  }
}
