/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.core.user;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

/**
 * @since 3.2
 */
public class UserDto {
  private Long id;
  private String login;
  private String name;
  private String email;
  private boolean active = true;
  private String scmAccounts;
  private String cryptedPassword;
  private String salt;
  private Long createdAt;
  private Long updatedAt;

  public Long getId() {
    return id;
  }

  public UserDto setId(Long id) {
    this.id = id;
    return this;
  }

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

  public String getScmAccounts() {
    return scmAccounts;
  }

  public UserDto setScmAccounts(String scmAccounts) {
    this.scmAccounts = scmAccounts;
    return this;
  }

  public String getCryptedPassword() {
    return cryptedPassword;
  }

  public UserDto setCryptedPassword(String cryptedPassword) {
    this.cryptedPassword = cryptedPassword;
    return this;
  }

  public String getSalt() {
    return salt;
  }

  public UserDto setSalt(String salt) {
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

  public DefaultUser toUser() {
    return new DefaultUser()
      .setLogin(login)
      .setName(name)
      .setEmail(email)
      .setActive(active);
  }
}
