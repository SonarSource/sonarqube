/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.analysis;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ScannerPluginTest {

  @Test
  public void verify_getters() {
    ScannerPlugin plugin = new ScannerPlugin("key", "base", 12345L);

    assertThat(plugin.getKey()).isEqualTo("key");
    assertThat(plugin.getBasePluginKey()).isEqualTo("base");
    assertThat(plugin.getUpdatedAt()).isEqualTo(12345L);
  }

  @Test
  public void verify_toString() {
    ScannerPlugin plugin = new ScannerPlugin("key", "base", 12345L);

    assertThat(plugin.toString()).isEqualTo("ScannerPlugin{key='key', basePluginKey='base', updatedAt='12345'}");
  }

  @Test
  public void equals_is_based_on_key_only() {
    ScannerPlugin plugin = new ScannerPlugin("key", "base", 12345L);

    assertThat(plugin).isEqualTo(plugin);
    assertThat(plugin).isEqualTo(new ScannerPlugin("key", null, 45678L));
    assertThat(plugin).isNotEqualTo(new ScannerPlugin("key2", "base", 12345L));
    assertThat(plugin).isNotEqualTo(null);
    assertThat(plugin).isNotEqualTo("toto");
  }

  @Test
  public void hashcode_is_based_on_key_only() {
    ScannerPlugin plugin = new ScannerPlugin("key", "base", 12345L);

    assertThat(plugin.hashCode()).isEqualTo("key".hashCode());
  }
}
