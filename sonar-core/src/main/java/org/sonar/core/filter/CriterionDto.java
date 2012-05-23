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
package org.sonar.core.filter;

/**
 * @since 3.1
 */
public final class CriterionDto {
  private Long filterId;
  private String family;
  private String key;
  private String operator;
  private Float value;
  private String textValue;
  private Boolean variation;

  /**
   * @param filterId the filter id to set
   */
  public CriterionDto setFilterId(Long filterId) {
    this.filterId = filterId;
    return this;
  }

  /**
   * @param family the family to set
   */
  public CriterionDto setFamily(String family) {
    this.family = family;
    return this;
  }

  /**
   * @param key the key to set
   */
  public CriterionDto setKey(String key) {
    this.key = key;
    return this;
  }

  /**
   * @param operator the operator to set
   */
  public CriterionDto setOperator(String operator) {
    this.operator = operator;
    return this;
  }

  /**
   * @param value the value to set
   */
  public CriterionDto setValue(Float value) {
    this.value = value;
    return this;
  }

  /**
   * @param textValue the textValue to set
   */
  public CriterionDto setTextValue(String textValue) {
    this.textValue = textValue;
    return this;
  }

  /**
   * @param variation the variation to set
   */
  public CriterionDto setVariation(Boolean variation) {
    this.variation = variation;
    return this;
  }
}
