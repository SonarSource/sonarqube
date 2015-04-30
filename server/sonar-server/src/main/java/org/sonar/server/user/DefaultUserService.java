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
package org.sonar.server.user;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import org.sonar.api.user.RubyUserService;
import org.sonar.api.user.User;
import org.sonar.api.user.UserFinder;
import org.sonar.api.user.UserQuery;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.user.index.UserDoc;
import org.sonar.server.user.index.UserIndex;
import org.sonar.server.util.RubyUtils;

import javax.annotation.CheckForNull;

import java.util.List;
import java.util.Map;

public class DefaultUserService implements RubyUserService {

  private final UserIndex userIndex;
  private final UserUpdater userUpdater;
  private final UserFinder finder;

  public DefaultUserService(UserIndex userIndex, UserUpdater userUpdater, UserFinder finder) {
    this.userIndex = userIndex;
    this.userUpdater = userUpdater;
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

  private UserQuery parseQuery(Map<String, Object> params) {
    UserQuery.Builder builder = UserQuery.builder();
    if (RubyUtils.toBoolean(params.get("includeDeactivated")) == Boolean.TRUE) {
      builder.includeDeactivated();
    }
    builder.logins(RubyUtils.toStrings(params.get("logins")));
    builder.searchText((String) params.get("s"));
    return builder.build();
  }

  @CheckForNull
  public UserDoc getByLogin(String login) {
    return userIndex.getNullableByLogin(login);
  }

  public boolean create(Map<String, Object> params) {
    NewUser newUser = NewUser.create()
      .setLogin((String) params.get("login"))
      .setName((String) params.get("name"))
      .setEmail((String) params.get("email"))
      .setScmAccounts(RubyUtils.toStrings(params.get("scm_accounts")))
      .setPassword((String) params.get("password"))
      .setPasswordConfirmation((String) params.get("password_confirmation"));
    return userUpdater.create(newUser);
  }

  public void update(Map<String, Object> params) {
    UpdateUser updateUser = UpdateUser.create((String) params.get("login"));
    if (params.containsKey("name")) {
      updateUser.setName((String) params.get("name"));
    }
    if (params.containsKey("email")) {
      updateUser.setEmail((String) params.get("email"));
    }
    if (params.containsKey("scm_accounts")) {
      updateUser.setScmAccounts(RubyUtils.toStrings(params.get("scm_accounts")));
    }
    if (params.containsKey("password")) {
      updateUser.setPassword((String) params.get("password"));
      updateUser.setPasswordConfirmation((String) params.get("password_confirmation"));
    }
    userUpdater.update(updateUser);
  }

  public void deactivate(String login) {
    if (Strings.isNullOrEmpty(login)) {
      throw new BadRequestException("Login is missing");
    }
    UserSession userSession = UserSession.get();
    userSession.checkGlobalPermission(GlobalPermissions.SYSTEM_ADMIN);
    if (Objects.equal(userSession.login(), login)) {
      throw new BadRequestException("Self-deactivation is not possible");
    }
    userUpdater.deactivateUserByLogin(login);
  }

  public void index() {
    userUpdater.index();
  }
}
