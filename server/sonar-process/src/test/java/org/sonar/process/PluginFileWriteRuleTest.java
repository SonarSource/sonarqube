/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.process;

import java.io.FilePermission;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.PropertyPermission;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PluginFileWriteRuleTest {
  private final Path home = Paths.get("/path/to/home");
  private final Path tmp = Paths.get("/path/to/home/tmp");
  private final PluginFileWriteRule rule = new PluginFileWriteRule(home, tmp);

  @Test
  public void policy_restricts_modifying_home() {
    assertThat(rule.implies(new FilePermission(Paths.get("/path/to/home/file").toAbsolutePath().toString(), "write"))).isFalse();
    assertThat(rule.implies(new FilePermission(Paths.get("/path/to/home/file").toAbsolutePath().toString(), "execute"))).isFalse();
    assertThat(rule.implies(new FilePermission(Paths.get("/path/to/home/file").toAbsolutePath().toString(), "delete"))).isFalse();
    assertThat(rule.implies(new FilePermission(Paths.get("/path/to/home/file").toAbsolutePath().toString(), "read"))).isTrue();
    assertThat(rule.implies(new FilePermission(Paths.get("/path/to/home/file").toAbsolutePath().toString(), "readlink"))).isTrue();

    assertThat(rule.implies(new FilePermission(Paths.get("/path/to/home/extensions/file").toAbsolutePath().toString(), "write"))).isFalse();

    assertThat(rule.implies(new FilePermission(Paths.get("/path/to/").toAbsolutePath().toString(), "write"))).isTrue();
  }

  @Test
  public void policy_implies_other_permissions() {
    assertThat(rule.implies(new PropertyPermission(Paths.get("/path/to/").toAbsolutePath().toString(), "write"))).isTrue();
  }
}
