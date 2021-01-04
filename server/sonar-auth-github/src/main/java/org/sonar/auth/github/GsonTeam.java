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
 * Lite representation of JSON response of GET https://api.github.com/user/teams
 */
public class GsonTeam {

  private String slug;
  private GsonOrganization organization;

  public GsonTeam() {
    // http://stackoverflow.com/a/18645370/229031
    this("", new GsonOrganization());
  }

  public GsonTeam(String slug, GsonOrganization organization) {
    this.slug = slug;
    this.organization = organization;
  }

  public String getId() {
    return slug;
  }

  public String getOrganizationId() {
    return organization.getLogin();
  }

  public static List<GsonTeam> parse(String json) {
    Type collectionType = new TypeToken<Collection<GsonTeam>>() {
    }.getType();
    Gson gson = new Gson();
    return gson.fromJson(json, collectionType);
  }

  public static class GsonOrganization {
    private String login;

    public GsonOrganization() {
      // http://stackoverflow.com/a/18645370/229031
      this("");
    }

    public GsonOrganization(String login) {
      this.login = login;
    }

    public String getLogin() {
      return login;
    }
  }
}
