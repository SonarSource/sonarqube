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
  private String ruleSeverity;

  /**
   * @deprecated since 2.5 See http://jira.codehaus.org/browse/SONAR-2007
   */
  @Deprecated
  private String ruleCategory;

  private Double variation1, variation2, variation3, variation4, variation5;

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
    if (value == null) {
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
    if (formattedValue == null) {
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

  public Map<String, String> getDataAsMap() {
    return getDataAsMap(",");
  }

  public Map<String, String> getDataAsMap(String separator) {
    if (data == null) {
      return null;
    }
    Map<String, String> map = new LinkedHashMap<String, String>();
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

  /**
   * @deprecated since 2.5 See http://jira.codehaus.org/browse/SONAR-2007
   */
  @Deprecated
  public String getRuleCategory() {
    return ruleCategory;
  }

  /**
   * @deprecated since 2.5 See http://jira.codehaus.org/browse/SONAR-2007
   */
  @Deprecated
  public Measure setRuleCategory(String ruleCategory) {
    this.ruleCategory = ruleCategory;
    return this;
  }

  /**
   * @since 2.5
   */
  public Measure setRuleSeverity(String ruleSeverity) {
    this.ruleSeverity = ruleSeverity;
    return this;
  }

  /**
   * @since 2.5
   */
  public String getRuleSeverity() {
    return ruleSeverity;
  }

  /**
   * @deprecated since 2.5 use {@link #getRuleSeverity()} instead. See http://jira.codehaus.org/browse/SONAR-1829
   */
  @Deprecated
  public String getRulePriority() {
    return ruleSeverity;
  }

  /**
   * @deprecated since 2.5 use {@link #setRuleSeverity(String)} instead. See http://jira.codehaus.org/browse/SONAR-1829
   */
  @Deprecated
  public Measure setRulePriority(String rulePriority) {
    this.ruleSeverity = rulePriority;
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

  /**
   * Variation value on period 1. The value is loaded if ResourceQuery#setIncludeTrends() is set to true.
   * @since 2.5
   */
  public Double getVariation1() {
    return variation1;
  }

  /**
   * @since 2.5
   */
  public Measure setVariation1(Double variation1) {
    this.variation1 = variation1;
    return this;
  }

  /**
   * Variation value on period 2. The value is loaded if ResourceQuery#setIncludeTrends() is set to true.
   * @since 2.5
   */
  public Double getVariation2() {
    return variation2;
  }

  /**
   * @since 2.5
   */
  public Measure setVariation2(Double variation2) {
    this.variation2 = variation2;
    return this;
  }

  /**
   * Variation value on period 3. The value is loaded if ResourceQuery#setIncludeTrends() is set to true.
   * @since 2.5
   */
  public Double getVariation3() {
    return variation3;
  }

  /**
   * @since 2.5
   */
  public Measure setVariation3(Double variation3) {
    this.variation3 = variation3;
    return this;
  }

  /**
   * Variation value on period 4. The value is loaded if ResourceQuery#setIncludeTrends() is set to true.
   * @since 2.5
   */
  public Double getVariation4() {
    return variation4;
  }

  /**
   * @since 2.5
   */
  public Measure setVariation4(Double variation4) {
    this.variation4 = variation4;
    return this;
  }

  /**
   * Variation value on period 5. The value is loaded if ResourceQuery#setIncludeTrends() is set to true.
   * @since 2.5
   */
  public Double getVariation5() {
    return variation5;
  }

  /**
   * @since 2.5
   */
  public Measure setVariation5(Double variation5) {
    this.variation5 = variation5;
    return this;
  }

  @Override
  public String toString() {
    return new StringBuilder().append("Measure{")
        .append("metricKey='").append(metricKey).append('\'')
        .append(", metricName='").append(metricName).append('\'')
        .append(", value=").append(value)
        .append(", formattedValue='").append(formattedValue).append('\'')
        .append(", data='").append(data).append('\'')
        .append(", characteristicKey='").append(characteristicKey).append('\'')
        .append(", characteristicName='").append(characteristicName).append('\'')
        .append(", trend=").append(trend).append(", var=").append(var)
        .append(", ruleKey='").append(ruleKey).append('\'')
        .append(", ruleName='").append(ruleName).append('\'')
        .append(", ruleCategory='").append(ruleCategory).append('\'')
        .append(", rulePriority='").append(ruleSeverity).append('\'')
        .append('}').toString();
  }
}
