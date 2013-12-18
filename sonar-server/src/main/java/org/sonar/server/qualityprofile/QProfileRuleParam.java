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
package org.sonar.server.qualityprofile;

public class QProfileRuleParam {
  private final String key;
  private final String value;
  private final String description;
  private final String defaultValue;
  private final String type;
  public QProfileRuleParam(String key, String value, String description, String defaultValue, String type) {
    super();
    this.key = key;
    this.value = value;
    this.description = description;
    this.defaultValue = defaultValue;
    this.type = type;
  }
  public String key() {
    return key;
  }
  public String value() {
    return value;
  }
  public String description() {
    return description;
  }
  public String defaultValue() {
    return defaultValue;
  }
  public String type() {
    return type;
  }
}
