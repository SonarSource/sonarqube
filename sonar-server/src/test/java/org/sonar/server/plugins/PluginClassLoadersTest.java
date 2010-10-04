/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.server.plugins;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.sonar.test.TestUtils;

import java.io.File;
import java.io.IOException;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

public class PluginClassLoadersTest {

  @Test
  public void createClassloaderFromJar() throws IOException {
    // foo-plugin.jar is a simple plugin with correct metadata.
    // It just includes the file foo.txt
    File jar = getFile("foo-plugin.jar");
    PluginMetadata metadata = PluginMetadata.createFromJar(jar, false);
    metadata.addDeployedFile(jar);
        
    assertNull(getClass().getClassLoader().getResource("foo.txt"));

    PluginClassLoaders classloaders = new PluginClassLoaders();
    ClassLoader classloader = classloaders.create(metadata);

    assertNotNull(classloader);
    assertNotNull(classloader.getResource("foo.txt"));
  }

  @Test
  public void shouldGetClassByName() throws IOException {
    File jar = getFile("sonar-build-breaker-plugin-0.1.jar");

    PluginClassLoaders classloaders = new PluginClassLoaders();
    classloaders.create("build-breaker", Lists.<File>newArrayList(jar));

    assertNotNull(classloaders.getClass("build-breaker", "org.sonar.plugins.buildbreaker.BuildBreakerPlugin"));
    assertNull(classloaders.getClass("build-breaker", "org.sonar.plugins.buildbreaker.Unknown"));
    assertNull(classloaders.getClass("unknown", "org.sonar.plugins.buildbreaker.BuildBreakerPlugin"));
  }

  private File getFile(String filename) {
    return TestUtils.getResource(PluginClassLoadersTest.class, filename);
  }
}
