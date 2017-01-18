/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.user;

import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import org.sonar.api.user.RubyUserService;
import org.sonar.api.user.User;
import org.sonar.api.user.UserFinder;
import org.sonar.api.user.UserQuery;
import org.sonar.server.util.RubyUtils;

public class DefaultUserService implements RubyUserService {

  private final UserFinder finder;

  public DefaultUserService(UserFinder finder) {
    this.finder = finder;
  }

  @Override
  @CheckForNull
  public User findByLogin(String login) {
    return finder.findByLogin(login);
  }

  @Override
  public List<User> find(Map<String, Object> params) {
    UserQuery query = parseQuery(params);
    return finder.find(query);
  }

  private static UserQuery parseQuery(Map<String, Object> params) {
    UserQuery.Builder builder = UserQuery.builder();
    if (RubyUtils.toBoolean(params.get("includeDeactivated")) == Boolean.TRUE) {
      builder.includeDeactivated();
    }
    builder.logins(RubyUtils.toStrings(params.get("logins")));
    builder.searchText((String) params.get("s"));
    return builder.build();
  }
}
