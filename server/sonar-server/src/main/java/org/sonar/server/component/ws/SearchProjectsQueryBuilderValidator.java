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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.sonar.core.util.stream.Collectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.component.ws.SearchProjectsQueryBuilder.SearchProjectsCriteriaQuery;

import static org.sonar.server.component.ws.SearchProjectsQueryBuilder.SearchProjectsCriteriaQuery.MetricCriteria;

public class SearchProjectsQueryBuilderValidator {

  private final DbClient dbClient;

  public SearchProjectsQueryBuilderValidator(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  public void validate(DbSession dbSession, SearchProjectsCriteriaQuery query) {
    List<String> metricKeys = new ArrayList<>(query.getMetricCriterias().stream().map(MetricCriteria::getMetricKey).collect(Collectors.toSet()));
    List<MetricDto> metricDtos = dbClient.metricDao().selectByKeys(dbSession, metricKeys);
    if (metricDtos.size() == metricKeys.size()) {
      return;
    }
    List<String> metricDtoKeys = metricDtos.stream().map(MetricDto::getKey).collect(Collectors.toList());
    Set<String> unknownKeys = metricKeys.stream().filter(metricKey -> !metricDtoKeys.contains(metricKey)).collect(Collectors.toSet());
    throw new IllegalArgumentException(String.format("Unknown metric(s) %s", unknownKeys));
  }
}
