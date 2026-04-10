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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.sonar.api.config.PropertyDefinition;

import static org.assertj.core.api.Assertions.assertThat;

class McpPropertyDefinitionsTest {

  @Test
  void all_returns_two_properties() {
    assertThat(McpPropertyDefinitions.all()).hasSize(2);
  }

  @Test
  void serverUrl_property_has_no_default_value() {
    PropertyDefinition serverUrl = byKey().get(McpPropertyDefinitions.PROP_MCP_SERVER_URL);
    assertThat(serverUrl).isNotNull();
    assertThat(serverUrl.defaultValue()).isEmpty();
  }

  @Test
  void enabled_property_is_boolean_and_defaults_to_true() {
    PropertyDefinition enabled = byKey().get(McpPropertyDefinitions.PROP_MCP_ENABLED);
    assertThat(enabled).isNotNull();
    assertThat(enabled.defaultValue()).isEqualTo("true");
  }

  @Test
  void core_property_definitions_includes_mcp_properties() {
    List<PropertyDefinition> all = CorePropertyDefinitions.all();
    assertThat(all).extracting(PropertyDefinition::key).contains(
      McpPropertyDefinitions.PROP_MCP_ENABLED,
      McpPropertyDefinitions.PROP_MCP_SERVER_URL);
  }

  private static Map<String, PropertyDefinition> byKey() {
    return McpPropertyDefinitions.all().stream()
      .collect(Collectors.toMap(PropertyDefinition::key, Function.identity()));
  }
}
