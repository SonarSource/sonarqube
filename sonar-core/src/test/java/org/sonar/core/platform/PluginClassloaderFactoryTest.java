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
package org.sonar.core.platform;

import com.sonarsource.plugins.license.api.FooBar;
import java.io.File;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.sonar.api.server.rule.RulesDefinition;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

public class PluginClassloaderFactoryTest {

  static final String BASE_PLUGIN_CLASSNAME = "org.sonar.plugins.base.BasePlugin";
  static final String DEPENDENT_PLUGIN_CLASSNAME = "org.sonar.plugins.dependent.DependentPlugin";
  static final String BASE_PLUGIN_KEY = "base";
  static final String DEPENDENT_PLUGIN_KEY = "dependent";

  PluginClassloaderFactory factory = new PluginClassloaderFactory();

  @Test
  public void create_isolated_classloader() {
    PluginClassLoaderDef def = basePluginDef();
    Map<PluginClassLoaderDef, ClassLoader> map = factory.create(emptyMap(), asList(def));

    assertThat(map).containsOnlyKeys(def);
    ClassLoader classLoader = map.get(def);

    // plugin can access to API classes, and of course to its own classes !
    assertThat(canLoadClass(classLoader, RulesDefinition.class.getCanonicalName())).isTrue();
    assertThat(canLoadClass(classLoader, BASE_PLUGIN_CLASSNAME)).isTrue();

    // plugin can not access to core classes
    assertThat(canLoadClass(classLoader, PluginClassloaderFactory.class.getCanonicalName())).isFalse();
    assertThat(canLoadClass(classLoader, Test.class.getCanonicalName())).isFalse();
    assertThat(canLoadClass(classLoader, StringUtils.class.getCanonicalName())).isFalse();
  }

  @Test
  public void classloader_exports_resources_to_other_classloaders() {
    PluginClassLoaderDef baseDef = basePluginDef();
    PluginClassLoaderDef dependentDef = dependentPluginDef();
    Map<PluginClassLoaderDef, ClassLoader> map = factory.create(emptyMap(), asList(baseDef, dependentDef));
    ClassLoader baseClassloader = map.get(baseDef);
    ClassLoader dependentClassloader = map.get(dependentDef);

    // base-plugin exports its API package to other plugins
    assertThat(canLoadClass(dependentClassloader, "org.sonar.plugins.base.api.BaseApi")).isTrue();
    assertThat(canLoadClass(dependentClassloader, BASE_PLUGIN_CLASSNAME)).isFalse();
    assertThat(canLoadClass(dependentClassloader, DEPENDENT_PLUGIN_CLASSNAME)).isTrue();

    // dependent-plugin does not export its classes
    assertThat(canLoadClass(baseClassloader, DEPENDENT_PLUGIN_CLASSNAME)).isFalse();
    assertThat(canLoadClass(baseClassloader, BASE_PLUGIN_CLASSNAME)).isTrue();
  }

  @Test
  public void classloader_exports_resources_to_other_classloaders_loaded_later() {
    PluginClassLoaderDef baseDef = basePluginDef();
    Map<PluginClassLoaderDef, ClassLoader> map1 = factory.create(emptyMap(), List.of(baseDef));

    PluginClassLoaderDef dependentDef = dependentPluginDef();
    Map<PluginClassLoaderDef, ClassLoader> map2 = factory.create(map1, List.of(dependentDef));

    ClassLoader dependentClassloader = map2.get(dependentDef);

    // base-plugin exports its API package to other plugins
    assertThat(canLoadClass(dependentClassloader, "org.sonar.plugins.base.api.BaseApi")).isTrue();
    assertThat(canLoadClass(dependentClassloader, BASE_PLUGIN_CLASSNAME)).isFalse();
    assertThat(canLoadClass(dependentClassloader, DEPENDENT_PLUGIN_CLASSNAME)).isTrue();
  }

  @Test
  public void classloader_exposes_license_api_from_main_classloader() {
    PluginClassLoaderDef def = basePluginDef();
    Map<PluginClassLoaderDef, ClassLoader> map = factory.create(emptyMap(), asList(def));

    assertThat(map).containsOnlyKeys(def);
    ClassLoader classLoader = map.get(def);

    assertThat(canLoadClass(classLoader, FooBar.class.getCanonicalName())).isTrue();
  }

  private static PluginClassLoaderDef basePluginDef() {
    PluginClassLoaderDef def = new PluginClassLoaderDef(BASE_PLUGIN_KEY);
    def.addMainClass(BASE_PLUGIN_KEY, BASE_PLUGIN_CLASSNAME);
    def.getExportMask().include("org/sonar/plugins/base/api/");
    def.addFiles(asList(fakePluginJar("base-plugin/target/base-plugin-0.1-SNAPSHOT.jar")));
    return def;
  }

  private static PluginClassLoaderDef dependentPluginDef() {
    PluginClassLoaderDef def = new PluginClassLoaderDef(DEPENDENT_PLUGIN_KEY);
    def.addMainClass(DEPENDENT_PLUGIN_KEY, DEPENDENT_PLUGIN_CLASSNAME);
    def.getExportMask().include("org/sonar/plugins/dependent/api/");
    def.addFiles(asList(fakePluginJar("dependent-plugin/target/dependent-plugin-0.1-SNAPSHOT.jar")));
    return def;
  }

  private static File fakePluginJar(String path) {
    // Maven way
    File file = new File("src/test/projects/" + path);
    if (!file.exists()) {
      // Intellij way
      file = new File("sonar-core/src/test/projects/" + path);
      if (!file.exists()) {
        throw new IllegalArgumentException("Fake projects are not built: " + path);
      }
    }
    return file;
  }

  private static boolean canLoadClass(ClassLoader classloader, String classname) {
    try {
      classloader.loadClass(classname);
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }
}
