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
package org.sonar.server.config.ws;

import org.sonar.api.server.ws.RailsHandler;
import org.sonar.api.server.ws.WebService;

public class PropertiesWs implements WebService {

  @Override
  public void define(Context context) {
    NewController controller = context.createController("api/properties");
    controller.setDescription("Properties management");
    controller.setSince("2.6");

    defineIndexAction(controller);

    controller.done();
  }

  private void defineIndexAction(NewController controller) {
    controller.createAction("index")
      .setDescription("Documentation of this web service is available <a href=\"http://redirect.sonarsource.com/doc/old-web-service-api.html\">here</a>")
      .setResponseExample(getClass().getResource("index-example.xml"))
      .setSince("2.6")
      .setHandler(RailsHandler.INSTANCE);
  }
}
