/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.core.config;

import java.util.List;
import org.sonar.api.CoreProperties;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;

public final class NewsProperties {

  public static final String NEWS_ENABLED = "sonar.news.enabled";
  public static final String NEWS_SUBCATEGORY = "news";

  private NewsProperties() {
    // only static stuff
  }

  public static List<PropertyDefinition> all() {
    return List.of(
      PropertyDefinition.builder(NEWS_ENABLED)
        .name("Enable product news")
        .description("Show product news and announcements (including the notification bell) to users. " +
          "Disable this if you don't want users connecting to third-party services for news content.")
        .type(PropertyType.BOOLEAN)
        .defaultValue(Boolean.toString(true))
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(NEWS_SUBCATEGORY)
        .index(1)
        .build());
  }
}
