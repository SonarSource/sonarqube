/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.Collections;
import org.junit.Test;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.PluginUpdate;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.Version;

import static org.assertj.core.api.Assertions.assertThat;

public class PluginUpdateAggregatorTest {

  public static final Version SOME_VERSION = Version.create("1.0");
  public static final PluginUpdate.Status SOME_STATUS = PluginUpdate.Status.COMPATIBLE;
  private PluginUpdateAggregator underTest = new PluginUpdateAggregator();

  @Test
  public void aggregates_returns_an_empty_collection_when_plugin_collection_is_null() {
    assertThat(underTest.aggregate(null)).isEmpty();
  }

  @Test
  public void aggregates_returns_an_empty_collection_when_plugin_collection_is_empty() {
    assertThat(underTest.aggregate(Collections.emptyList())).isEmpty();
  }

  @Test
  public void aggregates_groups_pluginUpdate_per_plugin_key() {
    Collection<PluginUpdateAggregator.PluginUpdateAggregate> aggregates = underTest.aggregate(ImmutableList.of(
      createPluginUpdate("key1"),
      createPluginUpdate("key1"),
      createPluginUpdate("key0"),
      createPluginUpdate("key2"),
      createPluginUpdate("key0")));

    assertThat(aggregates).hasSize(3);
    assertThat(aggregates).extracting("plugin.key").containsOnlyOnce("key1", "key0", "key2");
  }

  @Test
  public void aggregate_put_pluginUpdates_with_same_plugin_in_the_same_PluginUpdateAggregate() {
    PluginUpdate pluginUpdate1 = createPluginUpdate("key1");
    PluginUpdate pluginUpdate2 = createPluginUpdate("key1");
    PluginUpdate pluginUpdate3 = createPluginUpdate("key1");
    Collection<PluginUpdateAggregator.PluginUpdateAggregate> aggregates = underTest.aggregate(ImmutableList.of(
      pluginUpdate1,
      pluginUpdate2,
      pluginUpdate3));

    assertThat(aggregates).hasSize(1);
    Collection<PluginUpdate> releases = aggregates.iterator().next().getUpdates();
    assertThat(releases).hasSize(3);
    assertThat(releases).contains(pluginUpdate1);
    assertThat(releases).contains(pluginUpdate2);
    assertThat(releases).contains(pluginUpdate3);
  }

  private PluginUpdate createPluginUpdate(String pluginKey) {
    return PluginUpdate.createWithStatus(new Release(Plugin.factory(pluginKey), SOME_VERSION), SOME_STATUS);
  }
}
