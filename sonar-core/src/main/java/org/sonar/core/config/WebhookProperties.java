/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyFieldDefinition;
import org.sonar.api.resources.Qualifiers;

public class WebhookProperties {

  public static final String GLOBAL_KEY = "sonar.webhooks.global";
  public static final String PROJECT_KEY = "sonar.webhooks.project";
  public static final String NAME_FIELD = "name";
  public static final String URL_FIELD = "url";
  private static final String CATEGORY = "webhooks";
  private static final String DESCRIPTION = "Webhooks are used to notify external services when a project analysis is done. " +
    "A HTTP POST request including a JSON payload is sent to each of the provided URLs. " +
    "Learn more in the <a href=\"#\">Webhooks documentation</a>.";

  private WebhookProperties() {
    // only static stuff
  }

  static List<PropertyDefinition> all() {
    return ImmutableList.of(
      PropertyDefinition.builder(GLOBAL_KEY)
        .category(CATEGORY)
        .name("Webhooks")
        .description(DESCRIPTION)
        .fields(
          PropertyFieldDefinition.build(NAME_FIELD)
            .name("Name")
            .type(PropertyType.STRING)
            .build(),
          PropertyFieldDefinition.build(URL_FIELD)
            .name("URL")
            .type(PropertyType.STRING)
            .build())
        .build(),

      PropertyDefinition.builder(PROJECT_KEY)
        .category(CATEGORY)
        .name("Project Webhooks")
        .description(DESCRIPTION)
        .onlyOnQualifiers(Qualifiers.PROJECT)
        .fields(
          PropertyFieldDefinition.build(NAME_FIELD)
            .name("Name")
            .type(PropertyType.STRING)
            .build(),
          PropertyFieldDefinition.build(URL_FIELD)
            .name("URL")
            .type(PropertyType.STRING)
            .build())
        .build());
  }
}
