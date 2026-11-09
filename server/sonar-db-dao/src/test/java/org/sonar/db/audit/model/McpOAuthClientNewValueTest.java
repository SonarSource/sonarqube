/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.db.audit.model;

import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class McpOAuthClientNewValueTest {

  @Test
  void toString_formats_the_client_id_the_name_and_the_sorted_redirect_uris() {
    var result = new McpOAuthClientNewValue("https://client.example/metadata.json", "Example client", Set.of("https://client.example/b", "https://client.example/a")).toString();

    assertThat(result).isEqualTo("{\"clientId\": \"https://client.example/metadata.json\", \"clientName\": \"Example client\", \"redirectUris\": \"https://client.example/a https://client.example/b\" }");
  }

  @Test
  void toString_omits_the_name_and_the_redirect_uris_when_there_is_none() {
    assertThat(new McpOAuthClientNewValue("client", null, Set.of())).hasToString("{\"clientId\": \"client\" }");
  }
}
