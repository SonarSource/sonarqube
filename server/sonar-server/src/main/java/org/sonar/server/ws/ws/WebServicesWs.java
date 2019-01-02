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
package org.sonar.server.ws.ws;

import java.util.List;
import org.sonar.api.server.ws.WebService;

/**
 * This web service lists all the existing web services, including itself,
 * for documentation usage.
 *
 * @since 4.2
 */
public class WebServicesWs implements WebService {
  private final List<WebServicesWsAction> actions;

  public WebServicesWs(List<WebServicesWsAction> actions) {
    this.actions = actions;
  }

  @Override
  public void define(final Context context) {
    NewController controller = context
      .createController("api/webservices")
      .setSince("4.2")
      .setDescription("Get information on the web api supported on this instance.");

    actions.forEach(action -> {
      action.define(controller);
      action.setContext(context);
    });

    controller.done();
  }
}
