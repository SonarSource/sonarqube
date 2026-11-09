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
import org.sonar.api.utils.DateUtils;

import static org.assertj.core.api.Assertions.assertThat;

class McpOAuthNewValueTest {

  @Test
  void toString_formats_all_fields_with_sorted_scopes() {
    long expiresAt = 1_700_000_000_000L;

    var result = new McpOAuthNewValue("user-uuid", "jdoe", "https://client.example/metadata.json", Set.of("write", "read"), expiresAt).toString();

    assertThat(result).isEqualTo("{\"userLogin\": \"jdoe\", \"clientId\": \"https://client.example/metadata.json\", \"scopes\": \"read write\", \"expiresAt\": \""
      + DateUtils.formatDateTime(expiresAt) + "\" }");
  }

  @Test
  void getters_return_the_user() {
    var newValue = new McpOAuthNewValue("user-uuid", "jdoe", "client", Set.of("read"), 42L);

    assertThat(newValue.getUserUuid()).isEqualTo("user-uuid");
    assertThat(newValue.getUserLogin()).isEqualTo("jdoe");
  }

  @Test
  void toString_omits_the_fields_that_are_not_set_and_never_the_user_uuid() {
    var result = new McpOAuthNewValue("user-uuid", "jdoe", null, Set.of(), null).toString();

    assertThat(result).isEqualTo("{\"userLogin\": \"jdoe\" }").doesNotContain("user-uuid");
  }
}
