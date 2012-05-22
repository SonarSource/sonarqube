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
public final class CriteriaDto {
  private Long id;
  private Long filterId;
  private String family;
  private String key;
  private String operator;
  private Float value;
  private String textValue;
  private Boolean variation;

  public Long getId() {
    return id;
  }

  public CriteriaDto setId(Long id) {
    this.id = id;
    return this;
  }

  public Long getFilterId() {
    return filterId;
  }

  public CriteriaDto setFilterId(Long filterId) {
    this.filterId = filterId;
    return this;
  }

  public String getFamily() {
    return family;
  }

  public CriteriaDto setFamily(String family) {
    this.family = family;
    return this;
  }

  public String getKey() {
    return key;
  }

  public CriteriaDto setKey(String key) {
    this.key = key;
    return this;
  }

  public String getOperator() {
    return operator;
  }

  public CriteriaDto setOperator(String operator) {
    this.operator = operator;
    return this;
  }

  public Float getValue() {
    return value;
  }

  public CriteriaDto setValue(Float value) {
    this.value = value;
    return this;
  }

  public String getTextValue() {
    return textValue;
  }

  public CriteriaDto setTextValue(String textValue) {
    this.textValue = textValue;
    return this;
  }

  public Boolean getVariation() {
    return variation;
  }

  public CriteriaDto setVariation(Boolean variation) {
    this.variation = variation;
    return this;
  }
}
