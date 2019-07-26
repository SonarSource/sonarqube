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
package org.sonar.server.startup;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.utils.System2;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.plugin.PluginDto;
import org.sonar.server.plugins.InstalledPlugin;
import org.sonar.server.plugins.PluginFileSystem;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;

public class RegisterPluginsTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private final long now = 12345L;
  private DbClient dbClient = dbTester.getDbClient();
  private PluginFileSystem pluginFileSystem = mock(PluginFileSystem.class);
  private UuidFactory uuidFactory = mock(UuidFactory.class);
  private System2 system2 = mock(System2.class);

  @Before
  public void setUp() {
    when(system2.now()).thenReturn(now).thenThrow(new IllegalStateException("Should be called only once"));
  }

  /**
   * Insert new plugins
   */
  @Test
  public void insert_new_plugins() throws IOException {
    File fakeJavaJar = temp.newFile();
    FileUtils.write(fakeJavaJar, "fakejava", StandardCharsets.UTF_8);
    File fakeJavaCustomJar = temp.newFile();
    FileUtils.write(fakeJavaCustomJar, "fakejavacustom", StandardCharsets.UTF_8);
    when(pluginFileSystem.getInstalledFiles()).thenReturn(asList(
      newPlugin("java", fakeJavaJar, null),
      newPlugin("javacustom", fakeJavaCustomJar, "java")));
    when(uuidFactory.create()).thenReturn("a").thenReturn("b").thenThrow(new IllegalStateException("Should be called only twice"));
    RegisterPlugins register = new RegisterPlugins(pluginFileSystem, dbClient, uuidFactory, system2);
    register.start();

    Map<String, PluginDto> pluginsByKey = selectAllPlugins();
    assertThat(pluginsByKey).hasSize(2);
    verify(pluginsByKey.get("java"), null, "bd451e47a1aa76e73da0359cef63dd63", now, now);
    verify(pluginsByKey.get("javacustom"), "java", "de9b2de3ddc0680904939686c0dba5be", now, now);

    register.stop();
  }

  /**
   * Update existing plugins, only when checksum is different and don't remove uninstalled plugins
   */
  @Test
  public void update_only_changed_plugins() throws IOException {
    dbClient.pluginDao().insert(dbTester.getSession(), new PluginDto()
      .setUuid("a")
      .setKee("java")
      .setBasePluginKey(null)
      .setFileHash("bd451e47a1aa76e73da0359cef63dd63")
      .setCreatedAt(1L)
      .setUpdatedAt(1L));
    dbClient.pluginDao().insert(dbTester.getSession(), new PluginDto()
      .setUuid("b")
      .setKee("javacustom")
      .setBasePluginKey("java")
      .setFileHash("de9b2de3ddc0680904939686c0dba5be")
      .setCreatedAt(1L)
      .setUpdatedAt(1L));
    dbTester.commit();

    File fakeJavaCustomJar = temp.newFile();
    FileUtils.write(fakeJavaCustomJar, "fakejavacustomchanged", StandardCharsets.UTF_8);
    when(pluginFileSystem.getInstalledFiles()).thenReturn(asList(
      newPlugin("javacustom", fakeJavaCustomJar, "java2")));

    new RegisterPlugins(pluginFileSystem, dbClient, uuidFactory, system2).start();

    Map<String, PluginDto> pluginsByKey = selectAllPlugins();
    assertThat(pluginsByKey).hasSize(2);
    verify(pluginsByKey.get("java"), null, "bd451e47a1aa76e73da0359cef63dd63", 1L, 1L);
    verify(pluginsByKey.get("javacustom"), "java2", "d22091cff5155e892cfe2f9dab51f811", 1L, now);
  }

  private static InstalledPlugin newPlugin(String key, File file, @Nullable String basePlugin) {
    InstalledPlugin.FileAndMd5 jar = new InstalledPlugin.FileAndMd5(file);
    PluginInfo info = new PluginInfo(key)
      .setBasePlugin(basePlugin)
      .setJarFile(file);
    return new InstalledPlugin(info, jar, null);
  }

  private Map<String, PluginDto> selectAllPlugins() {
    return dbTester.getDbClient().pluginDao().selectAll(dbTester.getSession())
      .stream()
      .collect(uniqueIndex(PluginDto::getKee));
  }

  private void verify(PluginDto java, @Nullable String basePluginKey, String fileHash, @Nullable Long createdAt, long updatedAt) {
    assertThat(java.getBasePluginKey()).isEqualTo(basePluginKey);
    assertThat(java.getFileHash()).isEqualTo(fileHash);
    assertThat(java.getCreatedAt()).isEqualTo(createdAt);
    assertThat(java.getUpdatedAt()).isEqualTo(updatedAt);
  }

}
