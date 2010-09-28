/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.server.filters;

import com.google.common.collect.Sets;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.resources.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Filter {

  // path
  private Integer rootSnapshotId;
  private Integer baseSnapshotId;
  private String baseSnapshotPath;

  // filters on resources
  private Set<String> scopes;
  private Set<String> qualifiers;
  private Set<String> languages;
  private Set<Integer> favouriteIds;
  private DateCriterion dateCriterion;
  private String keyRegexp;
  private String nameRegexp;
  private boolean isViewContext = false;

  // filters on measures
  private List<MeasureCriterion> measureCriteria = new ArrayList<MeasureCriterion>();

  // sorting
  private Integer sortedMetricId;
  private boolean sortedByLanguage;
  private boolean sortedByName;
  private boolean sortedByDate;
  private boolean sortedByVersion;
  private boolean useMeasureValueToSort = true; // only if sortedMetricId is not null
  private boolean ascendingSort = true;

  public Filter setPath(Integer rootSnapshotId, Integer snapshotId, String snapshotPath, boolean isViewContext) {
    this.baseSnapshotId = snapshotId;
    if (rootSnapshotId==null) {
      this.rootSnapshotId = snapshotId;
    } else {
      this.rootSnapshotId = rootSnapshotId;
    }      
    this.baseSnapshotPath = snapshotPath;
    this.isViewContext = isViewContext;
    return this;
  }

  public Integer getRootSnapshotId() {
    return rootSnapshotId;
  }

  public boolean hasBaseSnapshot() {
    return rootSnapshotId != null && baseSnapshotId != null && baseSnapshotPath != null;
  }

  public Integer getBaseSnapshotId() {
    return baseSnapshotId;
  }

  public String getBaseSnapshotPath() {
    return baseSnapshotPath;
  }

  public boolean isViewContext() {
    return isViewContext;
  }

  public void setViewContext(boolean b) {
    isViewContext = b;
  }

  public Set<String> getScopes() {
    return scopes;
  }

  public boolean hasScopes() {
    return scopes != null && !scopes.isEmpty();
  }

  public Filter setScopes(Set<String> scopes) {
    this.scopes = scopes;
    return this;
  }

  public Filter setScopes(String... scopes) {
    this.scopes = Sets.newHashSet(scopes);
    return this;
  }

  public Set<String> getQualifiers() {
    return qualifiers;
  }

  public boolean hasQualifiers() {
    return qualifiers != null && qualifiers.size() > 0;
  }

  public Filter setQualifiers(Set<String> qualifiers) {
    this.qualifiers = qualifiers;
    return this;
  }

  public Filter setQualifiers(String... qualifiers) {
    this.qualifiers = Sets.newHashSet(qualifiers);
    return this;
  }

  public Set<String> getLanguages() {
    return languages;
  }

  public boolean hasLanguages() {
    return languages!=null && !languages.isEmpty();
  }

  public Filter setLanguages(Set<String> languages) {
    this.languages = languages;
    return this;
  }

  public Filter setLanguages(String... languages) {
    this.languages = Sets.newHashSet(languages);
    return this;
  }

  public Set<Integer> getFavouriteIds() {
    return favouriteIds;
  }

  public boolean hasFavouriteIds() {
    return favouriteIds!=null && !favouriteIds.isEmpty();
  }

  public Filter setFavouriteIds(Set<Integer> favouriteIds) {
    this.favouriteIds = favouriteIds;
    return this;
  }

  public Filter setFavouriteIds(Integer... favouriteIds) {
    this.favouriteIds = Sets.newHashSet(favouriteIds);
    return this;
  }

  public Integer getSortedMetricId() {
    return sortedMetricId;
  }

  public boolean useMeasureValueToSort() {
    return useMeasureValueToSort;
  }

  public Filter setSortedMetricId(Integer id) {
    return setSortedMetricId(id, true);
  }

  public Filter setSortedMetricId(Integer id, boolean useValue) {
    unsetSorts();
    this.sortedMetricId = id;
    this.useMeasureValueToSort = useValue;
    return this;
  }

  public boolean isSortedByLanguage() {
    return sortedByLanguage;
  }

  public Filter setSortedByLanguage() {
    unsetSorts();
    this.sortedByLanguage = true;
    return this;
  }

  public boolean isSortedByName() {
    return sortedByName;
  }

  public boolean isSortedByVersion() {
    return sortedByVersion;
  }

  public Filter setSortedByVersion() {
    unsetSorts();
    this.sortedByVersion = true;
    return this;
  }

  public boolean isSorted() {
    return isSortedByLanguage() || isSortedByName() || isSortedByDate() || isSortedByVersion() || getSortedMetricId() != null;
  }

  public boolean isSortedByDate() {
    return sortedByDate;
  }

  public Filter setSortedByDate() {
    unsetSorts();
    sortedByDate = true;
    return this;
  }

  public Filter setSortedByName() {
    unsetSorts();
    this.sortedByName = true;
    return this;
  }

  private void unsetSorts() {
    this.sortedByDate = false;
    this.sortedByLanguage = false;
    this.sortedByName = false;
    this.sortedMetricId = null;
    this.sortedByVersion = false;
    this.useMeasureValueToSort = true;
  }

  public List<MeasureCriterion> getMeasureCriteria() {
    return measureCriteria;
  }

  public Filter setMeasureCriteria(List<MeasureCriterion> l) {
    this.measureCriteria = l;
    return this;
  }

  public Filter addMeasureCriterion(MeasureCriterion c) {
    this.measureCriteria.add(c);
    return this;
  }

  public Filter createMeasureCriterionOnValue(Integer metricId, String operator, Double value) {
    this.measureCriteria.add(new MeasureCriterion(metricId, operator, value));
    return this;
  }

  public boolean hasMeasureCriteria() {
    return !measureCriteria.isEmpty();
  }

  protected boolean hasMeasureCriteriaOnMetric(Integer metricId) {
    if (metricId!=null) {
      for (MeasureCriterion criterion : measureCriteria) {
        if (metricId.equals(criterion.getMetricId())) {
          return true;
        }
      }
    }
    return false;
  }

  public boolean mustJoinMeasuresTable() {
    return sortedMetricId != null || hasMeasureCriteria();
  }

  public boolean isAscendingSort() {
    return ascendingSort;
  }

  public Filter setAscendingSort(boolean b) {
    this.ascendingSort = b;
    return this;
  }

  public DateCriterion getDateCriterion() {
    return dateCriterion;
  }

  public Filter setDateCriterion(DateCriterion dc) {
    this.dateCriterion = dc;
    return this;
  }

  public Filter setDateCriterion(String operator, Integer daysAgo) {
    this.dateCriterion = new DateCriterion().setOperator(operator).setDate(daysAgo);
    return this;
  }

  public String getKeyRegexp() {
    return keyRegexp;
  }

  public Filter setKeyRegexp(String s) {
    this.keyRegexp = s;
    return this;
  }

  public String getNameRegexp() {
    return nameRegexp;
  }

  public Filter setNameRegexp(String s) {
    this.nameRegexp = s;
    return this;
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);    
  }

  public static Filter createForAllQualifiers() {
    return new Filter().setQualifiers(Resource.QUALIFIER_VIEW, Resource.QUALIFIER_SUBVIEW,
        Resource.QUALIFIER_PROJECT, Resource.QUALIFIER_MODULE, Resource.QUALIFIER_DIRECTORY, Resource.QUALIFIER_PACKAGE,
        Resource.QUALIFIER_FILE, Resource.QUALIFIER_CLASS, Resource.QUALIFIER_UNIT_TEST_CLASS, Resource.QUALIFIER_LIB);
  }
}
