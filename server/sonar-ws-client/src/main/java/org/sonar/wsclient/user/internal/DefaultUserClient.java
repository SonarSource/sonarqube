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
package org.sonar.wsclient.user.internal;

import org.json.simple.JSONValue;
import org.sonar.wsclient.internal.HttpRequestFactory;
import org.sonar.wsclient.user.User;
import org.sonar.wsclient.user.UserClient;
import org.sonar.wsclient.user.UserParameters;
import org.sonar.wsclient.user.UserQuery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Do not instantiate this class, but use {@link org.sonar.wsclient.SonarClient#userClient()}.
 */
public class DefaultUserClient implements UserClient {

  private static final String BASE_URL = "/api/users/";
  private static final String SEARCH_URL = BASE_URL + "search";
  private static final String CREATE_URL = BASE_URL + "create";
  private static final String UPDATE_URL = BASE_URL + "update";
  private static final String DEACTIVATE_URL = BASE_URL + "deactivate";

  private final HttpRequestFactory requestFactory;

  /**
   * For internal use. Use {@link org.sonar.wsclient.SonarClient} to get an instance.
   */
  public DefaultUserClient(HttpRequestFactory requestFactory) {
    this.requestFactory = requestFactory;
  }

  @Override
  public List<User> find(UserQuery query) {
    String json = requestFactory.get(SEARCH_URL, query.urlParams());
    List<User> result = new ArrayList<User>();
    Map jsonRoot = (Map) JSONValue.parse(json);
    List<Map> jsonUsers = (List<Map>) jsonRoot.get("users");
    if (jsonUsers != null) {
      for (Map jsonUser : jsonUsers) {
        result.add(new User(jsonUser));
      }
    }
    return result;
  }

  @Override
  public User create(UserParameters userParameters) {
    String json = requestFactory.post(CREATE_URL, userParameters.urlParams());
    Map jsonRoot = (Map) JSONValue.parse(json);
    Map jsonUser = (Map) jsonRoot.get("user");
    return new User(jsonUser);
  }

  @Override
  public User update(UserParameters userParameters) {
    String json = requestFactory.post(UPDATE_URL, userParameters.urlParams());
    Map jsonRoot = (Map) JSONValue.parse(json);
    Map jsonUser = (Map) jsonRoot.get("user");
    return new User(jsonUser);
  }

  @Override
  public void deactivate(String login) {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("login", login);
    requestFactory.post(DEACTIVATE_URL, params);
  }
}
