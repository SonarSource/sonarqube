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
package util.user;

import com.google.gson.Gson;
import java.util.List;

@Deprecated
public class Users {

  private List<User> users;

  private Users(List<User> users) {
    this.users = users;
  }

  public List<User> getUsers() {
    return users;
  }

  public static Users parse(String json) {
    Gson gson = new Gson();
    return gson.fromJson(json, Users.class);
  }

  public static class User {
    private final String login;
    private final String name;
    private final String email;
    private final String externalIdentity;
    private final String externalProvider;
    private final List<String> groups;
    private final List<String> scmAccounts;
    private final boolean active;
    private final boolean local;
    private int tokensCount;

    private User(String login, String name, String email, String externalIdentity, String externalProvider, List<String> groups, List<String> scmAccounts,
                 boolean active, boolean local, int tokensCount) {
      this.login = login;
      this.name = name;
      this.externalIdentity = externalIdentity;
      this.externalProvider = externalProvider;
      this.email = email;
      this.groups = groups;
      this.scmAccounts = scmAccounts;
      this.active = active;
      this.tokensCount = tokensCount;
      this.local = local;
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

    public List<String> getGroups() {
      return groups;
    }

    public List<String> getScmAccounts() {
      return scmAccounts;
    }

    public boolean isActive() {
      return active;
    }

    public boolean isLocal() {
      return local;
    }

    public int getTokensCount() {
      return tokensCount;
    }

    public String getExternalIdentity() {
      return externalIdentity;
    }

    public String getExternalProvider() {
      return externalProvider;
    }
  }
}
