/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.db.permission;

import java.util.Arrays;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class GlobalPermissionTest {

  @Test
  public void fromKey_returns_enum_with_specified_key() {
    for (GlobalPermission p : GlobalPermission.values()) {
      assertThat(GlobalPermission.fromKey(p.getKey())).isEqualTo(p);
    }
  }

  @Test
  public void fromKey_throws_exception_for_non_existing_keys() {
    String non_existing_permission = "non_existing";
    assertThatThrownBy(() -> GlobalPermission.fromKey(non_existing_permission))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void contains_returns_true_for_existing_permissions() {
    Arrays.stream(GlobalPermission.values())
      .map(GlobalPermission::getKey)
      .forEach(key -> {
        assertThat(GlobalPermission.contains(key)).isTrue();
      });
  }

  @Test
  public void contains_returns_false_for_non_existing_permissions() {
    String non_existing_permission = "non_existing";
    assertThat(GlobalPermission.contains(non_existing_permission)).isFalse();
  }

  @Test
  public void all_in_one_line_contains_all_permissions() {
    assertThat(GlobalPermission.ALL_ON_ONE_LINE).isEqualTo("admin, gateadmin, profileadmin, provisioning, scan, applicationcreator, portfoliocreator");
  }
}
