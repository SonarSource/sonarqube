/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.repository.user;

import com.google.common.base.Joiner;
import org.sonar.batch.bootstrap.ServerClient;
import org.sonar.batch.protocol.GsonHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class UserRepository {

  private ServerClient serverClient;

  public UserRepository(ServerClient serverClient) {
    this.serverClient = serverClient;
  }

  private static class Users {

    private List<User> users = new ArrayList<>();

    public List<User> getUsers() {
      return users;
    }
  }

  public Collection<User> loadFromWs(List<String> userLogins) {
    if (userLogins.isEmpty()) {
      return Collections.emptyList();
    }
    String url = "/api/users/search?format=json&includeDeactivated=true&logins=" + Joiner.on(',').join(userLogins);
    String json = serverClient.request(url);
    Users users = GsonHelper.create().fromJson(json, Users.class);
    return users.getUsers();
  }

}
