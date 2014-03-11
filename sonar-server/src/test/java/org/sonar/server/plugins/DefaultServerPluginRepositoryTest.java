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

import org.hamcrest.core.Is;
import org.junit.After;
import org.junit.Test;
import org.sonar.api.BatchExtension;
import org.sonar.api.ExtensionProvider;
import org.sonar.api.ServerExtension;
import org.sonar.api.SonarPlugin;
import org.sonar.api.platform.PluginMetadata;
import org.sonar.core.plugins.DefaultPluginMetadata;
import org.sonar.test.TestUtils;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static junit.framework.Assert.assertFalse;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultServerPluginRepositoryTest {

  private DefaultServerPluginRepository repository;

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

  @Test
  public void shouldDisablePlugin() {
    DefaultServerPluginRepository repository = new DefaultServerPluginRepository(mock(PluginDeployer.class));
    repository.disable("checkstyle");

    assertTrue(repository.isDisabled("checkstyle"));
    assertFalse(repository.isDisabled("sqale"));
  }

  @Test
  public void shouldDisableDependentPlugins() {
    PluginDeployer deployer = mock(PluginDeployer.class);
    List<PluginMetadata> metadata = Arrays.asList(
        newMetadata("checkstyle", null),
        newMetadata("checkstyle-extensions", "checkstyle"),
        newMetadata("sqale", null)
        );
    when(deployer.getMetadata()).thenReturn(metadata);
    DefaultServerPluginRepository repository = new DefaultServerPluginRepository(deployer);

    repository.disable("checkstyle");

    assertTrue(repository.isDisabled("checkstyle"));
    assertTrue(repository.isDisabled("checkstyle-extensions"));
    assertFalse(repository.isDisabled("sqale"));
  }

  @Test
  public void shouldNotDisableBasePlugin() {
    PluginDeployer deployer = mock(PluginDeployer.class);
    List<PluginMetadata> metadata = Arrays.asList(
        newMetadata("checkstyle", null),
        newMetadata("checkstyle-extensions", "checkstyle"),
        newMetadata("sqale", null)
        );
    when(deployer.getMetadata()).thenReturn(metadata);
    DefaultServerPluginRepository repository = new DefaultServerPluginRepository(deployer);

    repository.disable("checkstyle-extensions");

    assertFalse(repository.isDisabled("checkstyle"));
    assertTrue(repository.isDisabled("checkstyle-extensions"));
  }

  private PluginMetadata newMetadata(String pluginKey, String basePluginKey) {
    PluginMetadata plugin = mock(PluginMetadata.class);
    when(plugin.getKey()).thenReturn(pluginKey);
    when(plugin.getBasePlugin()).thenReturn(basePluginKey);
    return plugin;
  }

  public static class FakePlugin extends SonarPlugin {
    private final List<Class> extensions;

    public FakePlugin(List<Class> extensions) {
      this.extensions = extensions;
    }

    public List<Class> getExtensions() {
      return extensions;
    }
  }

  public static class FakeBatchExtension implements BatchExtension {

  }

  public static class FakeServerExtension implements ServerExtension {

  }

  public static class FakeExtensionProvider extends ExtensionProvider implements ServerExtension {

    @Override
    public Object provide() {
      return Arrays.<Object> asList(FakeBatchExtension.class, FakeServerExtension.class);
    }
  }

  public static class SuperExtensionProvider extends ExtensionProvider implements ServerExtension {

    @Override
    public Object provide() {
      return FakeExtensionProvider.class;
    }
  }
}
