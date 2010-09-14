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
package org.sonar.api.checks.templates;

import org.sonar.api.checks.templates.CheckTemplateProperty;

import java.util.Locale;

/**
 * @since 2.1
 */
public class DefaultCheckTemplateProperty extends CheckTemplateProperty {

  private String title;
  private String description;

  public String getTitle() {
    if (title == null || "".equals(title)) {
      return getKey();
    }
    return title;
  }

  @Override
  public String getTitle(Locale locale) {
    return getTitle();
  }

  public void setTitle(String s) {
    this.title = s;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String s) {
    this.description = s;
  }

  @Override
  public String getDescription(Locale locale) {
    return getDescription();
  }
}
