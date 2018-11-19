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
package org.sonarqube.ws.client.user;

import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Collections.emptyList;

@Immutable
public class CreateRequest {

  private final String login;
  private final String password;
  private final String name;
  private final String email;
  private final List<String> scmAccounts;
  private final boolean local;

  private CreateRequest(Builder builder) {
    this.login = builder.login;
    this.password = builder.password;
    this.name = builder.name;
    this.email = builder.email;
    this.scmAccounts = builder.scmAccounts;
    this.local = builder.local;
  }

  public String getLogin() {
    return login;
  }

  @CheckForNull
  public String getPassword() {
    return password;
  }

  public String getName() {
    return name;
  }

  @CheckForNull
  public String getEmail() {
    return email;
  }

  public List<String> getScmAccounts() {
    return scmAccounts;
  }

  public boolean isLocal() {
    return local;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String login;
    private String password;
    private String name;
    private String email;
    private List<String> scmAccounts = emptyList();
    private boolean local = true;

    private Builder() {
      // enforce factory method use
    }

    public Builder setLogin(String login) {
      this.login = login;
      return this;
    }

    public Builder setPassword(@Nullable String password) {
      this.password = password;
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

    public Builder setLocal(boolean local) {
      this.local = local;
      return this;
    }

    public CreateRequest build() {
      checkArgument(!isNullOrEmpty(login), "Login is mandatory and must not be empty");
      checkArgument(!isNullOrEmpty(name), "Name is mandatory and must not be empty");
      checkArgument(!local || !isNullOrEmpty(password), "Password is mandatory and must not be empty");
      checkArgument(local || isNullOrEmpty(password), "Password should only be set on local user");
      return new CreateRequest(this);
    }
  }
}
