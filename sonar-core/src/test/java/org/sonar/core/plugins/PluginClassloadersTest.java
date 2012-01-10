/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.core.plugins;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.Plugin;
import org.sonar.api.platform.PluginMetadata;

import java.io.File;
import java.util.Arrays;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class PluginClassloadersTest {

  private PluginClassloaders classloaders;

  @Before
  public void before() {
    classloaders = new PluginClassloaders(getClass().getClassLoader());
  }

  @After
  public void clean() {
    classloaders.clean();
  }

  @Test
  public void shouldImport() throws Exception {
    classloaders.add(DefaultPluginMetadata.create(null).setKey("foo").addDeployedFile(getFile("PluginClassloadersTest/foo.jar")));
    classloaders.add(DefaultPluginMetadata.create(null).setKey("bar").addDeployedFile(getFile("PluginClassloadersTest/bar.jar")));
    classloaders.done();

    String resourceName = "org/sonar/plugins/bar/api/resource.txt";
    assertThat(classloaders.get("bar").getResourceAsStream(resourceName), notNullValue());
    assertThat(classloaders.get("foo").getResourceAsStream(resourceName), notNullValue());
  }

  @Test
  public void shouldCreateBaseClassloader() {
    classloaders = new PluginClassloaders(getClass().getClassLoader());
    DefaultPluginMetadata checkstyle = DefaultPluginMetadata.create(null)
        .setKey("checkstyle")
        .setMainClass("org.sonar.plugins.checkstyle.CheckstylePlugin")
        .addDeployedFile(getFile("sonar-checkstyle-plugin-2.8.jar"));

    Map<String, Plugin> map = classloaders.init(Arrays.<PluginMetadata>asList(checkstyle));

    Plugin checkstyleEntryPoint = map.get("checkstyle");
    ClassRealm checkstyleRealm = (ClassRealm) checkstyleEntryPoint.getClass().getClassLoader();
    assertThat(checkstyleRealm.getId(), is("checkstyle"));
  }

  @Test
  public void shouldExtendPlugin() {
    classloaders = new PluginClassloaders(getClass().getClassLoader());

    DefaultPluginMetadata checkstyle = DefaultPluginMetadata.create(null)
        .setKey("checkstyle")
        .setMainClass("org.sonar.plugins.checkstyle.CheckstylePlugin")
        .addDeployedFile(getFile("sonar-checkstyle-plugin-2.8.jar"));

    DefaultPluginMetadata checkstyleExt = DefaultPluginMetadata.create(null)
        .setKey("checkstyle-ext")
        .setBasePlugin("checkstyle")
        .setMainClass("com.mycompany.sonar.checkstyle.CheckstyleExtensionsPlugin")
        .addDeployedFile(getFile("sonar-checkstyle-extensions-plugin-0.1-SNAPSHOT.jar"));

    Map<String, Plugin> map = classloaders.init(Arrays.<PluginMetadata>asList(checkstyle, checkstyleExt));

    Plugin checkstyleEntryPoint = map.get("checkstyle");
    Plugin checkstyleExtEntryPoint = map.get("checkstyle-ext");

    assertEquals(checkstyleEntryPoint.getClass().getClassLoader(), checkstyleExtEntryPoint.getClass().getClassLoader());
  }

  private File getFile(String filename) {
    return FileUtils.toFile(getClass().getResource("/org/sonar/core/plugins/" + filename));
  }
}
