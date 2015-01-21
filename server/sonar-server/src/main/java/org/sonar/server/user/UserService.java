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

import org.sonar.api.ServerComponent;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.server.user.index.UserDoc;
import org.sonar.server.user.index.UserIndex;
import org.sonar.server.user.index.UserIndexer;

import javax.annotation.CheckForNull;

public class UserService implements ServerComponent {

  private final UserIndexer userIndexer;
  private final UserIndex userIndex;
  private final UserUpdater userUpdater;

  public UserService(UserIndexer userIndexer, UserIndex userIndex, UserUpdater userUpdater) {
    this.userIndexer = userIndexer;
    this.userIndex = userIndex;
    this.userUpdater = userUpdater;
  }

  public boolean create(NewUser newUser) {
    checkPermission();
    boolean result = userUpdater.create(newUser);
    userIndexer.index();
    return result;
  }

  public void update(UpdateUser updateUser) {
    checkPermission();
    userUpdater.update(updateUser);
    userIndexer.index();
  }

  public UserDoc getByLogin(String login) {
    return userIndex.getByLogin(login);
  }

  @CheckForNull
  public UserDoc getNullableByLogin(String login) {
    return userIndex.getNullableByLogin(login);
  }

  public void index() {
    userIndexer.index();
  }

  private void checkPermission() {
    UserSession userSession = UserSession.get();
    userSession.checkLoggedIn();
    userSession.checkGlobalPermission(GlobalPermissions.SYSTEM_ADMIN);
  }
}
