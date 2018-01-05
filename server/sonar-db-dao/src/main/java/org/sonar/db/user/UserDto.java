/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.codec.digest.DigestUtils;
import org.sonar.core.user.DefaultUser;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static org.sonar.core.util.stream.MoreCollectors.join;

/**
 * @since 3.2
 */
public class UserDto {
  static final char LINE_BREAK_SEPARATOR = '\n';
  private static final Splitter LINE_BREAK_SPLITTER = Splitter.on(LINE_BREAK_SEPARATOR).omitEmptyStrings();
  private static final Joiner LINE_BREAK_JOINER = Joiner.on(LINE_BREAK_SEPARATOR);

  private Integer id;
  private String login;
  private String name;
  private String email;
  private boolean active = true;
  private String scmAccounts;
  private String secondaryEmails;
  private String externalIdentity;
  private String externalIdentityProvider;
  private String cryptedPassword;
  private String salt;
  private Long createdAt;
  private Long updatedAt;
  private String homepageType;
  private String homepageParameter;
  private boolean local = true;
  private boolean root = false;
  private boolean onboarded = false;

  public Integer getId() {
    return id;
  }

  public UserDto setId(Integer id) {
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

  public UserDto setScmAccounts(@Nullable String s) {
    this.scmAccounts = s;
    return this;
  }

  public List<String> getScmAccountsAsList() {
    return decodeLineBreakSeparatedString(scmAccounts);
  }

  public UserDto setScmAccounts(@Nullable List<String> list) {
    this.scmAccounts = encodeLineBreakSeparatedString(list);
    return this;
  }

  @CheckForNull
  public String getSecondaryEmails() {
    return secondaryEmails;
  }

  public List<String> getSecondaryEmailsAsList() {
    return decodeLineBreakSeparatedString(secondaryEmails);
  }

  public UserDto setSecondaryEmails(@Nullable String s) {
    this.secondaryEmails = s;
    return this;
  }

  public UserDto setSecondaryEmails(@Nullable List<String> list) {
    this.secondaryEmails = encodeLineBreakSeparatedString(list);
    return this;
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

  public boolean isRoot() {
    return root;
  }

  /**
   * Setters is not accessible as MyBatis doesn't need setter to set the field and dedicated SQL requests must be used
   * to update the root flag of a user:
   * <ul>
   *   <li>a user can not be created root</li>
   *   <li>the generic update method of a user can not change its root flag</li>
   * </ul>
   */
  protected void setRoot(boolean root) {
    this.root = root;
  }

  public boolean isOnboarded() {
    return onboarded;
  }

  public UserDto setOnboarded(boolean onboarded) {
    this.onboarded = onboarded;
    return this;
  }

  public DefaultUser toUser() {
    return new DefaultUser()
      .setLogin(login)
      .setName(name)
      .setEmail(email)
      .setActive(active);
  }

  public static String encryptPassword(String password, String salt) {
    requireNonNull(password, "Password cannot be empty");
    requireNonNull(salt, "Salt cannot be empty");
    return DigestUtils.sha1Hex("--" + salt + "--" + password + "--");
  }

  @CheckForNull
  private static String encodeLineBreakSeparatedString(@Nullable List<String> list) {
    if (list == null || list.isEmpty()) {
      return null;
    }
    return String.format("%s%s%s",
      LINE_BREAK_SEPARATOR,
      list.stream().collect(join(LINE_BREAK_JOINER)),
      LINE_BREAK_SEPARATOR);
  }

  private static List<String> decodeLineBreakSeparatedString(@Nullable String dbValue) {
    return dbValue == null ? emptyList() : LINE_BREAK_SPLITTER.splitToList(dbValue);
  }
}
