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

import java.util.LinkedHashMap;
import java.util.Map;

public class Measure extends Model {

  private String metricKey;
  private String metricName;
  private Double value;
  private String formattedValue;
  private String data;
  private String characteristicKey;
  private String characteristicName;

  private Integer trend;
  private Integer var;

  private String ruleKey;
  private String ruleName;
  private String ruleCategory;
  private String rulePriority;

  public String getMetricKey() {
    return metricKey;
  }

  public Measure setMetricKey(String metricKey) {
    this.metricKey = metricKey;
    return this;
  }

  public String getMetricName() {
    return metricName;
  }

  public Measure setMetricName(String metricName) {
    this.metricName = metricName;
    return this;
  }

  public Double getValue() {
    return value;
  }

  public Integer getIntValue() {
    if (value==null) {
      return null;
    }
    return value.intValue();
  }

  public Measure setValue(Double value) {
    this.value = value;
    return this;
  }

  public String getFormattedValue() {
    return formattedValue;
  }

  public String getFormattedValue(String defaultValue) {
    if (formattedValue==null) {
      return defaultValue;
    }
    return formattedValue;
  }

  public Measure setFormattedValue(String formattedValue) {
    this.formattedValue = formattedValue;
    return this;
  }

  public String getData() {
    return data;
  }

  public Map<String,String> getDataAsMap() {
    return getDataAsMap(",");
  }

  public Map<String,String> getDataAsMap(String separator) {
    if (data==null) {
      return null;
    }
    Map<String,String> map = new LinkedHashMap<String,String>();
    String[] parts = data.split(separator);
    for (String part : parts) {
      String[] kv = part.split("=");
      map.put(kv[0], kv[1]);
    }
    return map;
  }

  public Measure setData(String data) {
    this.data = data;
    return this;
  }

  public Integer getTrend() {
    return trend;
  }

  public Measure setTrend(Integer trend) {
    this.trend = trend;
    return this;
  }

  public Integer getVar() {
    return var;
  }

  public Measure setVar(Integer var) {
    this.var = var;
    return this;
  }

  public String getRuleKey() {
    return ruleKey;
  }

  public Measure setRuleKey(String ruleKey) {
    this.ruleKey = ruleKey;
    return this;
  }

  public String getRuleName() {
    return ruleName;
  }

  public Measure setRuleName(String ruleName) {
    this.ruleName = ruleName;
    return this;
  }

  public String getRuleCategory() {
    return ruleCategory;
  }

  public Measure setRuleCategory(String ruleCategory) {
    this.ruleCategory = ruleCategory;
    return this;
  }

  public String getRulePriority() {
    return rulePriority;
  }

  public Measure setRulePriority(String rulePriority) {
    this.rulePriority = rulePriority;
    return this;
  }

  public String getCharacteristicKey() {
    return characteristicKey;
  }

  public String getCharacteristicName() {
    return characteristicName;
  }

  public Measure setCharacteristicKey(String s) {
    this.characteristicKey = s;
    return this;
  }

  public Measure setCharacteristicName(String s) {
    this.characteristicName = s;
    return this;
  }
}
