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
package org.sonar.server.user;

import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class NewUser {

  private String login;
  private String name;
  private String email;
  private List<String> scmAccounts;
  private String password;
  private ExternalIdentity externalIdentity;

  private NewUser() {
    // No direct call to this constructor
  }

  public NewUser setLogin(@Nullable String login) {
    this.login = login;
    return this;
  }

  @Nullable
  public String login() {
    return login;
  }

  @Nullable
  public String name() {
    return name;
  }

  public NewUser setName(@Nullable String name) {
    this.name = name;
    return this;
  }

  @CheckForNull
  public String email() {
    return email;
  }

  public NewUser setEmail(@Nullable String email) {
    this.email = email;
    return this;
  }

  @Nullable
  public List<String> scmAccounts() {
    return scmAccounts;
  }

  public NewUser setScmAccounts(@Nullable List<String> scmAccounts) {
    this.scmAccounts = scmAccounts;
    return this;
  }

  @Nullable
  public String password() {
    return password;
  }

  public NewUser setPassword(@Nullable String password) {
    this.password = password;
    return this;
  }

  @Nullable
  public ExternalIdentity externalIdentity() {
    return externalIdentity;
  }

  public NewUser setExternalIdentity(@Nullable ExternalIdentity externalIdentity) {
    this.externalIdentity = externalIdentity;
    return this;
  }

  public static NewUser create() {
    return new NewUser();
  }

}
