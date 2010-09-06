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
package org.sonar.wsclient.services;

import java.util.Collections;
import java.util.List;

public class Resource extends Model {

  /* SCOPES */
  public static final String SCOPE_SET = "PRJ";
  public static final String SCOPE_SPACE = "DIR";
  public static final String SCOPE_ENTITY = "FIL";
  
  /* QUALIFIERS */
  public static final String QUALIFIER_VIEW = "VW";
  public static final String QUALIFIER_SUBVIEW = "SVW";
  public static final String QUALIFIER_LIB = "LIB";
  public static final String QUALIFIER_PROJECT = "TRK";
  public static final String QUALIFIER_MODULE = "BRC";
  public static final String QUALIFIER_PACKAGE = "PAC";
  public static final String QUALIFIER_DIRECTORY = "DIR";
  public static final String QUALIFIER_FILE = "FIL";
  public static final String QUALIFIER_CLASS = "CLA";
  public static final String QUALIFIER_UNIT_TEST_CLASS = "UTS";

  /* LANGUAGES */
  public static final String LANGUAGE_JAVA = "java";

  private Integer id;
  private String key;
  private String name;
  private String longName;
  private String scope;
  private String qualifier;
  private String language;
  private String version;
  private Integer copy;
  private String description;
  private List<Measure> measures;

  public Integer getId() {
    return id;
  }

  public Resource setId(Integer id) {
    this.id = id;
    return this;
  }

  public String getKey() {
    return key;
  }

  public Resource setKey(String key) {
    this.key = key;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public Resource setDescription(String description) {
    this.description = description;
    return this;
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

  public String getLongName() {
    return longName;
  }

  public Resource setLongName(String longName) {
    this.longName = longName;
    return this;
  }

  public Resource setName(String s) {
    this.name = s;
    return this;
  }

  public String getScope() {
    return scope;
  }

  public Resource setScope(String scope) {
    this.scope = scope;
    return this;
  }

  public String getQualifier() {
    return qualifier;
  }

  public Resource setQualifier(String qualifier) {
    this.qualifier = qualifier;
    return this;
  }

  public String getLanguage() {
    return language;
  }

  public Resource setLanguage(String language) {
    this.language = language;
    return this;
  }

  public String getVersion() {
    return version;
  }

  public Resource setVersion(String version) {
    this.version = version;
    return this;
  }

  public Integer getCopy() {
    return copy;
  }

  public Resource setCopy(Integer copy) {
    this.copy = copy;
    return this;
  }

  public List<Measure> getMeasures() {
    if (measures == null) {
      return Collections.emptyList();
    }
    return measures;
  }

  public Measure getMeasure(String metricKey) {
    for (Measure measure : getMeasures()) {
      if (metricKey.equals(measure.getMetricKey())) {
        return measure;
      }
    }
    return null;
  }

  public Double getMeasureValue(String metricKey) {
    Measure measure = getMeasure(metricKey);
    if (measure != null) {
      return measure.getValue();
    }
    return null;
  }

  public Integer getMeasureIntValue(String metricKey) {
    Double d = getMeasureValue(metricKey);
    if (d != null) {
      return d.intValue();
    }
    return null;
  }

  public String getMeasureFormattedValue(String metricKey, String defaultValue) {
    Measure measure = getMeasure(metricKey);
    if (measure != null) {
      return measure.getFormattedValue(defaultValue);
    }
    return defaultValue;
  }

  public void setMeasures(List<Measure> measures) {
    this.measures = measures;
  }

  @Override
  public String toString() {
    return new StringBuilder()
        .append("[id=")
        .append(id)
        .append(",key=")
        .append(key)
        .append("]")
        .toString();
  }
}
