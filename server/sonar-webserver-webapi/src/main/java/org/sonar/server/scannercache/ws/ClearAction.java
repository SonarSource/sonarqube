/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.server.scannercache.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.server.scannercache.ScannerCache;
import org.sonar.server.user.UserSession;

import static org.sonar.server.user.AbstractUserSession.insufficientPrivilegesException;

public class ClearAction implements ScannerCacheWsAction {
  private final UserSession userSession;
  private final ScannerCache cache;

  public ClearAction(UserSession userSession, ScannerCache cache) {
    this.userSession = userSession;
    this.cache = cache;
  }

  @Override
  public void define(WebService.NewController context) {
    context.createAction("clear")
      .setInternal(true)
      .setPost(true)
      .setDescription("Clear the scanner's cached data for all projects and branches. Requires global administration permission. ")
      .setSince("9.4")
      .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    checkPermission();
    cache.clear();
    response.noContent();
  }

  private void checkPermission() {
    if (!userSession.hasPermission(GlobalPermission.ADMINISTER)) {
      throw insufficientPrivilegesException();
    }
  }
}
