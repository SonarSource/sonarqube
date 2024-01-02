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
package org.sonar.server.startup;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.utils.System2;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.plugin.PluginType;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.plugin.PluginDto;
import org.sonar.db.plugin.PluginDto.Type;
import org.sonar.server.plugins.PluginFilesAndMd5;
import org.sonar.server.plugins.ServerPlugin;
import org.sonar.server.plugins.ServerPluginRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RegisterPluginsIT {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private final long now = 12345L;
  private final DbClient dbClient = dbTester.getDbClient();
  private final ServerPluginRepository serverPluginRepository = new ServerPluginRepository();
  private final UuidFactory uuidFactory = mock(UuidFactory.class);
  private final System2 system2 = mock(System2.class);
  private final RegisterPlugins register = new RegisterPlugins(serverPluginRepository, dbClient, uuidFactory, system2);


  @Before
  public void setUp() {
    when(system2.now()).thenReturn(now);
  }

  /**
   * Insert new plugins
   */
  @Test
  public void insert_new_plugins() throws IOException {
    addPlugin("java", null);
    addPlugin("javacustom", "java");
    when(uuidFactory.create()).thenReturn("a").thenReturn("b").thenThrow(new IllegalStateException("Should be called only twice"));
    register.start();

    Map<String, PluginDto> pluginsByKey = selectAllPlugins();
    assertThat(pluginsByKey).hasSize(2);
    verify(pluginsByKey.get("java"), Type.BUNDLED, null, "93f725a07423fe1c889f448b33d21f46", false, now, now);
    verify(pluginsByKey.get("javacustom"), Type.BUNDLED, "java", "bf8b3d4cb91efc5f9ef0bcd02f128ebf", false, now, now);

    register.stop();
  }

  @Test
  public void update_removed_plugins() throws IOException {
    // will be flagged as removed
    dbClient.pluginDao().insert(dbTester.getSession(), new PluginDto()
      .setUuid("a")
      .setKee("java")
      .setBasePluginKey(null)
      .setFileHash("bd451e47a1aa76e73da0359cef63dd63")
      .setType(Type.BUNDLED)
      .setCreatedAt(1L)
      .setUpdatedAt(1L));
    // already flagged as removed
    dbClient.pluginDao().insert(dbTester.getSession(), new PluginDto()
      .setUuid("b")
      .setKee("java2")
      .setBasePluginKey(null)
      .setFileHash("bd451e47a1aa76e73da0359cef63dd63")
      .setType(Type.BUNDLED)
      .setRemoved(true)
      .setCreatedAt(1L)
      .setUpdatedAt(1L));
    dbClient.pluginDao().insert(dbTester.getSession(), new PluginDto()
      .setUuid("c")
      .setKee("csharp")
      .setBasePluginKey(null)
      .setFileHash("a20d785dbacb8f41a3b7392aa7d03b78")
      .setType(Type.EXTERNAL)
      .setCreatedAt(1L)
      .setUpdatedAt(1L));
    dbTester.commit();

    addPlugin("csharp", PluginType.EXTERNAL, null);
    register.start();

    Map<String, PluginDto> pluginsByKey = selectAllPlugins();
    assertThat(pluginsByKey).hasSize(3);
    verify(pluginsByKey.get("java"), Type.BUNDLED, null, "bd451e47a1aa76e73da0359cef63dd63", true, 1L, now);
    verify(pluginsByKey.get("java2"), Type.BUNDLED, null, "bd451e47a1aa76e73da0359cef63dd63", true, 1L, 1L);
    verify(pluginsByKey.get("csharp"), Type.EXTERNAL, null, "a20d785dbacb8f41a3b7392aa7d03b78", false, 1L, 1L);
  }

  @Test
  public void re_add_previously_removed_plugin() throws IOException {
    dbClient.pluginDao().insert(dbTester.getSession(), new PluginDto()
      .setUuid("c")
      .setKee("csharp")
      .setBasePluginKey(null)
      .setFileHash("a20d785dbacb8f41a3b7392aa7d03b78")
      .setType(Type.EXTERNAL)
      .setRemoved(true)
      .setCreatedAt(1L)
      .setUpdatedAt(1L));
    dbTester.commit();

    addPlugin("csharp", PluginType.EXTERNAL, null);
    register.start();

    Map<String, PluginDto> pluginsByKey = selectAllPlugins();
    assertThat(pluginsByKey).hasSize(1);
    verify(pluginsByKey.get("csharp"), Type.EXTERNAL, null, "a20d785dbacb8f41a3b7392aa7d03b78", false, 1L, now);
  }

  /**
   * Update existing plugins, only when checksum is different and don't remove uninstalled plugins
   */
  @Test
  public void update_changed_plugins() throws IOException {
    dbClient.pluginDao().insert(dbTester.getSession(), new PluginDto()
      .setUuid("a")
      .setKee("java")
      .setBasePluginKey(null)
      .setFileHash("bd451e47a1aa76e73da0359cef63dd63")
      .setType(Type.BUNDLED)
      .setCreatedAt(1L)
      .setUpdatedAt(1L));
    dbClient.pluginDao().insert(dbTester.getSession(), new PluginDto()
      .setUuid("b")
      .setKee("javacustom")
      .setBasePluginKey("java")
      .setFileHash("de9b2de3ddc0680904939686c0dba5be")
      .setType(Type.BUNDLED)
      .setCreatedAt(1L)
      .setUpdatedAt(1L));
    dbClient.pluginDao().insert(dbTester.getSession(), new PluginDto()
      .setUuid("c")
      .setKee("csharp")
      .setBasePluginKey(null)
      .setFileHash("a20d785dbacb8f41a3b7392aa7d03b78")
      .setType(Type.EXTERNAL)
      .setCreatedAt(1L)
      .setUpdatedAt(1L));
    dbClient.pluginDao().insert(dbTester.getSession(), new PluginDto()
      .setUuid("d")
      .setKee("new-measures")
      .setBasePluginKey(null)
      .setFileHash("6d24712cf701c41ce5eaa948e0bd6d22")
      .setType(Type.EXTERNAL)
      .setCreatedAt(1L)
      .setUpdatedAt(1L));

    dbTester.commit();

    addPlugin("javacustom", PluginType.BUNDLED, "java2");
    // csharp plugin type changed
    addPlugin("csharp", PluginType.BUNDLED, null);

    register.start();

    Map<String, PluginDto> pluginsByKey = selectAllPlugins();
    assertThat(pluginsByKey).hasSize(4);
    verify(pluginsByKey.get("java"), Type.BUNDLED, null, "bd451e47a1aa76e73da0359cef63dd63", true, 1L, now);
    verify(pluginsByKey.get("javacustom"), Type.BUNDLED, "java2", "bf8b3d4cb91efc5f9ef0bcd02f128ebf", false, 1L, now);
    verify(pluginsByKey.get("csharp"), Type.BUNDLED, null, "a20d785dbacb8f41a3b7392aa7d03b78", false, 1L, now);
    verify(pluginsByKey.get("new-measures"), Type.EXTERNAL, null, "6d24712cf701c41ce5eaa948e0bd6d22", true, 1L, now);
  }

  private ServerPlugin addPlugin(String key, @Nullable String basePlugin) throws IOException {
    return addPlugin(key, PluginType.BUNDLED, basePlugin);
  }

  private ServerPlugin addPlugin(String key, PluginType type, @Nullable String basePlugin) throws IOException {
    File file = createPluginFile(key);
    PluginFilesAndMd5.FileAndMd5 jar = new PluginFilesAndMd5.FileAndMd5(file);
    PluginInfo info = new PluginInfo(key)
      .setBasePlugin(basePlugin)
      .setJarFile(file);
    ServerPlugin serverPlugin = new ServerPlugin(info, type, null, jar, null);
    serverPluginRepository.addPlugin(serverPlugin);
    return serverPlugin;
  }

  private File createPluginFile(String key) throws IOException {
    File pluginJar = temp.newFile();
    FileUtils.write(pluginJar, key, StandardCharsets.UTF_8);
    return pluginJar;
  }

  private Map<String, PluginDto> selectAllPlugins() {
    return dbTester.getDbClient().pluginDao().selectAll(dbTester.getSession()).stream()
      .collect(Collectors.toMap(PluginDto::getKee, Function.identity()));
  }

  private void verify(PluginDto pluginDto, Type type, @Nullable String basePluginKey, String fileHash, boolean removed, @Nullable Long createdAt, long updatedAt) {
    assertThat(pluginDto.getBasePluginKey()).isEqualTo(basePluginKey);
    assertThat(pluginDto.getType()).isEqualTo(type);
    assertThat(pluginDto.getFileHash()).isEqualTo(fileHash);
    assertThat(pluginDto.isRemoved()).isEqualTo(removed);
    assertThat(pluginDto.getCreatedAt()).isEqualTo(createdAt);
    assertThat(pluginDto.getUpdatedAt()).isEqualTo(updatedAt);
  }

}
