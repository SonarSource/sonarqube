/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.common.user.service;

import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;

public class UserCreateRequest {
  private final String email;
  private final Boolean local;
  private final String login;
  private final String name;
  private final String password;
  private final List<String> scmAccounts;

  private UserCreateRequest(Builder builder) {
    this.email = builder.email;
    this.local = builder.local;
    this.login = builder.login;
    this.name = builder.name;
    this.password = builder.password;
    this.scmAccounts = builder.scmAccounts;
  }

  public Optional<String> getEmail() {
    return Optional.ofNullable(email);
  }

  public Boolean isLocal() {
    return local;
  }

  public String getLogin() {
    return login;
  }

  public String getName() {
    return name;
  }

  public Optional<String> getPassword() {
    return Optional.ofNullable(password);
  }

  public Optional<List<String>> getScmAccounts() {
    return Optional.ofNullable(scmAccounts);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String email;
    private Boolean local;
    private String login;
    private String name;
    private String password;
    private List<String> scmAccounts;

    private Builder() {
      // enforce factory method use
    }

    public Builder setEmail(@Nullable String email) {
      this.email = email;
      return this;
    }

    public Builder setLocal(Boolean local) {
      this.local = local;
      return this;
    }

    public Builder setLogin(String login) {
      this.login = login;
      return this;
    }

    public Builder setName(String name) {
      this.name = name;
      return this;
    }

    public Builder setPassword(@Nullable String password) {
      this.password = password;
      return this;
    }

    public Builder setScmAccounts(@Nullable List<String> scmAccounts) {
      this.scmAccounts = scmAccounts;
      return this;
    }

    public UserCreateRequest build() {
      checkArgument(!local || !isNullOrEmpty(password), "Password is mandatory and must not be empty");
      checkArgument(local || isNullOrEmpty(password), "Password should only be set on local user");
      return new UserCreateRequest(this);
    }
  }

}
