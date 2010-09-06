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
package org.sonar.api.web.gwt.client.webservices;

import java.util.List;

public class Resource extends ResponsePOJO {
  public static final String SCOPE_SET = "PRJ";
  public static final String SCOPE_SPACE = "DIR";
  public static final String SCOPE_ENTITY = "FIL";

  @Deprecated
  public static final String SCOPE_PROJECT = SCOPE_SET;
  @Deprecated
  public static final String SCOPE_DIRECTORY = SCOPE_SPACE;
  @Deprecated
  public static final String SCOPE_FILE = SCOPE_ENTITY;

  public static final String QUALIFIER_PROJECT = "TRK";
  public static final String QUALIFIER_MODULE = "BRC";
  @Deprecated
  public static final String QUALIFIER_PROJECT_TRUNK = QUALIFIER_PROJECT;
  @Deprecated
  public static final String QUALIFIER_PROJECT_BRANCH = QUALIFIER_MODULE;
  public static final String QUALIFIER_PACKAGE = "PAC";
  public static final String QUALIFIER_DIRECTORY = "DIR";
  public static final String QUALIFIER_FILE = "FIL";
  public static final String QUALIFIER_CLASS = "CLA";
  public static final String QUALIFIER_UNIT_TEST = "UTS";

  private Integer id;
  private String key;
  private String name;
  private String longName;
  private String qualifier;
  private String scope;
  private String language;
  private Integer copy;
  private List<Measure> measures;

  public Resource() {
  }

  public Resource(Integer id, String key, String name, String scope, String qualifier, String language, Integer copy, List<Measure> measures) {
    this.id = id;
    this.key = key;
    this.name = name;
    this.qualifier = qualifier;
    this.scope = scope;
    this.language = language;
    this.measures = measures;
    this.copy = copy;
  }

  public Integer getId() {
    return id;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getName() {
    return name;
  }

  public String getName(boolean longFormatIfDefined) {
    if (longFormatIfDefined && longName != null && !"".equals(longName)) {
      return longName;
    }
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getLongName() {
    return longName;
  }

  public void setLongName(String longName) {
    this.longName = longName;
  }

  public String getQualifier() {
    return qualifier;
  }

  public void setQualifier(String qualifier) {
    this.qualifier = qualifier;
  }

  public String getScope() {
    return scope;
  }

  public void setScope(String scope) {
    this.scope = scope;
  }

  public String getLanguage() {
    return language;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  public Integer getCopy() {
    return copy;
  }

  public void setCopy(Integer copy) {
    this.copy = copy;
  }

  public List<Measure> getMeasures() {
    return measures;
  }

  public Measure getMeasure(WSMetrics.Metric metric) {
    if (measures != null) {
      for (Measure measure : measures) {
        if (measure.getMetric().equals(metric.getKey())) {
          return measure;
        }
      }
    }
    return null;
  }

  public boolean hasMeasure(WSMetrics.Metric metric) {
    return getMeasure(metric) != null;
  }

  public String getMeasureFormattedValue(WSMetrics.Metric metric, String defaultValue) {
    Measure measure = getMeasure(metric);
    if (measure != null) {
      return measure.getFormattedValue();
    }
    return defaultValue;
  }

  public void setMeasures(List<Measure> measures) {
    this.measures = measures;
  }

  public boolean matchesKey(String resourceKey) {
    return resourceKey != null && (getId().toString().equals(resourceKey) || getKey().equals(resourceKey));
  }

  @Override
  public String toString() {
    return "Resource{" +
        "id='" + id + '\'' +
        ", key='" + key + '\'' +
        ", name='" + name + '\'' +
        ", longName='" + longName + '\'' +
        ", scope='" + scope + '\'' +
        ", qualifier='" + qualifier + '\'' +
        ", language='" + language + '\'' +
        ", measures=" + measures +
        '}';
  }
}
