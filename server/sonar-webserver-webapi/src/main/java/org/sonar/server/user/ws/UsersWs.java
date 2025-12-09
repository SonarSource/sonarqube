/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.user.ws;

import java.util.Collection;
import org.sonar.api.server.ws.WebService;

public class UsersWs implements WebService {

  static final String API_USERS = "api/users";
  static final String DESCRIPTION = "Manage users.";
  static final String SINCE_VERSION = "3.6";

  private final Collection<BaseUsersWsAction> usersWsActions;

  public UsersWs(Collection<BaseUsersWsAction> usersWsActions) {
    this.usersWsActions = usersWsActions;
  }

  @Override
  public void define(Context context) {
    NewController controller = context.createController(API_USERS)
      .setSince(SINCE_VERSION)
      .setDescription(DESCRIPTION);

    usersWsActions.forEach(action -> action.define(controller));
    controller.done();
  }
}
