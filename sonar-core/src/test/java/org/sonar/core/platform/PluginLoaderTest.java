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
package org.sonar.core.platform;

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.assertj.core.data.MapEntry;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.updatecenter.common.Version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;

public class PluginLoaderTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public LogTester logTester = new LogTester();

  private PluginClassloaderFactory classloaderFactory = mock(PluginClassloaderFactory.class);
  private PluginLoader underTest = new PluginLoader(new FakePluginExploder(), classloaderFactory);

  @Test
  public void define_classloader() throws Exception {
    File jarFile = temp.newFile();
    PluginInfo info = new PluginInfo("foo")
      .setJarFile(jarFile)
      .setMainClass("org.foo.FooPlugin")
      .setMinimalSqVersion(Version.create("5.2"));

    Collection<PluginClassLoaderDef> defs = underTest.defineClassloaders(ImmutableMap.of("foo", info));

    assertThat(defs).hasSize(1);
    PluginClassLoaderDef def = defs.iterator().next();
    assertThat(def.getBasePluginKey()).isEqualTo("foo");
    assertThat(def.isSelfFirstStrategy()).isFalse();
    assertThat(def.getFiles()).containsOnly(jarFile);
    assertThat(def.getMainClassesByPluginKey()).containsOnly(MapEntry.entry("foo", "org.foo.FooPlugin"));
    // TODO test mask - require change in sonar-classloader
  }

  /**
   * A plugin (the "base" plugin) can be extended by other plugins. In this case they share the same classloader.
   */
  @Test
  public void test_plugins_sharing_the_same_classloader() throws Exception {
    File baseJarFile = temp.newFile();
    File extensionJar1 = temp.newFile();
    File extensionJar2 = temp.newFile();
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

    Collection<PluginClassLoaderDef> defs = underTest.defineClassloaders(ImmutableMap.of(
      base.getKey(), base, extension1.getKey(), extension1, extension2.getKey(), extension2));

    assertThat(defs).hasSize(1);
    PluginClassLoaderDef def = defs.iterator().next();
    assertThat(def.getBasePluginKey()).isEqualTo("foo");
    assertThat(def.isSelfFirstStrategy()).isFalse();
    assertThat(def.getFiles()).containsOnly(baseJarFile, extensionJar1, extensionJar2);
    assertThat(def.getMainClassesByPluginKey()).containsOnly(
      entry("foo", "org.foo.FooPlugin"),
      entry("fooExtension1", "org.foo.Extension1Plugin"),
      entry("fooExtension2", "org.foo.Extension2Plugin"));
    // TODO test mask - require change in sonar-classloader
  }

  @Test
  public void log_warning_if_plugin_uses_child_first_classloader() throws IOException {
    File jarFile = temp.newFile();
    PluginInfo info = new PluginInfo("foo")
      .setJarFile(jarFile)
      .setUseChildFirstClassLoader(true)
      .setMainClass("org.foo.FooPlugin");

    Collection<PluginClassLoaderDef> defs = underTest.defineClassloaders(ImmutableMap.of("foo", info));
    assertThat(defs).extracting(PluginClassLoaderDef::getBasePluginKey).containsExactly("foo");

    List<String> warnings = logTester.logs(LoggerLevel.WARN);
    assertThat(warnings).contains("Plugin foo [foo] uses a child first classloader which is deprecated");
  }

  @Test
  public void log_warning_if_plugin_is_built_with_api_5_2_or_lower() throws Exception {
    File jarFile = temp.newFile();
    PluginInfo info = new PluginInfo("foo")
      .setJarFile(jarFile)
      .setMainClass("org.foo.FooPlugin")
      .setMinimalSqVersion(Version.create("4.5.2"));

    Collection<PluginClassLoaderDef> defs = underTest.defineClassloaders(ImmutableMap.of("foo", info));
    assertThat(defs).extracting(PluginClassLoaderDef::getBasePluginKey).containsExactly("foo");

    List<String> warnings = logTester.logs(LoggerLevel.WARN);
    assertThat(warnings).contains("API compatibility mode is no longer supported. In case of error, plugin foo [foo] should package its dependencies.");
  }

  /**
   * Does not unzip jar file. It directly returns the JAR file defined on PluginInfo.
   */
  private static class FakePluginExploder extends PluginJarExploder {
    @Override
    public ExplodedPlugin explode(PluginInfo info) {
      return new ExplodedPlugin(info.getKey(), info.getNonNullJarFile(), Collections.emptyList());
    }
  }
}
