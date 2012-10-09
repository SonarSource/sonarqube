/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.core.measure;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.sonar.api.ServerComponent;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.MetricFinder;
import org.sonar.api.utils.DateUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MeasureFilterDecoder implements ServerComponent {
  private MetricFinder metricFinder;
  private JSONParser parser = new JSONParser();

  public MeasureFilterDecoder(MetricFinder metricFinder) {
    this.metricFinder = metricFinder;
  }

  public MeasureFilter decode(String text) throws ParseException {
    MeasureFilter filter = new MeasureFilter();
    JSONObject map = (JSONObject) parser.parse(text);
    parseResourceConditions(filter, map);
    parseMeasureConditions(map, filter);
    parseSorting(map, filter);
    return filter;
  }

  private void parseResourceConditions(MeasureFilter filter, JSONObject map) {
    filter.setBaseResourceKey((String) map.get("base"));
    if (map.containsKey("onBaseChildren")) {
      filter.setOnBaseResourceChildren(((Boolean) map.get("onBaseChildren")).booleanValue());
    }
    filter.setResourceScopes((List<String>) map.get("scopes"));
    filter.setResourceQualifiers((List<String>) map.get("qualifiers"));
    filter.setResourceLanguages((List<String>) map.get("languages"));
    filter.setResourceName((String) map.get("name"));

    if (map.containsKey("fromDate")) {
      filter.setFromDate(toDate(map, "fromDate"));
    }
    if (map.containsKey("afterDays")) {
      filter.setFromDate(toDays(map, "afterDays"));
    }
    if (map.containsKey("toDate")) {
      filter.setToDate(toDate(map, "toDate"));
    }
    if (map.containsKey("beforeDays")) {
      filter.setToDate(toDays(map, "beforeDays"));
    }
    if (map.containsKey("favourites")) {
      filter.setUserFavourites(((Boolean) map.get("favourites")).booleanValue());
    }
  }

  private void parseSorting(JSONObject map, MeasureFilter filter) {
    if (map.containsKey("sortAsc")) {
      filter.setSortAsc(((Boolean) map.get("sortAsc")).booleanValue());
    }
    String s = (String) map.get("sortField");
    if (s != null) {
      filter.setSortOn(MeasureFilterSort.Field.valueOf(s));
    }
    s = (String) map.get("sortField");
    if (s != null) {
      filter.setSortOn(MeasureFilterSort.Field.valueOf((String) map.get("sortField")));
    }
    s = (String) map.get("sortMetric");
    if (s != null) {
      filter.setSortOnMetric(metricFinder.findByKey(s));
    }
    if (map.containsKey("sortPeriod")) {
      filter.setSortOnPeriod(((Long) map.get("sortPeriod")).intValue());
    }
  }

  private void parseMeasureConditions(JSONObject map, MeasureFilter filter) {
    JSONArray conditions = (JSONArray) map.get("conditions");
    if (conditions != null) {
      for (Object obj : conditions) {
        JSONObject c = (JSONObject) obj;
        Metric metric = metricFinder.findByKey((String) c.get("metric"));
        String operator = (String) c.get("op");
        Double value = (Double) c.get("val");
        MeasureFilterCondition condition = new MeasureFilterCondition(metric, operator, value);
        if (c.containsKey("period")) {
          condition.setPeriod(((Long) c.get("period")).intValue());
        }
        filter.addCondition(condition);
      }
    }
  }

  private static Date toDate(JSONObject map, String key) {
    String date = (String) map.get(key);
    if (date != null) {
      return DateUtils.parseDate(date);
    }
    return null;
  }

  private static Date toDays(JSONObject map, String key) {
    int days = ((Long) map.get(key)).intValue();
    Date date = org.apache.commons.lang.time.DateUtils.truncate(new Date(), Calendar.DATE);
    date = org.apache.commons.lang.time.DateUtils.addDays(date, -days);
    return date;
  }

}
