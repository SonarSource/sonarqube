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

import com.google.common.io.Resources;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.WebService;

class MetricWs implements WebService {
  @Override
  public void define(Context context) {
    NewController newController = context
      .createController("api/metric")
      .setDescription("Metrics")
      .setSince("3.2");

    // action with default values
    newController.createAction("show")
      .setSince("3.2")
      .setDescription("Show Description")
      .setResponseExample(getClass().getResource("web-services-ws-test.txt"))
      .setHandler((request, response) -> {
      });

    // action with a lot of overridden values
    NewAction create = newController.createAction("create")
      .setPost(true)
      .setDescription("Create metric")
      .setSince("4.1")
      .setDeprecatedSince("5.3")
      .setResponseExample(Resources.getResource(getClass(), "ListActionTest/metrics_example.json"))
      .setChangelog(
        new Change("4.5", "Deprecate database ID in response"),
        new Change("6.0", "Remove database ID from response"),
        new Change("6.6", "Requires 'Administer System' permission instead of 'Administer Quality Profiles'"))
      .setHandler((request, response) -> {
      });

    create
      .createParam("severity")
      .setDescription("Severity")
      .setSince("4.4")
      .setDeprecatedSince("5.2")
      .setDeprecatedKey("old_severity", "4.6")
      .setRequired(false)
      .setPossibleValues("BLOCKER", "INFO")
      .setExampleValue("INFO")
      .setDefaultValue("BLOCKER");
    create.createParam("name");
    create.createParam("internal").setInternal(true);

    create
      .createParam("constrained_string_param")
      .setMaximumLength(64)
      .setMinimumLength(3);

    create
      .createParam("constrained_numeric_param")
      .setMaximumValue(12);

    newController.createAction("internal_action")
      .setDescription("Internal Action Description")
      .setResponseExample(getClass().getResource("web-services-ws-test.txt"))
      .setSince("5.3")
      .setInternal(true)
      .setHandler((request, response) -> {
      });

    newController.done();
  }
}
