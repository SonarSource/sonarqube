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

import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.server.ws.WebService.NewParam;

import static org.sonarqube.ws.client.measure.MeasuresWsParameters.ADDITIONAL_FIELDS;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_ADDITIONAL_FIELDS;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_METRIC_KEYS;

class MeasuresWsParametersBuilder {

  private MeasuresWsParametersBuilder() {
    // prevent instantiation
  }

  static NewParam createAdditionalFieldsParameter(NewAction action) {
    return action.createParam(PARAM_ADDITIONAL_FIELDS)
      .setDescription("Comma-separated list of additional fields that can be returned in the response.")
      .setPossibleValues(ADDITIONAL_FIELDS)
      .setExampleValue("periods,metrics");
  }

  static NewParam createMetricKeysParameter(NewAction action) {
    return action.createParam(PARAM_METRIC_KEYS)
      .setDescription("Metric keys")
      .setRequired(true)
      .setExampleValue("ncloc,complexity,violations");
  }

}
