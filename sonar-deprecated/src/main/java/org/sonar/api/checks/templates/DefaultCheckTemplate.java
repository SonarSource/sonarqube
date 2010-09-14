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

import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.Locale;

/**
 * EXPERIMENTAL - will be used in version 2.2
 *
 * Non-internationalized check
 *
 * @since 2.1
 */
public class DefaultCheckTemplate extends CheckTemplate {

  private String title;
  private String description;

  public DefaultCheckTemplate() {
  }

  public DefaultCheckTemplate(String key) {
    super(key);
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public String getTitle(Locale locale) {
    if (title == null || "".equals(title)) {
      return getKey();
    }
    return title;
  }

  @Override
  public String getDescription(Locale locale) {
    return description;
  }

  @Override
  public String getMessage(Locale locale, String key, Object... params) {
    return null;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("key", key)
        .append("title", title)
        .append("configKey", configKey)
        .append("priority", priority)
        .append("isoCategory", isoCategory)
        .toString();
  }
}
