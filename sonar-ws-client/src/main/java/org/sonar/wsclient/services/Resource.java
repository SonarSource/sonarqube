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
package org.sonar.wsclient.services;

import java.util.Collections;
import java.util.Date;
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
  private Date date;
  private List<Measure> measures;

  // periods used for variations and tracking of violations
  private String period1Mode, period2Mode, period3Mode, period4Mode, period5Mode;
  private String period1Param, period2Param, period3Param, period4Param, period5Param;
  private Date period1Date, period2Date, period3Date, period4Date, period5Date;

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

  public Date getDate() {
    return date;
  }

  public Resource setDate(Date d) {
    this.date = d;
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

  /**
   * @since 2.5 only on projects, else null
   */
  public String getPeriod1Mode() {
    return period1Mode;
  }

  /**
   * @since 2.5
   */
  public Resource setPeriod1Mode(String period1Mode) {
    this.period1Mode = period1Mode;
    return this;
  }

  /**
   * @since 2.5 only on projects, else null
   */
  public String getPeriod2Mode() {
    return period2Mode;
  }

  /**
   * @since 2.5
   */
  public Resource setPeriod2Mode(String period2Mode) {
    this.period2Mode = period2Mode;
    return this;
  }

  /**
   * @since 2.5 only on projects, else null
   */
  public String getPeriod3Mode() {
    return period3Mode;
  }

  /**
   * @since 2.5
   */
  public Resource setPeriod3Mode(String period3Mode) {
    this.period3Mode = period3Mode;
    return this;
  }

  /**
   * @since 2.5 only on projects, else null
   */
  public String getPeriod4Mode() {
    return period4Mode;
  }

  /**
   * @since 2.5
   */
  public Resource setPeriod4Mode(String period4Mode) {
    this.period4Mode = period4Mode;
    return this;
  }

  /**
   * @since 2.5 only on projects, else null
   */
  public String getPeriod5Mode() {
    return period5Mode;
  }

  /**
   * @since 2.5
   */
  public Resource setPeriod5Mode(String period5Mode) {
    this.period5Mode = period5Mode;
    return this;
  }

  /**
   * @since 2.5 only on projects, else null
   */
  public String getPeriod1Param() {
    return period1Param;
  }

  /**
   * @since 2.5
   */
  public Resource setPeriod1Param(String period1Param) {
    this.period1Param = period1Param;
    return this;
  }

  /**
   * @since 2.5 only on projects, else null
   */
  public String getPeriod2Param() {
    return period2Param;
  }

  /**
   * @since 2.5
   */
  public Resource setPeriod2Param(String period2Param) {
    this.period2Param = period2Param;
    return this;
  }

  /**
   * @since 2.5 only on projects, else null
   */
  public String getPeriod3Param() {
    return period3Param;
  }

  /**
   * @since 2.5
   */
  public Resource setPeriod3Param(String period3Param) {
    this.period3Param = period3Param;
    return this;
  }

  /**
   * @since 2.5 only on projects, else null
   */
  public String getPeriod4Param() {
    return period4Param;
  }

  /**
   * @since 2.5
   */
  public Resource setPeriod4Param(String period4Param) {
    this.period4Param = period4Param;
    return this;
  }

  /**
   * @since 2.5 only on projects, else null
   */
  public String getPeriod5Param() {
    return period5Param;
  }

  /**
   * @since 2.5
   */
  public Resource setPeriod5Param(String period5Param) {
    this.period5Param = period5Param;
    return this;
  }

  /**
   * @since 2.5 only on projects, else null
   */
  public Date getPeriod1Date() {
    return period1Date;
  }

  /**
   * @since 2.5
   */
  public Resource setPeriod1Date(Date period1Date) {
    this.period1Date = period1Date;
    return this;
  }

  /**
   * @since 2.5 only on projects, else null
   */
  public Date getPeriod2Date() {
    return period2Date;
  }

  /**
   * @since 2.5
   */
  public Resource setPeriod2Date(Date period2Date) {
    this.period2Date = period2Date;
    return this;
  }

  /**
   * @since 2.5 only on projects, else null
   */
  public Date getPeriod3Date() {
    return period3Date;
  }

  /**
   * @since 2.5
   */
  public Resource setPeriod3Date(Date period3Date) {
    this.period3Date = period3Date;
    return this;
  }

  /**
   * @since 2.5 only on projects, else null
   */
  public Date getPeriod4Date() {
    return period4Date;
  }

  /**
   * @since 2.5
   */
  public Resource setPeriod4Date(Date period4Date) {
    this.period4Date = period4Date;
    return this;
  }

  /**
   * @since 2.5 only on projects, else null
   */
  public Date getPeriod5Date() {
    return period5Date;
  }

  /**
   * @since 2.5
   */
  public Resource setPeriod5Date(Date period5Date) {
    this.period5Date = period5Date;
    return this;
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
