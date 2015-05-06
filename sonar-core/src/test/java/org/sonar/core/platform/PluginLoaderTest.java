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
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class PluginLoaderTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void load_and_unload_plugins() throws Exception {
    File checkstyleJar = FileUtils.toFile(getClass().getResource("/org/sonar/core/plugins/sonar-checkstyle-plugin-2.8.jar"));
    PluginInfo checkstyleInfo = PluginInfo.create(checkstyleJar);

    PluginLoader loader = new PluginLoader(new TempPluginExploder());
    Map<String, Plugin> instances = loader.load(ImmutableMap.of("checkstyle", checkstyleInfo));

    assertThat(instances).containsOnlyKeys("checkstyle");
    Plugin checkstyleInstance = instances.get("checkstyle");
    assertThat(checkstyleInstance.getClass().getName()).isEqualTo("org.sonar.plugins.checkstyle.CheckstylePlugin");

    loader.unload(instances.values());
    // TODO test that classloaders are closed. Two strategies:
    //
  }

  @Test
  public void define_classloader() throws Exception {
    File jarFile = temp.newFile();
    PluginInfo info = new PluginInfo("foo")
      .setJarFile(jarFile)
      .setMainClass("org.foo.FooPlugin");

    PluginLoader loader = new PluginLoader(new FakePluginExploder());
    Collection<ClassloaderDef> defs = loader.defineClassloaders(ImmutableMap.of("foo", info));

    assertThat(defs).hasSize(1);
    ClassloaderDef def = defs.iterator().next();
    assertThat(def.getBasePluginKey()).isEqualTo("foo");
    assertThat(def.isSelfFirstStrategy()).isFalse();
    assertThat(def.getFiles()).containsOnly(jarFile);
    assertThat(def.getMainClassesByPluginKey()).containsOnly(MapEntry.entry("foo", "org.foo.FooPlugin"));
    // TODO test mask - require change in sonar-classloader
  }

  /**
   * A plugin can be extended by other plugins. In this case they share the same classloader.
   * The first plugin is named "base plugin".
   */
  @Test
  public void define_same_classloader_for_multiple_plugins() throws Exception {
    File baseJarFile = temp.newFile(), extensionJar1 = temp.newFile(), extensionJar2 = temp.newFile();
    PluginInfo base = new PluginInfo("foo")
      .setJarFile(baseJarFile)
      .setMainClass("org.foo.FooPlugin")
      .setUseChildFirstClassLoader(false);

    PluginInfo extension1 = new PluginInfo("fooExtension1")
      .setJarFile(extensionJar1)
      .setMainClass("org.foo.Extension1Plugin")
      .setBasePlugin("foo");

    // This extension tries to change the classloader-ordering strategy of base plugin
    // (see setUseChildFirstClassLoader(true)).
    // That is not allowed and should be ignored -> strategy is still the one
    // defined on base plugin (parent-first in this example)
    PluginInfo extension2 = new PluginInfo("fooExtension2")
      .setJarFile(extensionJar2)
      .setMainClass("org.foo.Extension2Plugin")
      .setBasePlugin("foo")
      .setUseChildFirstClassLoader(true);

    PluginLoader loader = new PluginLoader(new FakePluginExploder());

    Collection<ClassloaderDef> defs = loader.defineClassloaders(ImmutableMap.of(
      base.getKey(), base, extension1.getKey(), extension1, extension2.getKey(), extension2));

    assertThat(defs).hasSize(1);
    ClassloaderDef def = defs.iterator().next();
    assertThat(def.getBasePluginKey()).isEqualTo("foo");
    assertThat(def.isSelfFirstStrategy()).isFalse();
    assertThat(def.getFiles()).containsOnly(baseJarFile, extensionJar1, extensionJar2);
    assertThat(def.getMainClassesByPluginKey()).containsOnly(
      entry("foo", "org.foo.FooPlugin"),
      entry("fooExtension1", "org.foo.Extension1Plugin"),
      entry("fooExtension2", "org.foo.Extension2Plugin"));
    // TODO test mask - require change in sonar-classloader
  }

  /**
   * Does not unzip jar file. It directly returns the JAR file defined on PluginInfo.
   */
  private static class FakePluginExploder extends PluginExploder {
    @Override
    public ExplodedPlugin explode(PluginInfo info) {
      return new ExplodedPlugin(info.getKey(), info.getNonNullJarFile(), Collections.<File>emptyList());
    }
  }

  private class TempPluginExploder extends PluginExploder {
    @Override
    public ExplodedPlugin explode(PluginInfo info) {
      try {
        File tempDir = temp.newFolder();
        ZipUtils.unzip(info.getNonNullJarFile(), tempDir, newLibFilter());
        return explodeFromUnzippedDir(info.getKey(), info.getNonNullJarFile(), tempDir);

      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }
}
