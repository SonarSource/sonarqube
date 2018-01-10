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
package org.sonar.server.badge.ws;

import com.google.common.io.Resources;
import org.apache.commons.io.IOUtils;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;

import static java.util.Arrays.asList;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;

public class QualityGateAction implements ProjectBadgesWsAction {

  public static final String PARAM_COMPONENT = "component";
  public static final String PARAM_TYPE = "type";

  @Override
  public void define(WebService.NewController controller) {
    NewAction action = controller.createAction("quality_gate")
      .setHandler(this)
      .setDescription("Generate badge for quality gate as an SVG")
      .setResponseExample(Resources.getResource(getClass(), "quality_gate-example.svg"));
    action.createParam(PARAM_COMPONENT)
      .setDescription("Project key")
      .setRequired(true)
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);
    action.createParam(PARAM_TYPE)
      .setDescription("Type of badge.")
      .setRequired(false)
      .setPossibleValues(asList("BADGE", "CARD"));
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    response.stream().setMediaType("image/svg+xml");
    IOUtils.copy(Resources.getResource(getClass(), "quality_gate-example.svg").openStream(), response.stream().output());
  }

}
