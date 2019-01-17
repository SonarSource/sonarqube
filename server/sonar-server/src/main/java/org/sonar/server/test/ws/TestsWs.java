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
package org.sonar.server.test.ws;

import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.ws.RemovedWebServiceHandler;

public class TestsWs implements WebService {

  @Override
  public void define(Context context) {
    NewController controller = context.createController("api/tests")
      .setSince("4.4")
      .setDescription("Removed in 7.6");

    controller.createAction("covered_files")
      .setDescription("This web API is no longer supported")
      .setSince("4.4")
      .setDeprecatedSince("5.6")
      .setChangelog(new Change("7.6", "This action has been removed"))
      .setResponseExample(RemovedWebServiceHandler.INSTANCE.getResponseExample())
      .setHandler(RemovedWebServiceHandler.INSTANCE);

    controller
      .createAction("list")
      .setDescription("This web API is no longer supported")
      .setSince("5.2")
      .setDeprecatedSince("5.6")
      .setChangelog(new Change("7.6", "This action has been removed"))
      .setResponseExample(RemovedWebServiceHandler.INSTANCE.getResponseExample())
      .setHandler(RemovedWebServiceHandler.INSTANCE);

    controller.done();
  }

}
