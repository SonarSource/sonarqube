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
package org.sonar.server.plugins;

import org.junit.Test;
import org.sonar.core.plugin.PluginType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.core.plugin.PluginType.BUNDLED;
import static org.sonar.core.plugin.PluginType.EXTERNAL;

public class ServerPluginInfoTest {
  @Test
  public void equals_returns_false_with_different_types() {
    ServerPluginInfo info1 = new ServerPluginInfo("key1").setType(EXTERNAL);
    ServerPluginInfo info2 = new ServerPluginInfo("key1").setType(PluginType.BUNDLED);
    ServerPluginInfo info3 = new ServerPluginInfo("key1").setType(EXTERNAL);

    assertThat(info1).isNotEqualTo(info2)
      .isEqualTo(info3)
      .hasSameHashCodeAs(info3.hashCode());
    assertThat(info1.hashCode()).isNotEqualTo(info2.hashCode());
  }

  @Test
  public void set_and_get_type() {
    ServerPluginInfo info = new ServerPluginInfo("key1").setType(EXTERNAL);
    assertThat(info.getType()).isEqualTo(EXTERNAL);

    info.setType(BUNDLED);
    assertThat(info.getType()).isEqualTo(BUNDLED);
  }
}
