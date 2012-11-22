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

import org.apache.commons.lang.StringUtils;
import org.sonar.api.ServerComponent;
import org.sonar.api.measures.MetricFinder;
import org.sonar.api.utils.DateUtils;

import javax.annotation.Nullable;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class MeasureFilterFactory implements ServerComponent {

  private MetricFinder metricFinder;

  public MeasureFilterFactory(MetricFinder metricFinder) {
    this.metricFinder = metricFinder;
  }

  public MeasureFilter create(Map<String, Object> properties) {
    MeasureFilter filter = new MeasureFilter();
    filter.setBaseResourceKey((String)properties.get("base"));
    filter.setResourceScopes((List<String>)properties.get("scopes"));
    filter.setResourceQualifiers((List<String>)(properties.get("qualifiers")));
    filter.setResourceLanguages((List<String>)(properties.get("languages")));
    if (properties.containsKey("onBaseChildren")) {
      filter.setOnBaseResourceChildren(Boolean.valueOf((String)properties.get("onBaseChildren")));
    }
    filter.setResourceName((String)properties.get("nameRegexp"));
    filter.setResourceKeyRegexp((String)properties.get("keyRegexp"));
    if (properties.containsKey("fromDate")) {
      filter.setFromDate(toDate((String)properties.get("fromDate")));
    } else if (properties.containsKey("afterDays")) {
      filter.setFromDate(toDays((String)properties.get("afterDays")));
    }
    if (properties.containsKey("toDate")) {
      filter.setToDate(toDate((String)properties.get("toDate")));
    } else if (properties.containsKey("beforeDays")) {
      filter.setToDate(toDays((String)properties.get("beforeDays")));
    }

    if (properties.containsKey("favourites")) {
      filter.setUserFavourites(Boolean.valueOf((String)properties.get("favourites")));
    }
    if (properties.containsKey("asc")) {
      filter.setSortAsc(Boolean.valueOf((String)properties.get("asc")));
    }
    String s = (String)properties.get("sort");
    if (s != null) {
      if (StringUtils.startsWith(s, "metric:")) {
        filter.setSortOnMetric(metricFinder.findByKey(StringUtils.substringAfter(s, "metric:")));
      } else {
        filter.setSortOn(MeasureFilterSort.Field.valueOf(s.toUpperCase()));
      }
    }
//    if (map.containsKey("sortPeriod")) {
//      filter.setSortOnPeriod(((Long) map.get("sortPeriod")).intValue());
//    }
    return filter;
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
