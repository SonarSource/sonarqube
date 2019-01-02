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
import org.sonar.server.plugins.InstalledPlugin;
import org.sonar.server.plugins.PluginFileSystem;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RegisterPluginsTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private DbClient dbClient = dbTester.getDbClient();
  private PluginFileSystem pluginFileSystem = mock(PluginFileSystem.class);
  private UuidFactory uuidFactory = mock(UuidFactory.class);
  private System2 system2 = mock(System2.class);

  @Before
  public void setUp() {
    when(system2.now()).thenReturn(12345L).thenThrow(new IllegalStateException("Should be called only once"));
  }

  /**
   * Insert new plugins
   */
  @Test
  public void insert_new_plugins() throws IOException {
    dbTester.prepareDbUnit(getClass(), "insert_new_plugins.xml");

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

    dbTester.assertDbUnit(getClass(), "insert_new_plugins-result.xml", "plugins");

    register.stop();
  }

  /**
   * Update existing plugins, only when checksum is different and don't remove uninstalled plugins
   */
  @Test
  public void update_only_changed_plugins() throws IOException {
    dbTester.prepareDbUnit(getClass(), "update_only_changed_plugins.xml");

    File fakeJavaCustomJar = temp.newFile();
    FileUtils.write(fakeJavaCustomJar, "fakejavacustomchanged", StandardCharsets.UTF_8);
    when(pluginFileSystem.getInstalledFiles()).thenReturn(asList(
      newPlugin("javacustom", fakeJavaCustomJar, "java2")));

    new RegisterPlugins(pluginFileSystem, dbClient, uuidFactory, system2).start();

    dbTester.assertDbUnit(getClass(), "update_only_changed_plugins-result.xml", "plugins");
  }

  private static InstalledPlugin newPlugin(String key, File file, @Nullable String basePlugin) {
    InstalledPlugin.FileAndMd5 jar = new InstalledPlugin.FileAndMd5(file);
    PluginInfo info = new PluginInfo(key)
      .setBasePlugin(basePlugin)
      .setJarFile(file);
    return new InstalledPlugin(info, jar, null);
  }

}
