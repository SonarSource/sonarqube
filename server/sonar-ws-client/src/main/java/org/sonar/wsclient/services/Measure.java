/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

public class Measure extends Model {

  private String metricKey;
  private String metricName;
  private Double value;
  private String formattedValue;
  private String alertStatus;
  private String alertText;
  private String data;
  private String characteristicKey;
  private String characteristicName;

  private String ruleKey;
  private String ruleName;
  private String ruleSeverity;

  /**
   * @deprecated since 2.5 See http://jira.codehaus.org/browse/SONAR-2007
   */
  @Deprecated
  private String ruleCategory;

  private Double variation1, variation2, variation3, variation4, variation5;

  @CheckForNull
  public String getMetricKey() {
    return metricKey;
  }

  public Measure setMetricKey(@Nullable String metricKey) {
    this.metricKey = metricKey;
    return this;
  }

  @CheckForNull
  public String getMetricName() {
    return metricName;
  }

  public Measure setMetricName(@Nullable String metricName) {
    this.metricName = metricName;
    return this;
  }

  @CheckForNull
  public Double getValue() {
    return value;
  }

  @CheckForNull
  public Integer getIntValue() {
    if (value == null) {
      return null;
    }
    return value.intValue();
  }

  @CheckForNull
  public Long getLongValue() {
    if (value == null) {
      return null;
    }
    return value.longValue();
  }

  public Measure setValue(@Nullable Double value) {
    this.value = value;
    return this;
  }

  @CheckForNull
  public String getFormattedValue() {
    return formattedValue;
  }

  @CheckForNull
  public String getFormattedValue(@Nullable String defaultValue) {
    if (formattedValue == null) {
      return defaultValue;
    }
    return formattedValue;
  }

  public Measure setFormattedValue(@Nullable String formattedValue) {
    this.formattedValue = formattedValue;
    return this;
  }

  @CheckForNull
  public String getAlertStatus() {
    return alertStatus;
  }

  public Measure setAlertStatus(@Nullable String alertStatus) {
    this.alertStatus = alertStatus;
    return this;
  }

  @CheckForNull
  public String getAlertText() {
    return alertText;
  }

  public Measure setAlertText(@Nullable String alertText) {
    this.alertText = alertText;
    return this;
  }

  @CheckForNull
  public String getData() {
    return data;
  }

  @CheckForNull
  public Map<String, String> getDataAsMap() {
    return getDataAsMap(",");
  }

  @CheckForNull
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

  public Measure setData(@Nullable String data) {
    this.data = data;
    return this;
  }

  @CheckForNull
  public String getRuleKey() {
    return ruleKey;
  }

  public Measure setRuleKey(@Nullable String ruleKey) {
    this.ruleKey = ruleKey;
    return this;
  }

  @CheckForNull
  public String getRuleName() {
    return ruleName;
  }

  public Measure setRuleName(@Nullable String ruleName) {
    this.ruleName = ruleName;
    return this;
  }

  /**
   * @deprecated since 2.5 See http://jira.codehaus.org/browse/SONAR-2007
   */
  @Deprecated
  @CheckForNull
  public String getRuleCategory() {
    return ruleCategory;
  }

  /**
   * @deprecated since 2.5 See http://jira.codehaus.org/browse/SONAR-2007
   */
  @Deprecated
  public Measure setRuleCategory(@Nullable String ruleCategory) {
    this.ruleCategory = ruleCategory;
    return this;
  }

  /**
   * @since 2.5
   */
  public Measure setRuleSeverity(@Nullable String ruleSeverity) {
    this.ruleSeverity = ruleSeverity;
    return this;
  }

  /**
   * @since 2.5
   */
  @CheckForNull
  public String getRuleSeverity() {
    return ruleSeverity;
  }

  /**
   * @deprecated since 2.5 use {@link #getRuleSeverity()} instead. See http://jira.codehaus.org/browse/SONAR-1829
   */
  @Deprecated
  @CheckForNull
  public String getRulePriority() {
    return ruleSeverity;
  }

  /**
   * @deprecated since 2.5 use {@link #setRuleSeverity(String)} instead. See http://jira.codehaus.org/browse/SONAR-1829
   */
  @Deprecated
  public Measure setRulePriority(@Nullable String rulePriority) {
    this.ruleSeverity = rulePriority;
    return this;
  }

  @CheckForNull
  public String getCharacteristicKey() {
    return characteristicKey;
  }

  @CheckForNull
  public String getCharacteristicName() {
    return characteristicName;
  }

  public Measure setCharacteristicKey(@Nullable String s) {
    this.characteristicKey = s;
    return this;
  }

  public Measure setCharacteristicName(@Nullable String s) {
    this.characteristicName = s;
    return this;
  }

  /**
   * Variation value on period 1. The value is loaded if ResourceQuery#setIncludeTrends() is set to true.
   * @since 2.5
   */
  @CheckForNull
  public Double getVariation1() {
    return variation1;
  }

  /**
   * @since 2.5
   */
  public Measure setVariation1(@Nullable Double variation1) {
    this.variation1 = variation1;
    return this;
  }

  /**
   * Variation value on period 2. The value is loaded if ResourceQuery#setIncludeTrends() is set to true.
   * @since 2.5
   */
  @CheckForNull
  public Double getVariation2() {
    return variation2;
  }

  /**
   * @since 2.5
   */
  public Measure setVariation2(@Nullable Double variation2) {
    this.variation2 = variation2;
    return this;
  }

  /**
   * Variation value on period 3. The value is loaded if ResourceQuery#setIncludeTrends() is set to true.
   * @since 2.5
   */
  @CheckForNull
  public Double getVariation3() {
    return variation3;
  }

  /**
   * @since 2.5
   */
  public Measure setVariation3(@Nullable Double variation3) {
    this.variation3 = variation3;
    return this;
  }

  /**
   * Variation value on period 4. The value is loaded if ResourceQuery#setIncludeTrends() is set to true.
   * @since 2.5
   */
  @CheckForNull
  public Double getVariation4() {
    return variation4;
  }

  /**
   * @since 2.5
   */
  public Measure setVariation4(@Nullable Double variation4) {
    this.variation4 = variation4;
    return this;
  }

  /**
   * Variation value on period 5. The value is loaded if ResourceQuery#setIncludeTrends() is set to true.
   * @since 2.5
   */
  @CheckForNull
  public Double getVariation5() {
    return variation5;
  }

  /**
   * @since 2.5
   */
  public Measure setVariation5(@Nullable Double variation5) {
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
        .append(", ruleKey='").append(ruleKey).append('\'')
        .append(", ruleName='").append(ruleName).append('\'')
        .append(", ruleCategory='").append(ruleCategory).append('\'')
        .append(", rulePriority='").append(ruleSeverity).append('\'')
        .append('}').toString();
  }
}
