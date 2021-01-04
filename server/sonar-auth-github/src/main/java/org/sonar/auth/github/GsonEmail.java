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
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

/**
 * Lite representation of JSON response of GET https://api.github.com/user/emails
 */
public class GsonEmail {

  private String email;
  private boolean verified;
  private boolean primary;

  public GsonEmail() {
    // http://stackoverflow.com/a/18645370/229031
    this("", false, false);
  }

  public GsonEmail(String email, boolean verified, boolean primary) {
    this.email = email;
    this.verified = verified;
    this.primary = primary;
  }

  public String getEmail() {
    return email;
  }

  public boolean isVerified() {
    return verified;
  }

  public boolean isPrimary() {
    return primary;
  }

  public static List<GsonEmail> parse(String json) {
    Type collectionType = new TypeToken<Collection<GsonEmail>>() {
    }.getType();
    Gson gson = new Gson();
    return gson.fromJson(json, collectionType);
  }
}
