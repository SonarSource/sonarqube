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
package org.sonar.api.platform;

import javax.annotation.Nullable;
import org.sonar.api.ExtensionPoint;
import org.sonar.api.server.ServerSide;

import static java.util.Objects.requireNonNull;

/**
 * @since 3.2
 */
@ServerSide
@ExtensionPoint
public interface NewUserHandler {

  final class Context {
    private String login;
    private String name;
    private String email;

    private Context(String login, String name, @Nullable String email) {
      requireNonNull(login);
      requireNonNull(name);
      this.login = login;
      this.name = name;
      this.email = email;
    }

    public String getLogin() {
      return login;
    }

    public String getName() {
      return name;
    }

    public String getEmail() {
      return email;
    }

    public static Builder builder() {
      return new Builder();
    }

    public static final class Builder {
      private String login;
      private String name;
      private String email;

      private Builder() {
      }

      public Builder setLogin(String s) {
        this.login = s;
        return this;
      }

      public Builder setName(String s) {
        this.name = s;
        return this;
      }

      public Builder setEmail(@Nullable String s) {
        this.email = s;
        return this;
      }

      public Context build() {
        return new Context(login, name, email);
      }
    }
  }

  void doOnNewUser(Context context);
}
