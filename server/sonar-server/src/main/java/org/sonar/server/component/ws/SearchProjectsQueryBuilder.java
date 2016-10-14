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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.Locale.ENGLISH;
import static org.sonar.server.component.ws.SearchProjectsQueryBuilder.SearchProjectsCriteriaQuery.MetricCriteria;
import static org.sonar.server.component.ws.SearchProjectsQueryBuilder.SearchProjectsCriteriaQuery.Operator;

public class SearchProjectsQueryBuilder {

  private static final Splitter CRITERIA_SPLITTER = Splitter.on("and");
  private static final Pattern CRITERIA_PATTERN = Pattern.compile("(\\w+)\\s*([<>][=]?)\\s*(\\w+)");

  private SearchProjectsQueryBuilder() {
    // Only static methods
  }

  public static SearchProjectsCriteriaQuery build(String filter) {
    SearchProjectsCriteriaQuery query = new SearchProjectsCriteriaQuery();
    CRITERIA_SPLITTER.split(filter.toLowerCase(ENGLISH))
      .forEach(criteria -> processCriteria(criteria, query));
    return query;
  }

  private static void processCriteria(String criteria, SearchProjectsCriteriaQuery query) {
    Matcher matcher = CRITERIA_PATTERN.matcher(criteria);
    checkArgument(matcher.find() && matcher.groupCount() == 3, "Invalid criteria '%s'", criteria);
    String metric = matcher.group(1);
    Operator operator = Operator.create(matcher.group(2));
    Double value = Double.parseDouble(matcher.group(3));
    query.addMetricCriteria(new MetricCriteria(metric, operator, value));
  }

  public static class SearchProjectsCriteriaQuery {
    public enum Operator {
      LT("<="), GT(">"), EQ("=");

      String value;

      Operator(String value) {
        this.value = value;
      }

      String getValue() {
        return value;
      }

      public static Operator create(String value) {
        return stream(Operator.values())
          .filter(operator -> operator.getValue().equals(value))
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException(format("Unknown operator '%s'", value)));
      }
    }

    private List<MetricCriteria> metricCriterias = new ArrayList<>();

    SearchProjectsCriteriaQuery addMetricCriteria(MetricCriteria metricCriteria) {
      metricCriterias.add(metricCriteria);
      return this;
    }

    public List<MetricCriteria> getMetricCriterias() {
      return metricCriterias;
    }

    public static class MetricCriteria {
      private String metricKey;
      private Operator operator;
      private double value;

      private MetricCriteria(String metricKey, Operator operator, double value) {
        this.metricKey = metricKey;
        this.operator = operator;
        this.value = value;
      }

      public String getMetricKey() {
        return metricKey;
      }

      public Operator getOperator() {
        return operator;
      }

      public double getValue() {
        return value;
      }
    }
  }

}
