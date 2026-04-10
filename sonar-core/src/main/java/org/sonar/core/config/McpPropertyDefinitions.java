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

public class McpPropertyDefinitions {

  public static final String PROP_MCP_ENABLED = "sonar.mcp.enabled";

  public static final String PROP_MCP_SERVER_URL = "sonar.mcp.serverUrl";

  private McpPropertyDefinitions() {
    // only static stuff
  }

  public static List<PropertyDefinition> all() {
    return List.of(
      builder(PROP_MCP_ENABLED)
        .name("Enable MCP proxy")
        .description("When enabled, SonarQube forwards requests to /mcp/* to the configured MCP Server.")
        .type(PropertyType.BOOLEAN)
        .defaultValue(Boolean.toString(true))
        .hidden()
        .build(),

      builder(PROP_MCP_SERVER_URL)
        .name("MCP Server URL")
        .description("Base URL of the MCP Server that SonarQube will proxy requests to (e.g. http://127.0.0.1:8080). Required when MCP proxy is enabled.")
        .type(PropertyType.STRING)
        .hidden()
        .build());
  }
}
