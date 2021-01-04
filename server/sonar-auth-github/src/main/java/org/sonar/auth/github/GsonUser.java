/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
package org.sonar.auth.github;

import com.google.gson.Gson;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

/**
 * Lite representation of JSON response of GET https://api.github.com/user
 */
public class GsonUser {
  private String id;
  private String login;
  private String name;
  private String email;

  public GsonUser() {
    // even if empty constructor is not required for Gson, it is strongly
    // recommended:
    // http://stackoverflow.com/a/18645370/229031
  }

  public GsonUser(String id, String login, @Nullable String name, @Nullable String email) {
    this.id = id;
    this.login = login;
    this.name = name;
    this.email = email;
  }

  public String getId() {
    return id;
  }

  public String getLogin() {
    return login;
  }

  /**
   * Name is optional at GitHub
   */
  @CheckForNull
  public String getName() {
    return name;
  }

  /**
   * Name is optional at GitHub
   */
  @CheckForNull
  public String getEmail() {
    return email;
  }

  public static GsonUser parse(String json) {
    Gson gson = new Gson();
    return gson.fromJson(json, GsonUser.class);
  }
}
