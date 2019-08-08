/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.qualityprofile.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.ws.RemovedWebServiceHandler;

public class RestoreBuiltInAction implements QProfileWsAction {

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("restore_built_in")
      .setDescription("This web service has no effect since 6.4. It's no more possible to restore built-in quality profiles because they are automatically " +
        "updated and read only. Returns HTTP code 410.")
      .setSince("4.4")
      .setDeprecatedSince("6.4")
      .setPost(true)
      .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    RemovedWebServiceHandler.INSTANCE.handle(request, response);
  }
}
