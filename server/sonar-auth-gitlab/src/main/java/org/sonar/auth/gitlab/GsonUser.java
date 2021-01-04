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
package org.sonar.auth.gitlab;

import com.google.gson.Gson;

/**
 * Lite representation of JSON response of GET https://gitlab.com/api/v4/user
 */
public class GsonUser {
  private long id;
  private String username;
  private String name;
  private String email;

  public GsonUser() {
    // even if empty constructor is not required for Gson, it is strongly
    // recommended:
    // http://stackoverflow.com/a/18645370/229031
  }

  public long getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }

  public String getName() {
    return name;
  }

  public String getEmail() {
    return email;
  }

  public static GsonUser parse(String json) {
    Gson gson = new Gson();
    return gson.fromJson(json, GsonUser.class);
  }
}
