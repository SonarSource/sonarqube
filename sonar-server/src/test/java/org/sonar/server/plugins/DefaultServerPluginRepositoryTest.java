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
package org.sonar.server.plugins;

import org.junit.After;
import org.junit.Test;
import org.sonar.api.platform.PluginMetadata;
import org.sonar.core.plugins.DefaultPluginMetadata;
import org.sonar.test.TestUtils;

import java.io.File;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultServerPluginRepositoryTest {

  DefaultServerPluginRepository repository;

  @After
  public void stop() {
    if (repository != null) {
      repository.stop();
    }
  }

  @Test
  public void testStart() {
    PluginDeployer deployer = mock(PluginDeployer.class);
    File pluginFile = TestUtils.getResource("/org/sonar/server/plugins/DefaultServerPluginRepositoryTest/sonar-artifact-size-plugin-0.2.jar");
    PluginMetadata plugin = DefaultPluginMetadata.create(pluginFile)
      .setKey("artifactsize")
      .setMainClass("org.sonar.plugins.artifactsize.ArtifactSizePlugin")
      .addDeployedFile(pluginFile);
    when(deployer.getMetadata()).thenReturn(Arrays.asList(plugin));

    repository = new DefaultServerPluginRepository(deployer);
    repository.start();

    assertThat(repository.getPlugin("artifactsize"), not(nullValue()));
    assertThat(repository.getClassLoader("artifactsize"), not(nullValue()));
    assertThat(repository.getClass("artifactsize", "org.sonar.plugins.artifactsize.ArtifactSizeMetrics"), not(nullValue()));
    assertThat(repository.getClass("artifactsize", "org.Unknown"), nullValue());
    assertThat(repository.getClass("other", "org.sonar.plugins.artifactsize.ArtifactSizeMetrics"), nullValue());
  }
}
