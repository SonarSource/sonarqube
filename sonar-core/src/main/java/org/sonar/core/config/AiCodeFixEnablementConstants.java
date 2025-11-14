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

public final class AiCodeFixEnablementConstants {
  public static final String SUGGESTION_FEATURE_ENABLED_PROPERTY = "sonar.ai.suggestions.enabled";
  public static final String SUGGESTION_PROVIDER_KEY_PROPERTY = "sonar.ai.suggestions.provider.key";
  public static final String SUGGESTION_PROVIDER_MODEL_KEY_PROPERTY = "sonar.ai.suggestions.provider.modelKey";
  public static final String SUGGESTION_PROVIDER_ENDPOINT_PROPERTY = "sonar.ai.suggestions.provider.endpoint";
  public static final String SUGGESTION_PROVIDER_API_KEY_INTERNAL_PROPERTY = "sonar.ai.suggestions.provider.apiKey";

  private AiCodeFixEnablementConstants() {
  }

}
