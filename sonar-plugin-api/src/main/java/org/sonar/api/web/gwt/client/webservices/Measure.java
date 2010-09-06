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

import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

public class Measure {
  private String metric;
  private String metricName;
  private Double value;
  private String formattedValue;
  private String data;

  private String ruleKey;
  private String ruleName;
  private String ruleCategory;
  private String rulePriority;
  
  private Date date;

  public Measure() {
  }

  public Measure(String metric, Double value, String formattedValue) {
    this.metric = metric;
    this.value = value;
    this.formattedValue = formattedValue;
  }

  public String getMetric() {
    return metric;
  }

  public void setMetric(String metric) {
    this.metric = metric;
  }

  public Double getValue() {
    return value;
  }

  public void setValue(Double value) {
    this.value = value;
  }

  public String getFormattedValue() {
    return formattedValue;
  }

  public void setFormattedValue(String formattedValue) {
    this.formattedValue = formattedValue;
  }

  public String getData() {
    return data;
  }

  public Map<String, String> getDataAsMap() {
    Map<String, String> map = new TreeMap<String, String>();
    if (data != null) {
      String[] strings = data.split(";");
      for (String string : strings) {
        String[] keyValue = string.split("=");
        map.put(keyValue[0], keyValue[1]);
      }
    }
    return map;

  }

  public void setData(String data) {
    this.data = data;
  }

  public String getMetricName() {
    return metricName;
  }

  public void setMetricName(String metricName) {
    this.metricName = metricName;
  }

  public String getRuleKey() {
    return ruleKey;
  }

  public void setRuleKey(String s) {
    this.ruleKey = s;
  }

  public String getRuleName() {
    return ruleName;
  }

  public void setRuleName(String ruleName) {
    this.ruleName = ruleName;
  }

  public String getRuleCategory() {
    return ruleCategory;
  }

  public void setRuleCategory(String s) {
    this.ruleCategory = s;
  }

  public String getRulePriority() {
    return rulePriority;
  }

  public void setRulePriority(String rulePriority) {
    this.rulePriority = rulePriority;
  }
  
  public Date getDate() {
    return date;
  }

  public void setDate(Date date) {
    this.date = date;
  }

  @Override
  public String toString() {
    return "Measure{" +
        "metric='" + metric + '\'' +
        ", metric_name='" + metricName + '\'' +
        ", val='" + value + '\'' +
        ", f_val='" + formattedValue + '\'' +
        '}';
  }

}
