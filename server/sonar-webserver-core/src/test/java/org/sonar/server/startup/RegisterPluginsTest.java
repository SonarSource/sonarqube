/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import org.sonar.db.plugin.PluginDto.Type;
import org.sonar.server.plugins.PluginFilesAndMd5;
import org.sonar.server.plugins.PluginType;
import org.sonar.server.plugins.ServerPlugin;
import org.sonar.server.plugins.ServerPluginRepository;

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
  private final DbClient dbClient = dbTester.getDbClient();
  private final ServerPluginRepository serverPluginRepository = mock(ServerPluginRepository.class);
  private final UuidFactory uuidFactory = mock(UuidFactory.class);
  private final System2 system2 = mock(System2.class);

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
    when(serverPluginRepository.getPlugins()).thenReturn(asList(
      newPlugin("java", fakeJavaJar, null),
      newPlugin("javacustom", fakeJavaCustomJar, "java")));
    when(uuidFactory.create()).thenReturn("a").thenReturn("b").thenThrow(new IllegalStateException("Should be called only twice"));
    RegisterPlugins register = new RegisterPlugins(serverPluginRepository, dbClient, uuidFactory, system2);
    register.start();

    Map<String, PluginDto> pluginsByKey = selectAllPlugins();
    assertThat(pluginsByKey).hasSize(2);
    verify(pluginsByKey.get("java"), Type.BUNDLED, null, "bd451e47a1aa76e73da0359cef63dd63", now, now);
    verify(pluginsByKey.get("javacustom"), Type.BUNDLED, "java", "de9b2de3ddc0680904939686c0dba5be", now, now);

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
      .setFileHash("a4813b6d879c4ec852747c175cdd6141")
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

    File fakeJavaCustomJar = temp.newFile();
    FileUtils.write(fakeJavaCustomJar, "fakejavacustomchanged", StandardCharsets.UTF_8);

    File fakeCSharpJar = temp.newFile();
    FileUtils.write(fakeCSharpJar, "fakecsharp", StandardCharsets.UTF_8);

    when(serverPluginRepository.getPlugins()).thenReturn(asList(
      newPlugin("javacustom", PluginType.BUNDLED, fakeJavaCustomJar, "java2"),
      // csharp plugin type changed
      newPlugin("csharp", PluginType.BUNDLED, fakeCSharpJar, null)));

    new RegisterPlugins(serverPluginRepository, dbClient, uuidFactory, system2).start();

    Map<String, PluginDto> pluginsByKey = selectAllPlugins();
    assertThat(pluginsByKey).hasSize(4);
    verify(pluginsByKey.get("java"), Type.BUNDLED, null, "bd451e47a1aa76e73da0359cef63dd63", 1L, 1L);
    verify(pluginsByKey.get("javacustom"), Type.BUNDLED, "java2", "d22091cff5155e892cfe2f9dab51f811", 1L, now);
    verify(pluginsByKey.get("csharp"), Type.BUNDLED, null, "a4813b6d879c4ec852747c175cdd6141", 1L, now);
    verify(pluginsByKey.get("new-measures"), Type.EXTERNAL, null, "6d24712cf701c41ce5eaa948e0bd6d22", 1L, 1L);
  }

  private static ServerPlugin newPlugin(String key, File file, @Nullable String basePlugin) {
    return newPlugin(key, PluginType.BUNDLED, file, basePlugin);
  }

  private static ServerPlugin newPlugin(String key, PluginType type, File file, @Nullable String basePlugin) {
    PluginFilesAndMd5.FileAndMd5 jar = new PluginFilesAndMd5.FileAndMd5(file);
    PluginInfo info = new PluginInfo(key)
      .setBasePlugin(basePlugin)
      .setJarFile(file);
    return new ServerPlugin(info, PluginType.BUNDLED, null, jar, null, null);
  }

  private Map<String, PluginDto> selectAllPlugins() {
    return dbTester.getDbClient().pluginDao().selectAll(dbTester.getSession()).stream()
      .collect(uniqueIndex(PluginDto::getKee));
  }

  private void verify(PluginDto java, Type type, @Nullable String basePluginKey, String fileHash, @Nullable Long createdAt, long updatedAt) {
    assertThat(java.getBasePluginKey()).isEqualTo(basePluginKey);
    assertThat(java.getType()).isEqualTo(type);
    assertThat(java.getFileHash()).isEqualTo(fileHash);
    assertThat(java.getCreatedAt()).isEqualTo(createdAt);
    assertThat(java.getUpdatedAt()).isEqualTo(updatedAt);
  }

}
