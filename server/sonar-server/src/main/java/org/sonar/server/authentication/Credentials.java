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
package org.sonar.server.authentication;

import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang.StringUtils.defaultIfEmpty;

@Immutable
public class Credentials {

  private final String login;
  private final String password;

  public Credentials(String login, @Nullable String password) {
    checkArgument(login != null && !login.isEmpty(), "login must not be null nor empty");
    this.login = login;
    this.password = defaultIfEmpty(password, null);
  }

  /**
   * Non-empty login
   */
  public String getLogin() {
    return login;
  }

  /**
   * Non-empty password. {@code Optional.empty()} is returned if the password is not set
   * or initially empty. {@code Optional.of("")} is never returned.
   */
  public Optional<String> getPassword() {
    return Optional.ofNullable(password);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Credentials that = (Credentials) o;
    return login.equals(that.login) && password.equals(that.password);
  }

  @Override
  public int hashCode() {
    return Objects.hash(login, password);
  }
}
