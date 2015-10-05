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

package org.sonar.server.updatecenter.ws;

import com.google.common.io.Resources;
import org.sonar.api.server.ws.RailsHandler;
import org.sonar.api.server.ws.WebService;

public class UpdateCenterWs implements WebService {

  @Override
  public void define(Context context) {
    NewController controller = context.createController("api/updatecenter")
      .setDescription("Get list of installed plugins")
      .setSince("2.10");

    defineInstalledPluginsAction(controller);

    controller.done();
  }

  private void defineInstalledPluginsAction(NewController controller) {
    NewAction action = controller.createAction("installed_plugins")
      .setDescription("Get the list of all the plugins installed on the SonarQube instance")
      .setSince("2.10")
      .setHandler(RailsHandler.INSTANCE)
      .setInternal(true)
      .setResponseExample(Resources.getResource(this.getClass(), "example-installed_plugins.json"));
    RailsHandler.addFormatParam(action);
  }

}
