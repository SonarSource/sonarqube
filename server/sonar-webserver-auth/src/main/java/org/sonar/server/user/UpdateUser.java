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

import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class UpdateUser {

  private String login = null;
  private String name = null;
  private String email = null;
  private List<String> scmAccounts = null;
  private String password = null;
  private String externalIdentityProvider = null;
  private String externalIdentityProviderId = null;
  private String externalIdentityProviderLogin = null;
  private boolean loginChanged = false;
  private boolean nameChanged = false;
  private boolean emailChanged = false;
  private boolean scmAccountsChanged = false;
  private boolean passwordChanged = false;
  private boolean externalIdentityProviderChanged = false;
  private boolean externalIdentityProviderIdChanged = false;
  private boolean externalIdentityProviderLoginChanged = false;

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
  public String externalIdentityProvider() {
    return externalIdentityProvider;
  }

  public UpdateUser setExternalIdentityProvider(@Nullable String externalIdentityProvider) {
    this.externalIdentityProvider = externalIdentityProvider;
    externalIdentityProviderChanged = true;
    return this;
  }

  @CheckForNull
  public String externalIdentityProviderId() {
    return externalIdentityProviderId;
  }

  public UpdateUser setExternalIdentityProviderId(@Nullable String externalIdentityProviderId) {
    this.externalIdentityProviderId = externalIdentityProviderId;
    externalIdentityProviderIdChanged = true;
    return this;
  }

  @CheckForNull
  public String externalIdentityProviderLogin() {
    return externalIdentityProviderLogin;
  }

  public UpdateUser setExternalIdentityProviderLogin(@Nullable String getExternalIdentityProviderLogin) {
    this.externalIdentityProviderLogin = getExternalIdentityProviderLogin;
    externalIdentityProviderLoginChanged = true;
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

  public boolean isExternalIdentityProviderChanged() {
    return externalIdentityProviderChanged;
  }

  public boolean isExternalIdentityProviderIdChanged() {
    return externalIdentityProviderIdChanged;
  }

  public boolean isExternalIdentityProviderLoginChanged() {
    return externalIdentityProviderLoginChanged;
  }
}
