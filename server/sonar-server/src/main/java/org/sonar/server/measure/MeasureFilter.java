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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.sonar.api.measures.Metric;

public class MeasureFilter {

  // conditions on resources
  private String baseResourceKey;

  // only if baseResourceKey or baseResourceId are set
  private boolean onBaseResourceChildren = false;

  private List<String> resourceScopes = Collections.emptyList();
  private List<String> resourceQualifiers = Collections.emptyList();
  private String resourceKey = null;
  private String resourceName = null;
  private Date fromDate = null;
  private Date toDate = null;
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

  public MeasureFilter setResourceScopes(@Nullable List<String> list) {
    this.resourceScopes = sanitize(list);
    return this;
  }

  public MeasureFilter setResourceQualifiers(@Nullable List<String> list) {
    this.resourceQualifiers = sanitize(list);
    return this;
  }

  public MeasureFilter setUserFavourites(boolean b) {
    this.userFavourites = b;
    return this;
  }

  public boolean isOnFavourites() {
    return userFavourites;
  }

  @CheckForNull
  public String getResourceName() {
    return resourceName;
  }

  public MeasureFilter setResourceName(@Nullable String s) {
    this.resourceName = s;
    return this;
  }

  public String getResourceKey() {
    return resourceKey;
  }

  public MeasureFilter setResourceKey(String s) {
    this.resourceKey = s;
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

  public MeasureFilter setFromDate(@Nullable Date d) {
    this.fromDate = d;
    return this;
  }

  public MeasureFilter setToDate(@Nullable Date d) {
    this.toDate = d;
    return this;
  }

  @CheckForNull
  public Date getFromDate() {
    return fromDate;
  }

  @CheckForNull
  public Date getToDate() {
    return toDate;
  }

  public List<String> getResourceScopes() {
    return resourceScopes;
  }

  public List<String> getResourceQualifiers() {
    return resourceQualifiers;
  }

  public List<MeasureFilterCondition> getMeasureConditions() {
    return measureConditions;
  }

  MeasureFilterSort sort() {
    return sort;
  }

  public boolean isEmpty() {
    return resourceQualifiers.isEmpty() && resourceScopes.isEmpty() && StringUtils.isEmpty(baseResourceKey) && !userFavourites;
  }

  @VisibleForTesting
  static List<String> sanitize(@Nullable List<String> list) {
    return isEmptyList(list) ? Collections.emptyList() : Lists.newArrayList(list);
  }

  private static boolean isEmptyList(@Nullable List<String> list) {
    boolean blank = false;
    if (list == null || list.isEmpty() || (list.size() == 1 && Strings.isNullOrEmpty(list.get(0)))) {
      blank = true;
    }
    return blank;
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this);
  }
}
