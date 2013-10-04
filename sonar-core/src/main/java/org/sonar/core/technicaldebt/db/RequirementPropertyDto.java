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
package org.sonar.core.technicaldebt.db;

import org.sonar.core.technicaldebt.DefaultRequirementProperty;

import java.io.Serializable;

public class RequirementPropertyDto implements Serializable {

  private Long id;
  private String kee;
  private Double value;
  private String textValue;

  public Long getId() {
    return id;
  }

  public RequirementPropertyDto setId(Long id) {
    this.id = id;
    return this;
  }

  public String getKey() {
    return kee;
  }

  public RequirementPropertyDto setKey(String key) {
    this.kee = key;
    return this;
  }

  public Double getValue() {
    return value;
  }

  public RequirementPropertyDto setValue(Double value) {
    this.value = value;
    return this;
  }

  public String getTextValue() {
    return textValue;
  }

  public RequirementPropertyDto setTextValue(String textValue) {
    this.textValue = textValue;
    return this;
  }

  public DefaultRequirementProperty toDefaultRequirementProperty() {
    return new DefaultRequirementProperty()
      .setKey(kee)
      .setTextValue(textValue)
      .setValue(value);
  }

}
