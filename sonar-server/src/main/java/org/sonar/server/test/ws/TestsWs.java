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

package org.sonar.server.test.ws;

import com.google.common.io.Resources;
import org.sonar.api.server.ws.RailsHandler;
import org.sonar.api.server.ws.WebService;

public class TestsWs implements WebService {

  @Override
  public void define(Context context) {
    NewController controller = context.createController("api/tests")
      .setSince("3.5")
      .setDescription("Tests management");

    definePlanAction(controller);
    defineTestableAction(controller);

    controller.done();
  }

  private void definePlanAction(NewController controller) {
    NewAction action = controller.createAction("plan")
      .setDescription("Get the details of a given test plan : test cases, resources covered by test cases. Requires Browse permission on resource")
      .setSince("3.5")
      .setInternal(true)
      .setHandler(RailsHandler.INSTANCE)
      .setResponseExample(Resources.getResource(this.getClass(), "tests-example-plan.json"));

    action.createParam("resource")
      .setRequired(true)
      .setDescription("id or key of the test resource")
      .setExampleValue("org.codehaus.sonar.plugins:sonar-cpd-plugin:src/test/java/org/sonar/plugins/cpd/SonarBridgeEngineTest.java");
    RailsHandler.addJsonOnlyFormatParam(action);
  }

  private void defineTestableAction(NewController controller) {
    NewAction action = controller.createAction("testable")
      .setDescription("Get the details of a given resource : test plan, test cases covering lines. Requires Browse permission on resource")
      .setSince("3.5")
      .setInternal(true)
      .setHandler(RailsHandler.INSTANCE)
      .setResponseExample(Resources.getResource(this.getClass(), "tests-example-testable.json"));

    action.createParam("resource")
      .setRequired(true)
      .setDescription("id or key of the resource")
      .setExampleValue("org.codehaus.sonar.plugins:sonar-cpd-plugin:src/main/java/org/sonar/plugins/cpd/SonarBridgeEngine.java");
    RailsHandler.addJsonOnlyFormatParam(action);
  }

}
