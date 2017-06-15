/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.api.server.ws;

import org.sonar.api.ExtensionPoint;
import org.sonar.api.server.ServerSide;

/**
 * Defines a web service.
 * <br>
 * <br>
 * The classes implementing this extension point must be declared by {@link org.sonar.api.Plugin}.
 * <br>
 * <h3>How to use</h3>
 * <pre>
 * public class HelloWs implements WebServiceDefinition {
 *   {@literal @}Override
 *   public Webservice.Controller define() {
 *     NewController controller = context.createController("api/hello");
 *     controller.setDescription("Web service example");
 *
 *     // create the URL /api/hello/show
 *     controller.createAction("show")
 *       .setDescription("Entry point")
 *       .setHandler(new RequestHandler() {
 *         {@literal @}Override
 *         public void handle(Request request, Response response) {
 *           // read request parameters and generates response output
 *           response.newJsonWriter()
 *             .beginObject()
 *             .prop("hello", request.mandatoryParam("key"))
 *             .endObject()
 *             .close();
 *         }
 *      })
 *      .createParam("key").setDescription("Example key").setRequired(true);
 *
 *    return controller;
 *   }
 * }
 * </pre>
 *
 * A web service can call another web service to get some data. See {@link Request#localConnector()}
 * provided by {@link RequestHandler#handle(Request, Response)}.
 *
 * @since 6.5
 */
@ServerSide
@ExtensionPoint
public interface WebServiceDefinition {
  WebService.Controller define();
}
