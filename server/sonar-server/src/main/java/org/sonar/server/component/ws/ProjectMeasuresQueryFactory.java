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

package org.sonar.server.component.ws;

import com.google.common.base.Splitter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;
import org.sonar.server.component.es.ProjectMeasuresQuery;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Locale.ENGLISH;
import static org.sonar.server.component.es.ProjectMeasuresQuery.MetricCriteria;
import static org.sonar.server.component.es.ProjectMeasuresQuery.Operator;

class ProjectMeasuresQueryFactory {

  private static final Splitter CRITERIA_SPLITTER = Splitter.on("and");
  private static final Pattern CRITERIA_PATTERN = Pattern.compile("(\\w+)\\s*([<>][=]?)\\s*(\\w+)");

  private ProjectMeasuresQueryFactory() {
    // Only static methods
  }

  static ProjectMeasuresQuery newProjectMeasuresQuery(String filter) {
    if (StringUtils.isBlank(filter)) {
      return new ProjectMeasuresQuery();
    }

    ProjectMeasuresQuery query = new ProjectMeasuresQuery();

    CRITERIA_SPLITTER.split(filter.toLowerCase(ENGLISH))
      .forEach(criteria -> processCriteria(criteria, query));
    return query;
  }

  private static void processCriteria(String criteria, ProjectMeasuresQuery query) {
    Matcher matcher = CRITERIA_PATTERN.matcher(criteria);
    checkArgument(matcher.find() && matcher.groupCount() == 3, "Invalid criterion '%s'", criteria);
    String metric = matcher.group(1);
    Operator operator = Operator.getByValue(matcher.group(2));
    Double value = Double.parseDouble(matcher.group(3));
    query.addMetricCriterion(new MetricCriteria(metric, operator, value));
  }

}
