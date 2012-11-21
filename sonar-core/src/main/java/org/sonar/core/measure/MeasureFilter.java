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

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.measures.Metric;
import org.sonar.api.utils.DateUtils;

import javax.annotation.Nullable;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class MeasureFilter {
  private static final String[] EMPTY = {};

  // conditions on resources
  private String baseResourceKey;
  private boolean onBaseResourceChildren = false; // only if getBaseResourceKey is set
  private String[] resourceScopes = EMPTY;
  private String[] resourceQualifiers = EMPTY;
  private String[] resourceLanguages = EMPTY;
  private String resourceKeyRegexp;
  private String resourceName;
  private Date fromDate = null, toDate = null;
  private boolean userFavourites = false;

  // conditions on measures
  private List<MeasureFilterCondition> measureConditions = Lists.newArrayList();

  // sort
  private MeasureFilterSort sort = new MeasureFilterSort();

  public String getBaseResourceKey() {
    return baseResourceKey;
  }

  public MeasureFilter setBaseResourceKey(String s) {
    this.baseResourceKey = s;
    return this;
  }

  public MeasureFilter setOnBaseResourceChildren(boolean b) {
    this.onBaseResourceChildren = b;
    return this;
  }

  public boolean isOnBaseResourceChildren() {
    return onBaseResourceChildren;
  }

  public MeasureFilter setResourceScopes(String... l) {
    this.resourceScopes = (l != null ? l : EMPTY);
    return this;
  }

  public MeasureFilter setResourceQualifiers(String... l) {
    this.resourceQualifiers = l;
    return this;
  }

  public MeasureFilter setResourceLanguages(String... l) {
    this.resourceLanguages = (l != null ? l : EMPTY);
    return this;
  }

  public MeasureFilter setResourceScopes(@Nullable List<String> l) {
    this.resourceScopes = (l != null ? l.toArray(new String[l.size()]) : EMPTY);
    return this;
  }

  public MeasureFilter setResourceQualifiers(@Nullable List<String> l) {
    this.resourceQualifiers = (l != null ? l.toArray(new String[l.size()]) : EMPTY);
    return this;
  }

  public MeasureFilter setResourceLanguages(@Nullable List<String> l) {
    this.resourceLanguages = (l != null ? l.toArray(new String[l.size()]) : EMPTY);
    return this;
  }

  public MeasureFilter setUserFavourites(boolean b) {
    this.userFavourites = b;
    return this;
  }

  public boolean isOnFavourites() {
    return userFavourites;
  }

  public String getResourceName() {
    return resourceName;
  }

  public MeasureFilter setResourceName(String s) {
    this.resourceName = s;
    return this;
  }

  public String getResourceKeyRegexp() {
    return resourceKeyRegexp;
  }

  public MeasureFilter setResourceKeyRegexp(String s) {
    this.resourceKeyRegexp = s;
    return this;
  }

  public MeasureFilter addCondition(MeasureFilterCondition condition) {
    this.measureConditions.add(condition);
    return this;
  }

  public MeasureFilter setSortOn(MeasureFilterSort.Field sortField) {
    this.sort.setField(sortField);
    return this;
  }

  public MeasureFilter setSortAsc(boolean b) {
    this.sort.setAsc(b);
    return this;
  }

  public MeasureFilter setSortOnMetric(Metric m) {
    this.sort.setField(MeasureFilterSort.Field.METRIC);
    this.sort.setMetric(m);
    return this;
  }

  public MeasureFilter setSortOnPeriod(int period) {
    this.sort.setPeriod(period);
    return this;
  }

  public MeasureFilter setFromDate(Date d) {
    this.fromDate = d;
    return this;
  }

  public MeasureFilter setToDate(Date d) {
    this.toDate = d;
    return this;
  }

  public Date getFromDate() {
    return fromDate;
  }

  public Date getToDate() {
    return toDate;
  }

  public String[] getResourceScopes() {
    return resourceScopes;
  }

  public String[] getResourceQualifiers() {
    return resourceQualifiers;
  }

  public String[] getResourceLanguages() {
    return resourceLanguages;
  }

  public List<MeasureFilterCondition> getMeasureConditions() {
    return measureConditions;
  }

  MeasureFilterSort sort() {
    return sort;
  }

  public static MeasureFilter create(Map<String, String> properties) {
    MeasureFilter filter = new MeasureFilter();
    filter.setBaseResourceKey(properties.get("base"));
    filter.setResourceScopes(toArray(properties.get("scopes")));
    filter.setResourceQualifiers(toArray(properties.get("qualifiers")));
    filter.setResourceLanguages(toArray(properties.get("languages")));
    if (properties.containsKey("onBaseChildren")) {
      filter.setOnBaseResourceChildren(Boolean.valueOf(properties.get("onBaseChildren")));
    }
    filter.setResourceName(properties.get("nameRegexp"));
    filter.setResourceKeyRegexp(properties.get("keyRegexp"));
    if (properties.containsKey("fromDate")) {
      filter.setFromDate(toDate(properties.get("fromDate")));
    } else if (properties.containsKey("afterDays")) {
      filter.setFromDate(toDays(properties.get("afterDays")));
    }
    if (properties.containsKey("toDate")) {
      filter.setToDate(toDate(properties.get("toDate")));
    } else if (properties.containsKey("beforeDays")) {
      filter.setToDate(toDays(properties.get("beforeDays")));
    }

    if (properties.containsKey("favourites")) {
      filter.setUserFavourites(Boolean.valueOf(properties.get("favourites")));
    }
    return filter;
  }

  private static String[] toArray(@Nullable String s) {
    if (s == null) {
      return EMPTY;
    }
    return StringUtils.split(s, ",");
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
