/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.core.measure;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.ServerComponent;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.MetricFinder;
import org.sonar.api.utils.DateUtils;

import javax.annotation.Nullable;

import java.util.*;

public class MeasureFilterFactory implements ServerComponent {

  private MetricFinder metricFinder;

  public MeasureFilterFactory(MetricFinder metricFinder) {
    this.metricFinder = metricFinder;
  }

  public MeasureFilter create(Map<String, Object> properties) {

    MeasureFilter filter = new MeasureFilter();
    filter.setBaseResourceKey((String) properties.get("base"));
    String baseId = "baseId";
    if (properties.containsKey(baseId)) {
      filter.setBaseResourceId(Long.valueOf((String) properties.get(baseId)));
    }
    filter.setResourceScopes(toList(properties.get("scopes")));
    filter.setResourceQualifiers(toList(properties.get("qualifiers")));
    filter.setResourceLanguages(toList(properties.get("languages")));
    MeasureFilterCondition condition = alertToCondition(toList(properties.get("alertLevels")));
    if (condition != null) {
      filter.addCondition(condition);
    }
    String onBaseComponents = "onBaseComponents";
    if (properties.containsKey(onBaseComponents)) {
      filter.setOnBaseResourceChildren(Boolean.valueOf((String) properties.get(onBaseComponents)));
    }
    filter.setResourceName((String) properties.get("nameSearch"));
    filter.setResourceKeyRegexp((String) properties.get("keyRegexp"));
    String onFavourites = "onFavourites";
    if (properties.containsKey(onFavourites)) {
      filter.setUserFavourites(Boolean.valueOf((String) properties.get(onFavourites)));
    }
    fillDateConditions(filter, properties);
    fillSorting(filter, properties);
    fillMeasureConditions(properties, filter);
    return filter;
  }

  private void fillDateConditions(MeasureFilter filter, Map<String, Object> properties) {
    String fromDate = "fromDate";
    if (properties.containsKey(fromDate)) {
      filter.setFromDate(toDate((String) properties.get(fromDate)));
    } else {
      String ageMaxDays = "ageMaxDays";
      if (properties.containsKey(ageMaxDays)) {
        filter.setFromDate(toDays((String) properties.get(ageMaxDays)));
      }
    }
    String toDate = "toDate";
    if (properties.containsKey(toDate)) {
      filter.setToDate(toDate((String) properties.get(toDate)));
    } else {
      String ageMinDays = "ageMinDays";
      if (properties.containsKey(ageMinDays)) {
        filter.setToDate(toDays((String) properties.get(ageMinDays)));
      }
    }
  }

  private void fillMeasureConditions(Map<String, Object> properties, MeasureFilter filter) {
    for (int index = 1; index <= 3; index++) {
      MeasureFilterCondition condition = toCondition(properties, index);
      if (condition != null) {
        filter.addCondition(condition);
      }
    }
  }

  private void fillSorting(MeasureFilter filter, Map<String, Object> properties) {
    String s = (String) properties.get("sort");
    if (s != null) {
      if (StringUtils.startsWith(s, "metric:")) {
        String[] fields = StringUtils.split(s, ':');
        Metric metric = metricFinder.findByKey(fields[1]);
        if (metric != null) {
          filter.setSortOnMetric(metric);
          if (fields.length == 3) {
            filter.setSortOnPeriod(Integer.parseInt(fields[2]));
          }
        }
      } else {
        filter.setSortOn(MeasureFilterSort.Field.valueOf(s.toUpperCase()));
      }
    }

    if (properties.containsKey("asc")) {
      filter.setSortAsc(Boolean.valueOf((String) properties.get("asc")));
    }
  }

  private MeasureFilterCondition toCondition(Map<String, Object> props, int index) {
    MeasureFilterCondition condition = null;
    String metricKey = (String) props.get("c" + index + "_metric");
    String op = (String) props.get("c" + index + "_op");
    String val = (String) props.get("c" + index + "_val");
    if (!Strings.isNullOrEmpty(metricKey) && !Strings.isNullOrEmpty(op) && !Strings.isNullOrEmpty(val)) {
      Metric metric = metricFinder.findByKey(metricKey);
      MeasureFilterCondition.Operator operator = MeasureFilterCondition.Operator.fromCode(op);
      condition = new MeasureFilterCondition(metric, operator, Double.parseDouble(val));
      String period = (String) props.get("c" + index + "_period");
      if (period != null) {
        condition.setPeriod(Integer.parseInt(period));
      }
    }
    return condition;
  }

  private MeasureFilterCondition alertToCondition(List<String> alertLevels) {
    if (alertLevels == null || alertLevels.isEmpty()) {
      return null;
    }
    MeasureFilterCondition condition = null;
    String metricKey = CoreMetrics.ALERT_STATUS_KEY;
    String op = "in";
    List<String> alertLevelsUppercase = Lists.transform(alertLevels, new Function<String, String>() {
      @Override
      public String apply(String input) {
        return input != null ? input.toUpperCase() : "";
      }
    });
    String val = "('" + Joiner.on("', '").join(alertLevelsUppercase) + "')";
    if (!Strings.isNullOrEmpty(metricKey) && !Strings.isNullOrEmpty(op) && !Strings.isNullOrEmpty(val)) {
      Metric metric = metricFinder.findByKey(metricKey);
      MeasureFilterCondition.Operator operator = MeasureFilterCondition.Operator.fromCode(op);
      condition = new MeasureFilterCondition(metric, operator, val);
    }
    return condition;
  }

  private List<String> toList(@Nullable Object obj) {
    List<String> result = null;
    if (obj != null) {
      if (obj instanceof String) {
        result = Arrays.asList((String) obj);
      } else {
        result = (List<String>) obj;
      }
    }
    return result;
  }

  private static Date toDate(@Nullable String date) {
    if (date != null) {
      return DateUtils.parseDate(date);
    }
    return null;
  }

  private static Date toDays(@Nullable String s) {
    if (s != null) {
      int days = Integer.valueOf(s);
      Date date = org.apache.commons.lang.time.DateUtils.truncate(new Date(), Calendar.DATE);
      date = org.apache.commons.lang.time.DateUtils.addDays(date, -days);
      return date;
    }
    return null;
  }

}
