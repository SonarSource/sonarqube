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
package org.sonar.api.web;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * Definition of a criterion to be used to narrow down a {@see Filter}.
 *
 * @since 3.1
 */
public class Criterion {
  public static final List<String> OPERATORS = ImmutableList.of("=", ">", "<", ">=", "<=");

  private String family;
  private String key;
  private String operator;
  private Float value;
  private String textValue;
  private boolean variation;

  private Criterion() {
    // The factory method should be used
  }

  /**
   * Creates a new {@link Criterion}.
   */
  public static Criterion create() {
    return new Criterion();
  }

  public String getFamily() {
    return family;
  }

  public Criterion setFamily(String family) {
    this.family = family;
    return this;
  }

  public String getKey() {
    return key;
  }

  public Criterion setKey(String key) {
    this.key = key;
    return this;
  }

  public String getOperator() {
    return operator;
  }

  public Criterion setOperator(String operator) {
    Preconditions.checkArgument(OPERATORS.contains(operator), "Valid operators are %s, not %s", OPERATORS, operator);
    this.operator = operator;
    return this;
  }

  public Float getValue() {
    return value;
  }

  public Criterion setValue(Float value) {
    this.value = value;
    return this;
  }

  public String getTextValue() {
    return textValue;
  }

  public Criterion setTextValue(String textValue) {
    this.textValue = textValue;
    return this;
  }

  public boolean isVariation() {
    return variation;
  }

  public Criterion setVariation(boolean variation) {
    this.variation = variation;
    return this;
  }
}
