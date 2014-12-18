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

package org.sonar.server.user;

import com.google.common.base.Preconditions;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.List;

public class UpdateUser {

  private String login;
  private String name;
  private String email;
  private List<String> scmAccounts;

  private String password;
  private String passwordConfirmation;

  boolean isNameChanged, isEmailChanged, isScmAccountsChanged, isPasswordChanged;

  private UpdateUser(String login) {
    // No direct call to this constructor
    this.login = login;
  }

  public String login() {
    return login;
  }

  @CheckForNull
  public String name() {
    return name;
  }

  public UpdateUser setName(@Nullable String name) {
    this.name = name;
    isNameChanged = true;
    return this;
  }

  @CheckForNull
  public String email() {
    return email;
  }

  public UpdateUser setEmail(@Nullable String email) {
    this.email = email;
    isEmailChanged = true;
    return this;
  }

  @CheckForNull
  public List<String> scmAccounts() {
    return scmAccounts;
  }

  public UpdateUser setScmAccounts(@Nullable List<String> scmAccounts) {
    this.scmAccounts = scmAccounts;
    isScmAccountsChanged = true;
    return this;
  }

  @CheckForNull
  public String password() {
    return password;
  }

  public UpdateUser setPassword(@Nullable String password) {
    this.password = password;
    isPasswordChanged = true;
    return this;
  }

  @CheckForNull
  public String passwordConfirmation() {
    return passwordConfirmation;
  }

  public UpdateUser setPasswordConfirmation(@Nullable String passwordConfirmation) {
    this.passwordConfirmation = passwordConfirmation;
    isPasswordChanged = true;
    return this;
  }

  public boolean isNameChanged() {
    return isNameChanged;
  }

  public boolean isEmailChanged() {
    return isEmailChanged;
  }

  public boolean isScmAccountsChanged() {
    return isScmAccountsChanged;
  }

  public boolean isPasswordChanged() {
    return isPasswordChanged;
  }

  public static UpdateUser create(String login) {
    Preconditions.checkNotNull(login);
    return new UpdateUser(login);
  }
}
