/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.component.ws;

import org.sonar.api.server.ws.WebService;
import org.sonar.server.ws.RemovedWebServiceHandler;

public class ResourcesWs implements WebService {

  @Override
  public void define(Context context) {
    NewController controller = context.createController("api/resources")
      .setDescription("Removed since 6.3, please use api/components and api/measures instead")
      .setSince("2.10");
    defineIndexAction(controller);
    controller.done();
  }

  private static void defineIndexAction(NewController controller) {
    controller.createAction("index")
      .setDescription("The web service is removed and you're invited to use the alternatives: " +
        "<ul>" +
        "<li>if you need one component without measures: api/components/show</li>" +
        "<li>if you need one component with measures: api/measures/component</li>" +
        "<li>if you need several components without measures: api/components/tree</li>" +
        "<li>if you need several components with measures: api/measures/component_tree</li>" +
        "</ul>")
      .setSince("2.10")
      .setDeprecatedSince("5.4")
      .setHandler(RemovedWebServiceHandler.INSTANCE)
      .setResponseExample(RemovedWebServiceHandler.INSTANCE.getResponseExample());
  }

}
