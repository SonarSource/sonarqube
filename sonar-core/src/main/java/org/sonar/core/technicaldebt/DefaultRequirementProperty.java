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
package org.sonar.core.technicaldebt;

public class DefaultRequirementProperty {

  public static final String PROPERTY_REMEDIATION_FUNCTION = "remediationFunction";
  public static final String PROPERTY_REMEDIATION_FACTOR = "remediationFactor";
  public static final String PROPERTY_OFFSET = "offset";

  private String key;
  private Double value;
  private String textValue;

  public String key() {
    return key;
  }

  public DefaultRequirementProperty setKey(String key) {
    this.key = key;
    return this;
  }

  public Double value() {
    return value;
  }

  public DefaultRequirementProperty setValue(Double value) {
    this.value = value;
    return this;
  }

  public String textValue() {
    return textValue;
  }

  public DefaultRequirementProperty setTextValue(String textValue) {
    this.textValue = textValue;
    return this;
  }
}
