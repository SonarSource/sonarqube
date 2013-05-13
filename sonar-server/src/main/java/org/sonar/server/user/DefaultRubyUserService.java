/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.server.user;

import org.sonar.api.user.RubyUserService;
import org.sonar.api.user.User;
import org.sonar.api.user.UserFinder;
import org.sonar.api.user.UserQuery;
import org.sonar.server.util.RubyUtils;

import java.util.List;
import java.util.Map;

public class DefaultRubyUserService implements RubyUserService {
  private final UserFinder finder;

  public DefaultRubyUserService(UserFinder finder) {
    this.finder = finder;
  }

  @Override
  public User findByLogin(String login) {
    return finder.findByLogin(login);
  }

  @Override
  public List<User> find(Map<String, Object> params) {
    UserQuery query = parseQuery(params);
    return finder.find(query);
  }

  private UserQuery parseQuery(Map<String, Object> params) {
    UserQuery.Builder builder = UserQuery.builder();
    if (RubyUtils.toBoolean(params.get("includeDeactivated")) == Boolean.TRUE) {
      builder.includeDeactivated();
    }
    builder.logins(RubyUtils.toStrings(params.get("logins")));
    return builder.build();
  }
}
