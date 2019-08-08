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
package org.sonar.server.measure.custom.ws;

import org.sonar.api.PropertyType;
import org.sonar.api.measures.Metric;
import org.sonar.api.server.ServerSide;
import org.sonar.api.web.UserRole;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.measure.custom.CustomMeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.user.UserSession;
import org.sonar.server.util.TypeValidations;

@ServerSide
public class CustomMeasureValidator {
  private final TypeValidations typeValidations;

  public CustomMeasureValidator(TypeValidations typeValidations) {
    this.typeValidations = typeValidations;
  }

  public void setMeasureValue(CustomMeasureDto measure, String valueAsString, MetricDto metric) {
    Metric.ValueType metricType = Metric.ValueType.valueOf(metric.getValueType());
    switch (metricType) {
      case BOOL:
        checkAndSetBooleanMeasureValue(measure, valueAsString);
        break;
      case INT:
      case MILLISEC:
        checkAndSetIntegerMeasureValue(measure, valueAsString);
        break;
      case WORK_DUR:
        checkAndSetLongMeasureValue(measure, valueAsString);
        break;
      case FLOAT:
      case PERCENT:
      case RATING:
        checkAndSetFloatMeasureValue(measure, valueAsString);
        break;
      case LEVEL:
        checkAndSetLevelMeasureValue(measure, valueAsString);
        break;
      case STRING:
      case DATA:
      case DISTRIB:
        measure.setTextValue(valueAsString);
        break;
      default:
        throw new IllegalArgumentException("Unsupported metric type:" + metricType.name());
    }
  }

  private void checkAndSetLevelMeasureValue(CustomMeasureDto measure, String valueAsString) {
    typeValidations.validate(valueAsString, PropertyType.METRIC_LEVEL.name(), null);
    measure.setTextValue(valueAsString);
  }

  private void checkAndSetFloatMeasureValue(CustomMeasureDto measure, String valueAsString) {
    typeValidations.validate(valueAsString, PropertyType.FLOAT.name(), null);
    measure.setValue(Double.parseDouble(valueAsString));
  }

  private void checkAndSetLongMeasureValue(CustomMeasureDto measure, String valueAsString) {
    typeValidations.validate(valueAsString, PropertyType.LONG.name(), null);
    measure.setValue(Long.parseLong(valueAsString));
  }

  private void checkAndSetIntegerMeasureValue(CustomMeasureDto measure, String valueAsString) {
    typeValidations.validate(valueAsString, PropertyType.INTEGER.name(), null);
    measure.setValue(Integer.parseInt(valueAsString));
  }

  private void checkAndSetBooleanMeasureValue(CustomMeasureDto measure, String valueAsString) {
    typeValidations.validate(valueAsString, PropertyType.BOOLEAN.name(), null);
    measure.setValue(Boolean.parseBoolean(valueAsString) ? 1.0d : 0.0d);
  }

  public static void checkPermissions(UserSession userSession, ComponentDto component) {
    userSession.checkLoggedIn().checkComponentPermission(UserRole.ADMIN, component);
  }
}
