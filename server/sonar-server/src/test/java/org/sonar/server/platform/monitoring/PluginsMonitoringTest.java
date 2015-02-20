/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.platform.monitoring;

import org.junit.Test;
import org.sonar.api.Plugin;
import org.sonar.api.platform.PluginMetadata;
import org.sonar.api.platform.PluginRepository;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.plugins.DefaultPluginMetadata;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class PluginsMonitoringTest {

  PluginsMonitoring sut;

  @Test
  public void the_json_contains_name_and_version() throws Exception {
    sut = new PluginsMonitoring(new FakePluginRepository());
    StringWriter writer = new StringWriter();
    JsonWriter json = JsonWriter.of(writer);

    sut.toJson(json);

    assertThat(writer.toString()).contains("plugin-1", "1.1");
    assertThat(writer.toString()).contains("plugin-2", "2.2");
  }

  @Test
  public void the_string_contains_name_and_version() throws Exception {
    sut = new PluginsMonitoring(new FakePluginRepository());

    String plugins = sut.getPlugins();

    assertThat(plugins).contains("plugin-1", "1.1");
    assertThat(plugins).contains("plugin-2", "2.2");
  }

  private static class FakePluginRepository implements PluginRepository {

    @Override
    public Plugin getPlugin(String key) {
      return null;
    }

    @Override
    public Collection<PluginMetadata> getMetadata() {
      List<PluginMetadata> plugins = new ArrayList<>();
      plugins.add(DefaultPluginMetadata
        .create("key-1")
        .setName("plugin-1")
        .setVersion("1.1"));
      plugins.add(DefaultPluginMetadata
        .create("key-2")
        .setName("plugin-2")
        .setVersion("2.2"));
      return plugins;
    }

    @Override
    public PluginMetadata getMetadata(String pluginKey) {
      return null;
    }
  }
}
