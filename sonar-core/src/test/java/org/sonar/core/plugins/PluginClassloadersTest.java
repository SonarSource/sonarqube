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
package org.sonar.core.plugins;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.Plugin;
import org.sonar.api.platform.PluginMetadata;
import org.sonar.api.utils.SonarException;

import java.io.File;
import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PluginClassloadersTest {

  private PluginClassloaders classloaders;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void before() {
    classloaders = new PluginClassloaders(getClass().getClassLoader());
  }

  @After
  public void clean() {
    if (classloaders != null) {
      classloaders.clean();
    }
  }

  @Test
  public void shouldImport() throws Exception {
    classloaders.add(DefaultPluginMetadata.create("foo").addDeployedFile(getFile("PluginClassloadersTest/foo.jar")));
    classloaders.add(DefaultPluginMetadata.create("bar").addDeployedFile(getFile("PluginClassloadersTest/bar.jar")));
    classloaders.done();

    String resourceName = "org/sonar/plugins/bar/api/resource.txt";
    assertThat(classloaders.get("bar").getResourceAsStream(resourceName)).isNotNull();
    assertThat(classloaders.get("foo").getResourceAsStream(resourceName)).isNotNull();
  }

  @Test
  public void shouldCreateBaseClassloader() {
    classloaders = new PluginClassloaders(getClass().getClassLoader());
    DefaultPluginMetadata checkstyle = DefaultPluginMetadata.create("checkstyle")
      .setMainClass("org.sonar.plugins.checkstyle.CheckstylePlugin")
      .addDeployedFile(getFile("sonar-checkstyle-plugin-2.8.jar"));

    Map<String, Plugin> map = classloaders.init(Arrays.<PluginMetadata>asList(checkstyle));

    Plugin checkstyleEntryPoint = map.get("checkstyle");
    ClassRealm checkstyleRealm = (ClassRealm) checkstyleEntryPoint.getClass().getClassLoader();
    assertThat(checkstyleRealm.getId()).isEqualTo("checkstyle");
  }

  @Test
  public void shouldExtendPlugin() {
    classloaders = new PluginClassloaders(getClass().getClassLoader());

    DefaultPluginMetadata checkstyle = DefaultPluginMetadata.create("checkstyle")
      .setMainClass("org.sonar.plugins.checkstyle.CheckstylePlugin")
      .addDeployedFile(getFile("sonar-checkstyle-plugin-2.8.jar"));

    DefaultPluginMetadata checkstyleExt = DefaultPluginMetadata.create("checkstyle-ext")
      .setBasePlugin("checkstyle")
      .setMainClass("com.mycompany.sonar.checkstyle.CheckstyleExtensionsPlugin")
      .addDeployedFile(getFile("sonar-checkstyle-extensions-plugin-0.1-SNAPSHOT.jar"));

    Map<String, Plugin> map = classloaders.init(Arrays.<PluginMetadata>asList(checkstyle, checkstyleExt));

    Plugin checkstyleEntryPoint = map.get("checkstyle");
    Plugin checkstyleExtEntryPoint = map.get("checkstyle-ext");

    assertThat(checkstyleEntryPoint.getClass().getClassLoader().equals(checkstyleExtEntryPoint.getClass().getClassLoader())).isTrue();
  }

  @Test
  public void detect_plugins_compiled_for_bad_java_version() throws Exception {
    thrown.expect(SonarException.class);
    thrown.expectMessage("The plugin checkstyle is not supported with Java 1.");

    ClassWorld world = mock(ClassWorld.class);
    when(world.newRealm(anyString(), any(ClassLoader.class))).thenThrow(new UnsupportedClassVersionError());

    classloaders = new PluginClassloaders(getClass().getClassLoader(), world);

    DefaultPluginMetadata checkstyle = DefaultPluginMetadata.create("checkstyle")
      .setMainClass("org.sonar.plugins.checkstyle.CheckstylePlugin")
      .addDeployedFile(getFile("sonar-checkstyle-plugin-2.8.jar"));

    classloaders.init(Arrays.<PluginMetadata>asList(checkstyle));
  }

  private File getFile(String filename) {
    return FileUtils.toFile(getClass().getResource("/org/sonar/core/plugins/" + filename));
  }
}
