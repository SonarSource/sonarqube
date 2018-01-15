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
package org.sonar.server.sticker.ws;

import com.google.common.io.Resources;
import org.apache.commons.io.IOUtils;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;

import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;

public class MeasureAction implements StickersWsAction {

  public static final String PARAM_COMPONENT = "component";
  public static final String PARAM_METRIC = "metric";

  @Override
  public void define(WebService.NewController controller) {
    NewAction action = controller.createAction("measure")
      .setHandler(this)
      .setDescription("Generate badge for measure as an SVG")
      .setResponseExample(Resources.getResource(getClass(), "measure-example.svg"));
    action.createParam(PARAM_COMPONENT)
      .setDescription("Project key")
      .setRequired(true)
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);
    action.createParam(PARAM_METRIC)
      .setDescription("Metric key")
      .setRequired(true)
      .setPossibleValues(
        CoreMetrics.ALERT_STATUS_KEY,
        CoreMetrics.COVERAGE_KEY,
        CoreMetrics.RELIABILITY_RATING_KEY,
        CoreMetrics.SECURITY_RATING_KEY,
        CoreMetrics.SQALE_RATING_KEY,
        CoreMetrics.BUGS_KEY,
        CoreMetrics.VULNERABILITIES_KEY,
        CoreMetrics.CODE_SMELLS_KEY,
        CoreMetrics.DUPLICATED_LINES_DENSITY_KEY,
        CoreMetrics.TECHNICAL_DEBT_KEY,
        CoreMetrics.TESTS_KEY
        )
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    response.stream().setMediaType("image/svg+xml");
    IOUtils.copy(Resources.getResource(getClass(), "measure-example.svg").openStream(), response.stream().output());
  }

}
