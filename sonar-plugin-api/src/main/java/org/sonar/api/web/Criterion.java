/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.api.web;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSortedSet;

import java.util.Set;

/**
 * Definition of a criterion to be used to narrow down a {@link Filter}.
 *
 * @since 3.1
 */
public final class Criterion {
  private static final String METRIC_FAMILY = "metric";
  private static final String QUALIFIER_FAMILY = "qualifier";
  public static final String EQ = "eq";
  public static final String GT = "gt";
  public static final String GTE = "gte";
  public static final String LT = "lt";
  public static final String LTE = "lte";
  public static final Set<String> OPERATORS = ImmutableSortedSet.of(EQ, GT, GTE, LT, LTE);

  private final String family;
  private final String key;
  private final String operator;
  private final Float value;
  private final String textValue;
  private final boolean variation;

  private Criterion(String family, String key, String operator, Float value, String textValue, boolean variation) {
    Preconditions.checkArgument(OPERATORS.contains(operator), "Valid operators are %s, not '%s'", OPERATORS, operator);

    this.family = family;
    this.key = key;
    this.operator = operator;
    this.value = value;
    this.textValue = textValue;
    this.variation = variation;
  }

  /**
   * Creates a new {@link Criterion} with a numerical value.
   *
   * <p>Valid values for the {@code operator} are {@code #EQ}, {@code #GT}, {@code #GTE}, {@code #LT} and {@code #LTE}
   *
   * @throws IllegalArgumentException if {@code operator} is not valid
   */
  public static Criterion create(String family, String key, String operator, Float value, boolean variation) {
    return new Criterion(family, key, operator, value, null, variation);
  }

  /**
   * Creates a new {@link Criterion} with a text value.
   *
   * <p>Valid values for the {@code operator} are {@code #EQ}, {@code #GT}, {@code #GTE}, {@code #LT} and {@code #LTE}
   *
   * @throws IllegalArgumentException if {@code operator} is not valid
   */
  public static Criterion create(String family, String key, String operator, String textValue, boolean variation) {
    return new Criterion(family, key, operator, null, textValue, variation);
  }

  /**
   * Creates a new {@link Criterion} on a metric, with a numerical value.
   *
   * <p>Valid values for the {@code operator} are {@code #EQ}, {@code #GT}, {@code #GTE}, {@code #LT} and {@code #LTE}
   *
   * @throws IllegalArgumentException if {@code operator} is not valid
   */
  public static Criterion createForMetric(String key, String operator, Float value, boolean variation) {
    return new Criterion(METRIC_FAMILY, key, operator, value, null, variation);
  }

  /**
   * Creates a new {@link Criterion} on a metric, with a text value.
   *
   * <p>Valid values for the {@code operator} are {@code #EQ}, {@code #GT}, {@code #GTE}, {@code #LT} and {@code #LTE}
   *
   * @throws IllegalArgumentException if {@code operator} is not valid
   */
  public static Criterion createForMetric(String key, String operator, String textValue, boolean variation) {
    return new Criterion(METRIC_FAMILY, key, operator, null, textValue, variation);
  }

  /**
   * Creates a new {@link Criterion} on a qualifier.
   */
  public static Criterion createForQualifier(Object... values) {
    return new Criterion(QUALIFIER_FAMILY, null, EQ, null, Joiner.on(',').join(values), false);
  }

  /**
   * Get the the criterion's family.
   *
   * @return the family
   */
  public String getFamily() {
    return family;
  }

  /**
   * Get the the criterion's key.
   *
   * @return the key
   */
  public String getKey() {
    return key;
  }

  /**
   * Get the the criterion's operator.
   *
   * <p>Valid values for the {@code operator} are {@code #EQ}, {@code #GT}, {@code #GTE}, {@code #LT} and {@code #LTE}
   *
   * @return the operator
   */
  public String getOperator() {
    return operator;
  }

  /**
   * Get the the criterion's value.
   *
   * @return the value
   */
  public Float getValue() {
    return value;
  }

  /**
   * Get the the criterion's value as text.
   *
   * @return the value as text
   */
  public String getTextValue() {
    return textValue;
  }

  /**
   * A criterion can be based on the varation of a value rather than on the value itself.
   *
   * @return <code>true</code> when the variation is used rather than the value
   */
  public boolean isVariation() {
    return variation;
  }
}
