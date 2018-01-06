/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkState;

public class NewUser {

  private String login;
  private String password;
  private String name;
  private String email;
  private List<String> scmAccounts;
  private ExternalIdentity externalIdentity;

  private NewUser(Builder builder) {
    this.login = builder.login;
    this.password = builder.password;
    this.name = builder.name;
    this.email = builder.email;
    this.scmAccounts = builder.scmAccounts;
    this.externalIdentity = builder.externalIdentity;
  }

  public String login() {
    return login;
  }

  public String name() {
    return name;
  }

  @CheckForNull
  public String email() {
    return email;
  }

  public NewUser setEmail(@Nullable String email) {
    this.email = email;
    return this;
  }

  public List<String> scmAccounts() {
    return scmAccounts;
  }

  public NewUser setScmAccounts(List<String> scmAccounts) {
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

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String login;
    private String name;
    private String email;
    private List<String> scmAccounts = new ArrayList<>();
    private String password;
    private ExternalIdentity externalIdentity;

    public Builder setLogin(String login) {
      this.login = login;
      return this;
    }

    public Builder setName(String name) {
      this.name = name;
      return this;
    }

    public Builder setEmail(@Nullable String email) {
      this.email = email;
      return this;
    }

    public Builder setScmAccounts(List<String> scmAccounts) {
      this.scmAccounts = scmAccounts;
      return this;
    }

    public Builder setPassword(@Nullable String password) {
      this.password = password;
      return this;
    }

    public Builder setExternalIdentity(@Nullable ExternalIdentity externalIdentity) {
      this.externalIdentity = externalIdentity;
      return this;
    }

    public NewUser build() {
      checkState(externalIdentity == null || password == null, "Password should not be set with an external identity");
      return new NewUser(this);
    }
  }
}
