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
import com.google.common.collect.Sets;
import org.sonar.api.measures.Metric;

import java.util.Date;
import java.util.List;
import java.util.Set;

public class MeasureFilter {
  // conditions on resources
  private String baseResourceKey;
  private boolean onBaseResourceChildren = false; // only if baseResourceKey is set
  private Set<String> resourceScopes = Sets.newHashSet();
  private Set<String> resourceQualifiers = Sets.newHashSet();
  private Set<String> resourceLanguages = Sets.newHashSet();
  private String resourceName;
  private Date fromDate = null, toDate = null;
  private boolean userFavourites = false;

  // conditions on measures
  private List<MeasureFilterValueCondition> measureConditions = Lists.newArrayList();

  // sort
  private MeasureFilterSort sort = new MeasureFilterSort();

  public String baseResourceKey() {
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

  public MeasureFilter setResourceScopes(Set<String> resourceScopes) {
    this.resourceScopes = resourceScopes;
    return this;
  }

  public MeasureFilter setResourceQualifiers(String... qualifiers) {
    this.resourceQualifiers = Sets.newHashSet(qualifiers);
    return this;
  }

  public MeasureFilter setResourceLanguages(String... languages) {
    this.resourceLanguages = Sets.newHashSet(languages);
    return this;
  }

  public MeasureFilter setUserFavourites(boolean b) {
    this.userFavourites = b;
    return this;
  }

  public boolean userFavourites() {
    return userFavourites;
  }

  public String resourceName() {
    return resourceName;
  }

  public MeasureFilter setResourceName(String s) {
    this.resourceName = s;
    return this;
  }

  public MeasureFilter addCondition(MeasureFilterValueCondition condition) {
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

  public Date fromDate() {
    return fromDate;
  }

  public Date toDate() {
    return toDate;
  }

  public Set<String> resourceScopes() {
    return resourceScopes;
  }

  public Set<String> resourceQualifiers() {
    return resourceQualifiers;
  }

  public Set<String> resourceLanguages() {
    return resourceLanguages;
  }

  public List<MeasureFilterValueCondition> measureConditions() {
    return measureConditions;
  }

  MeasureFilterSort sort() {
    return sort;
  }

}
