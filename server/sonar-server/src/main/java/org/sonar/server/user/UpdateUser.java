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
package org.sonar.server.user;

import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class UpdateUser {

  private String login;
  private String name;
  private String email;
  private List<String> scmAccounts;
  private String password;
  private ExternalIdentity externalIdentity;

  private boolean loginChanged;
  private boolean nameChanged;
  private boolean emailChanged;
  private boolean scmAccountsChanged;
  private boolean passwordChanged;
  private boolean externalIdentityChanged;

  @CheckForNull
  public String login() {
    return login;
  }

  public UpdateUser setLogin(@Nullable String login) {
    this.login = login;
    loginChanged = true;
    return this;
  }

  @CheckForNull
  public String name() {
    return name;
  }

  public UpdateUser setName(@Nullable String name) {
    this.name = name;
    nameChanged = true;
    return this;
  }

  @CheckForNull
  public String email() {
    return email;
  }

  public UpdateUser setEmail(@Nullable String email) {
    this.email = email;
    emailChanged = true;
    return this;
  }

  @CheckForNull
  public List<String> scmAccounts() {
    return scmAccounts;
  }

  public UpdateUser setScmAccounts(@Nullable List<String> scmAccounts) {
    this.scmAccounts = scmAccounts;
    scmAccountsChanged = true;
    return this;
  }

  @CheckForNull
  public String password() {
    return password;
  }

  public UpdateUser setPassword(@Nullable String password) {
    this.password = password;
    passwordChanged = true;
    return this;
  }

  @CheckForNull
  public ExternalIdentity externalIdentity() {
    return externalIdentity;
  }

  /**
   * This method should only be used when updating a none local user
   */
  public UpdateUser setExternalIdentity(@Nullable ExternalIdentity externalIdentity) {
    this.externalIdentity = externalIdentity;
    externalIdentityChanged = true;
    return this;
  }

  public boolean isLoginChanged() {
    return loginChanged;
  }

  public boolean isNameChanged() {
    return nameChanged;
  }

  public boolean isEmailChanged() {
    return emailChanged;
  }

  public boolean isScmAccountsChanged() {
    return scmAccountsChanged;
  }

  public boolean isPasswordChanged() {
    return passwordChanged;
  }

  public boolean isExternalIdentityChanged() {
    return externalIdentityChanged;
  }

}
