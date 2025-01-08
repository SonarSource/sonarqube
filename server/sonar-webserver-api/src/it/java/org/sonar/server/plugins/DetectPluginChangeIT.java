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
package org.sonar.server.plugins;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.Plugin;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.plugin.PluginType;
import org.sonar.db.DbTester;
import org.sonar.db.plugin.PluginDto;
import org.sonar.server.plugins.PluginFilesAndMd5.FileAndMd5;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DetectPluginChangeIT {
  @Rule
  public DbTester dbTester = DbTester.create();

  private final ServerPluginRepository pluginRepository = new ServerPluginRepository();
  private final DetectPluginChange detectPluginChange = new DetectPluginChange(pluginRepository, dbTester.getDbClient());

  @Test
  public void detect_changed_plugin() {
    addPluginToDb("plugin1", "hash1", PluginDto.Type.BUNDLED);
    addPluginToFs("plugin1", "hash2", PluginType.BUNDLED);

    detectPluginChange.start();
    assertThat(detectPluginChange.anyPluginChanged()).isTrue();
  }

  @Test
  public void detect_changed_type() {
    addPluginToDb("plugin1", "hash1", PluginDto.Type.BUNDLED);
    addPluginToFs("plugin1", "hash1", PluginType.EXTERNAL);

    detectPluginChange.start();
    assertThat(detectPluginChange.anyPluginChanged()).isTrue();
  }

  @Test
  public void detect_new_plugin() {
    addPluginToDb("plugin1", "hash1", PluginDto.Type.BUNDLED);
    addPluginToFs("plugin2", "hash1", PluginType.BUNDLED);

    detectPluginChange.start();
    assertThat(detectPluginChange.anyPluginChanged()).isTrue();
  }

  @Test
  public void detect_removed_plugin() {
    addPluginToDb("plugin1", "hash1", PluginDto.Type.BUNDLED, true);
    addPluginToFs("plugin1", "hash1", PluginType.BUNDLED);

    detectPluginChange.start();
    assertThat(detectPluginChange.anyPluginChanged()).isTrue();
  }

  @Test
  public void detect_missing_plugin() {
    addPluginToDb("plugin1", "hash1", PluginDto.Type.BUNDLED);

    detectPluginChange.start();
    assertThat(detectPluginChange.anyPluginChanged()).isTrue();
  }

  @Test
  public void detect_no_changes() {
    addPluginToDb("plugin1", "hash1", PluginDto.Type.BUNDLED);
    addPluginToFs("plugin1", "hash1", PluginType.BUNDLED);
    addPluginToDb("plugin2", "hash2", PluginDto.Type.EXTERNAL);
    addPluginToFs("plugin2", "hash2", PluginType.EXTERNAL);

    detectPluginChange.start();
    assertThat(detectPluginChange.anyPluginChanged()).isFalse();
  }

  @Test
  public void fail_if_start_twice() {
    detectPluginChange.start();
    assertThrows(IllegalStateException.class, detectPluginChange::start);
  }

  @Test
  public void fail_if_not_started() {
    assertThrows(NullPointerException.class, detectPluginChange::anyPluginChanged);
  }

  private void addPluginToDb(String key, String hash, PluginDto.Type type) {
    addPluginToDb(key, hash, type,  false);
  }

  private void addPluginToDb(String key, String hash, PluginDto.Type type, boolean removed) {
    dbTester.pluginDbTester().insertPlugin(p -> p.setKee(key).setFileHash(hash).setType(type).setRemoved(removed));
  }

  private void addPluginToFs(String key, String hash, PluginType type) {
    PluginInfo pluginInfo = new PluginInfo(key);
    Plugin plugin = mock(Plugin.class);
    FileAndMd5 fileAndMd5 = mock(FileAndMd5.class);
    when(fileAndMd5.getMd5()).thenReturn(hash);
    ServerPlugin serverPlugin = new ServerPlugin(pluginInfo, type, plugin, fileAndMd5, null);
    pluginRepository.addPlugin(serverPlugin);
  }

}
