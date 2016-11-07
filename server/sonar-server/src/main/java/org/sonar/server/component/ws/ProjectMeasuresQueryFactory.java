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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.measures.Metric.Level;
import org.sonar.api.resources.Qualifiers;
import org.sonar.core.util.stream.Collectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.property.PropertyQuery;
import org.sonar.server.component.es.ProjectMeasuresQuery;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Locale.ENGLISH;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;
import static org.sonar.server.component.es.ProjectMeasuresQuery.MetricCriterion;
import static org.sonar.server.component.es.ProjectMeasuresQuery.Operator;

public class ProjectMeasuresQueryFactory {
  private static final Splitter CRITERIA_SPLITTER = Splitter.on(Pattern.compile("and", Pattern.CASE_INSENSITIVE));
  private static final Pattern CRITERIA_PATTERN = Pattern.compile("(\\w+)\\s*([<>]?[=]?)\\s*(\\w+)");
  private static final String IS_FAVORITE_CRITERION = "isFavorite";

  private final DbClient dbClient;
  private final UserSession userSession;

  public ProjectMeasuresQueryFactory(DbClient dbClient, UserSession userSession) {
    this.dbClient = dbClient;
    this.userSession = userSession;
  }

  ProjectMeasuresQuery newProjectMeasuresQuery(DbSession dbSession, String filter) {
    if (StringUtils.isBlank(filter)) {
      return new ProjectMeasuresQuery();
    }

    ProjectMeasuresQuery query = new ProjectMeasuresQuery();

    CRITERIA_SPLITTER.split(filter)
      .forEach(criteria -> processCriterion(dbSession, criteria, query));
    return query;
  }

  private void processCriterion(DbSession dbSession, String rawCriterion, ProjectMeasuresQuery query) {
    String criterion = rawCriterion.trim();

    try {
      if (IS_FAVORITE_CRITERION.equalsIgnoreCase(criterion)) {
        query.setProjectUuids(searchFavoriteUuids(dbSession));
        return;
      }

      Matcher matcher = CRITERIA_PATTERN.matcher(criterion);
      checkArgument(matcher.find() && matcher.groupCount() == 3, "Criterion should be 'isFavourite' or criterion should have a metric, an operator and a value");
      String metric = matcher.group(1).toLowerCase(ENGLISH);
      Operator operator = Operator.getByValue(matcher.group(2));
      String value = matcher.group(3);
      if (ALERT_STATUS_KEY.equals(metric)) {
        checkArgument(operator.equals(Operator.EQ), "Only equals operator is available for quality gate criteria");
        query.setQualityGateStatus(Level.valueOf(value));
      } else {
        double doubleValue = Double.parseDouble(matcher.group(3));
        query.addMetricCriterion(new MetricCriterion(metric, operator, doubleValue));
      }
    } catch (Exception e) {
      throw new IllegalArgumentException(String.format("Invalid criterion '%s'", criterion), e);
    }
  }

  private List<String> searchFavoriteUuids(DbSession dbSession) {
    List<Long> favoriteDbIds = dbClient.propertiesDao().selectByQuery(PropertyQuery.builder()
      .setUserId(userSession.getUserId())
      .setKey("favourite")
      .build(), dbSession)
      .stream()
      .map(PropertyDto::getResourceId)
      .collect(Collectors.toList());

    return dbClient.componentDao().selectByIds(dbSession, favoriteDbIds)
      .stream()
      .filter(dbComponent -> Qualifiers.PROJECT.equals(dbComponent.qualifier()))
      .map(ComponentDto::uuid)
      .collect(Collectors.toList());
  }

}
