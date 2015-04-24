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
package org.sonar.core.platform;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.FileUtils;
import org.assertj.core.data.MapEntry;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.Plugin;
import org.sonar.api.utils.ZipUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class PluginLoaderTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void complete_test() throws Exception {
    File checkstyleJar = FileUtils.toFile(getClass().getResource("/org/sonar/core/plugins/sonar-checkstyle-plugin-2.8.jar"));
    PluginInfo checkstyleInfo = PluginInfo.create(checkstyleJar);

    PluginLoader loader = new PluginLoader(new TempPluginUnzipper());
    Map<String, Plugin> instances = loader.load(ImmutableMap.of("checkstyle", checkstyleInfo));

    assertThat(instances).containsOnlyKeys("checkstyle");
    Plugin checkstyleInstance = instances.get("checkstyle");
    assertThat(checkstyleInstance.getClass().getName()).isEqualTo("org.sonar.plugins.checkstyle.CheckstylePlugin");

    loader.unload(instances.values());
    // should test that classloaders are closed
  }

  @Test
  public void define_plugin_classloader__nominal() throws Exception {
    PluginInfo info = new PluginInfo("foo")
      .setName("Foo")
      .setMainClass("org.foo.FooPlugin");
    File jarFile = temp.newFile();
    info.setFile(jarFile);

    PluginLoader loader = new PluginLoader(new BasicPluginUnzipper());
    Map<String, PluginLoader.ClassloaderDef> defs = loader.defineClassloaders(ImmutableMap.of("foo", info));

    assertThat(defs).containsOnlyKeys("foo");
    PluginLoader.ClassloaderDef def = defs.get("foo");
    assertThat(def.basePluginKey).isEqualTo("foo");
    assertThat(def.selfFirstStrategy).isFalse();
    assertThat(def.files).containsOnly(jarFile);
    assertThat(def.mainClassesByPluginKey).containsOnly(MapEntry.entry("foo", "org.foo.FooPlugin"));
    // TODO test mask - require change in sonar-classloader
  }

  @Test
  public void define_plugin_classloader__extend_base_plugin() throws Exception {
    File baseJarFile = temp.newFile(), extensionJarFile = temp.newFile();
    PluginInfo base = new PluginInfo("foo")
      .setName("Foo")
      .setMainClass("org.foo.FooPlugin")
      .setFile(baseJarFile);
    PluginInfo extension = new PluginInfo("fooContrib")
      .setName("Foo Contrib")
      .setMainClass("org.foo.ContribPlugin")
      .setFile(extensionJarFile)
      .setBasePlugin("foo")

      // not a base plugin, can't override base metadata -> will be ignored
      .setUseChildFirstClassLoader(true);

    PluginLoader loader = new PluginLoader(new BasicPluginUnzipper());
    Map<String, PluginLoader.ClassloaderDef> defs = loader.defineClassloaders(ImmutableMap.of("foo", base, "fooContrib", extension));

    assertThat(defs).containsOnlyKeys("foo");
    PluginLoader.ClassloaderDef def = defs.get("foo");
    assertThat(def.basePluginKey).isEqualTo("foo");
    assertThat(def.selfFirstStrategy).isFalse();
    assertThat(def.files).containsOnly(baseJarFile, extensionJarFile);
    assertThat(def.mainClassesByPluginKey).containsOnly(MapEntry.entry("foo", "org.foo.FooPlugin"), entry("fooContrib", "org.foo.ContribPlugin"));
    // TODO test mask - require change in sonar-classloader
  }

  /**
   * Does not unzip jar file.
   */
  private class BasicPluginUnzipper extends PluginUnzipper {
    @Override
    public UnzippedPlugin unzip(PluginInfo info) {
      return new UnzippedPlugin(info.getKey(), info.getFile(), Collections.<File>emptyList());
    }
  }

  private class TempPluginUnzipper extends PluginUnzipper {
    @Override
    public UnzippedPlugin unzip(PluginInfo info) {
      try {
        File tempDir = temp.newFolder();
        ZipUtils.unzip(info.getFile(), tempDir, newLibFilter());
        return UnzippedPlugin.createFromUnzippedDir(info.getKey(), info.getFile(), tempDir);

      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }
}
