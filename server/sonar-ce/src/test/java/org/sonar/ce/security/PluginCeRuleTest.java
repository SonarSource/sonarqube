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
package org.sonar.ce.security;

import java.security.Permission;
import java.security.SecurityPermission;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PluginCeRuleTest {
  private final PluginCeRule rule = new PluginCeRule();
  private final Permission allowedRuntime = new RuntimePermission("getFileSystemAttributes");
  private final Permission deniedRuntime = new RuntimePermission("getClassLoader");
  private final Permission allowedSecurity = new SecurityPermission("getProperty.key");
  private final Permission deniedSecurity = new SecurityPermission("setPolicy");

  @Test
  public void rule_restricts_denied_permissions() {
    assertThat(rule.implies(deniedSecurity)).isFalse();
    assertThat(rule.implies(deniedRuntime)).isFalse();
  }

  @Test
  public void rule_allows_permissions() {
    assertThat(rule.implies(allowedSecurity)).isTrue();
    assertThat(rule.implies(allowedRuntime)).isTrue();
  }
}
