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
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;

import static org.sonar.api.config.PropertyDefinition.builder;

public class AiCodefixPropertyDefinitions {
  public static final String PROP_AI_CODEFIX_HIDDEN = "sonar.ai.codefix.hidden";
  public static final String AI_CODE_CATEGORY = "ai_codefix";

  private AiCodefixPropertyDefinitions() {
    // only static stuff
  }

  public static List<PropertyDefinition> all() {
    return List.of(
      builder(PROP_AI_CODEFIX_HIDDEN)
        .name("AI Codefix feature hidden")
        .description("Defines if the AI Codefix feature should be hidden across the product, including its marketing content.")
        .type(PropertyType.BOOLEAN)
        .hidden()
        .category(AI_CODE_CATEGORY)
        .defaultValue(Boolean.toString(false))
        .build());
  }
}
