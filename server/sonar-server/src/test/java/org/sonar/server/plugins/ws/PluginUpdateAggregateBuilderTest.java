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
package org.sonar.server.plugins.ws;

import org.junit.Test;
import org.sonar.server.plugins.ws.PluginUpdateAggregator.PluginUpdateAggregateBuilder;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.PluginUpdate;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.Version;

public class PluginUpdateAggregateBuilderTest {

  private static final Plugin PLUGIN_1 = Plugin.factory("key1");
  private static final Plugin PLUGIN_2 = Plugin.factory("key2");
  private static final Version SOME_VERSION = Version.create("1.0");
  private static final PluginUpdate.Status SOME_STATUS = PluginUpdate.Status.COMPATIBLE;

  @Test(expected = NullPointerException.class)
  public void plugin_can_not_be_null_and_builderFor_enforces_it_with_NPE() {
    PluginUpdateAggregateBuilder.builderFor(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void add_throws_IAE_when_plugin_is_not_equal_to_the_one_of_the_builder() {
    PluginUpdateAggregateBuilder builder = PluginUpdateAggregateBuilder.builderFor(PLUGIN_1);

    builder.add(createPluginUpdate(PLUGIN_2));
  }

  @Test
  public void add_uses_equals_which_takes_only_key_into_account() {
    PluginUpdateAggregateBuilder builder = PluginUpdateAggregateBuilder.builderFor(PLUGIN_1);

    builder.add(createPluginUpdate(Plugin.factory(PLUGIN_1.getKey())));
  }

  private static PluginUpdate createPluginUpdate(Plugin plugin) {
    return PluginUpdate.createWithStatus(new Release(plugin, SOME_VERSION), SOME_STATUS);
  }
}
