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
package org.sonar.server.measure;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.MetricFinder;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static java.util.Collections.singletonList;

@ServerSide
public class MeasureFilterFactory {

  private final MetricFinder metricFinder;
  private final System2 system;

  public MeasureFilterFactory(MetricFinder metricFinder, System2 system) {
    this.metricFinder = metricFinder;
    this.system = system;
  }

  public MeasureFilter create(Map<String, Object> properties) {
    MeasureFilter filter = new MeasureFilter();
    filter.setBaseResourceKey((String) properties.get("base"));
    filter.setResourceScopes(toList(properties.get("scopes")));
    filter.setResourceQualifiers(toList(properties.get("qualifiers")));
    MeasureFilterCondition condition = alertToCondition(toList(properties.get("alertLevels")));
    if (condition != null) {
      filter.addCondition(condition);
    }
    String onBaseComponents = (String) properties.get("onBaseComponents");
    if (onBaseComponents != null) {
      filter.setOnBaseResourceChildren(Boolean.valueOf(onBaseComponents));
    }
    filter.setResourceName(toString(properties.get("nameSearch")));
    filter.setResourceKey((String) properties.get("keySearch"));
    String onFavourites = (String) properties.get("onFavourites");
    if (onFavourites != null) {
      filter.setUserFavourites(Boolean.valueOf(onFavourites));
    }
    fillDateConditions(filter, properties);
    fillSorting(filter, properties);
    fillMeasureConditions(properties, filter);
    return filter;
  }

  private void fillDateConditions(MeasureFilter filter, Map<String, Object> properties) {
    String fromDate = (String) properties.get("fromDate");
    if (fromDate != null) {
      filter.setFromDate(toDate(fromDate));
    } else {
      filter.setFromDate(toDays((String) properties.get("ageMaxDays")));
    }
    String toDate = (String) properties.get("toDate");
    if (toDate != null) {
      filter.setToDate(toDate(toDate));
    } else {
      filter.setToDate(toDays((String) properties.get("ageMinDays")));
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
        String sort = s.toUpperCase();
        if (sortFieldLabels().contains(sort)) {
          filter.setSortOn(MeasureFilterSort.Field.valueOf(sort));
        }
      }
    }

    if (properties.containsKey("asc")) {
      filter.setSortAsc(Boolean.valueOf((String) properties.get("asc")));
    }
  }

  private static List<String> sortFieldLabels() {
    return newArrayList(Iterables.transform(Arrays.asList(MeasureFilterSort.Field.values()), FieldToName.INSTANCE));
  }

  @CheckForNull
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

  @CheckForNull
  private MeasureFilterCondition alertToCondition(@Nullable List<String> alertLevels) {
    if (alertLevels == null || alertLevels.isEmpty()) {
      return null;
    }
    List<String> availableLevels = transform(Arrays.asList(Metric.Level.values()), MetricLevelToName.INSTANCE);
    List<String> alertLevelsUppercase = transform(alertLevels, new AlertLevelToUppercase(availableLevels));
    String val = "('" + Joiner.on("', '").skipNulls().join(alertLevelsUppercase) + "')";
    Metric metric = metricFinder.findByKey(CoreMetrics.ALERT_STATUS_KEY);
    if (metric != null) {
      MeasureFilterCondition.Operator operator = MeasureFilterCondition.Operator.fromCode("in");
      return new MeasureFilterCondition(metric, operator, val);
    }
    return null;
  }

  private static List<String> toList(@Nullable Object obj) {
    List<String> result = null;
    if (obj != null) {
      if (obj instanceof String) {
        result = singletonList((String) obj);
      } else {
        result = (List<String>) obj;
      }
    }
    return result;
  }

  @CheckForNull
  private static Date toDate(@Nullable String date) {
    if (date != null) {
      return DateUtils.parseDate(date);
    }
    return null;
  }

  @CheckForNull
  private Date toDays(@Nullable String s) {
    if (s != null) {
      int days = Integer.parseInt(s);
      Date date = org.apache.commons.lang.time.DateUtils.truncate(new Date(system.now()), Calendar.DATE);
      date = org.apache.commons.lang.time.DateUtils.addDays(date, -days);
      return date;
    }
    return null;
  }

  @CheckForNull
  public static String toString(@Nullable Object o) {
    if (o != null) {
      if (o instanceof List) {
        return Joiner.on(",").join((List) o);
      } else if (o instanceof String[]) {
        // assume that it contains only strings
        return Joiner.on(",").join((String[]) o);
      } else {
        return o.toString();
      }
    }
    return null;
  }

  private enum FieldToName implements Function<MeasureFilterSort.Field, String> {
    INSTANCE;

    @Override
    public String apply(@Nullable MeasureFilterSort.Field input) {
      return input != null ? input.name() : null;
    }
  }

  private enum MetricLevelToName implements Function<Metric.Level, String> {
    INSTANCE;

    @Override
    public String apply(@Nullable Metric.Level input) {
      return input != null ? input.name() : null;
    }
  }

  private static class AlertLevelToUppercase implements Function<String, String> {
    private final List<String> availableLevels;

    public AlertLevelToUppercase(List<String> availableLevels) {
      this.availableLevels = availableLevels;
    }

    @Override
    public String apply(@Nullable String input) {
      return input != null && availableLevels.contains(input.toUpperCase()) ? input.toUpperCase() : null;
    }
  }
}
