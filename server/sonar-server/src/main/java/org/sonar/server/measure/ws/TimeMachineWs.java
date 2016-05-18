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
package org.sonar.server.measure.ws;

import com.google.common.io.Resources;
import org.sonar.api.server.ws.RailsHandler;
import org.sonar.api.server.ws.WebService;

import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;

public class TimeMachineWs implements WebService {

  @Override
  public void define(Context context) {
    NewController controller = context.createController("api/timemachine")
      .setDescription("Get project measure data from past analyses.")
      .setSince("2.10");

    defineSystemAction(controller);

    controller.done();
  }

  private void defineSystemAction(NewController controller) {
    NewAction action = controller.createAction("index")
      .setDescription("Get a list of past measures. Requires Browse permission on project")
      .setSince("2.10")
      .setHandler(RailsHandler.INSTANCE)
      .setResponseExample(Resources.getResource(this.getClass(), "timemachine-example-index.json"));

    action.createParam("resource")
      .setDescription("id or key of the resource (ie: component)")
      .setRequired(true)
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);

    action.createParam("metrics")
      .setDescription("Comma-separated list of <a href=\"http://redirect.sonarsource.com/doc/metric-definitions.html\">metric keys/ids</a>")
      .setRequired(true)
      .setExampleValue("coverage,violations");

    action.createParam("fromDateTime")
      .setDescription("ISO-8601 datetime (inclusive)")
      .setExampleValue("2010-12-25T23:59:59+0100");

    action.createParam("toDateTime")
      .setDescription("ISO-8601 datetime (inclusive)")
      .setExampleValue("2010-12-25T23:59:59+0100");

    action.createParam("format")
      .setDescription("Response format")
      .setPossibleValues("json", "csv");
  }

}
