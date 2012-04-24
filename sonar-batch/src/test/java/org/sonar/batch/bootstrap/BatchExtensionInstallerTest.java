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
package org.sonar.batch.bootstrap;

import com.google.common.collect.Maps;
import org.junit.Test;
import org.sonar.api.*;
import org.sonar.api.batch.CoverageExtension;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.platform.PluginMetadata;
import org.sonar.batch.bootstrapper.EnvironmentInformation;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BatchExtensionInstallerTest {

  private static final PluginMetadata METADATA = mock(PluginMetadata.class);

  private static Map<PluginMetadata, Plugin> newPlugin(final Class... classes) {
    Map<PluginMetadata, Plugin> result = Maps.newHashMap();
    result.put(METADATA,
        new SonarPlugin() {
          public List<Class> getExtensions() {
            return Arrays.asList(classes);
          }
        }
    );
    return result;
  }

  @Test
  public void shouldInstallExtensionsWithBatchInstantiationStrategy() {
    BatchPluginRepository pluginRepository = mock(BatchPluginRepository.class);
    when(pluginRepository.getPluginsByMetadata()).thenReturn(newPlugin(BatchService.class, ProjectService.class, ServerService.class));
    Module module = new FakeModule().init();
    BatchExtensionInstaller installer = new BatchExtensionInstaller(pluginRepository, new EnvironmentInformation("ant", "1.7"), new DryRun(false));

    installer.install(module);

    assertThat(module.getComponentByType(BatchService.class), not(nullValue()));
    assertThat(module.getComponentByType(ProjectService.class), nullValue());
    assertThat(module.getComponentByType(ServerService.class), nullValue());
  }

  @Test
  public void shouldInstallProvidersWithBatchInstantiationStrategy() {
    BatchPluginRepository pluginRepository = mock(BatchPluginRepository.class);
    when(pluginRepository.getPluginsByMetadata()).thenReturn(newPlugin(BatchServiceProvider.class, ProjectServiceProvider.class));
    Module module = new FakeModule().init();
    BatchExtensionInstaller installer = new BatchExtensionInstaller(pluginRepository, new EnvironmentInformation("ant", "1.7"), new DryRun(false));

    installer.install(module);

    assertThat(module.getComponentByType(BatchService.class), not(nullValue()));
    assertThat(module.getComponentByType(ProjectService.class), nullValue());
    assertThat(module.getComponentByType(ServerService.class), nullValue());
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldNotSupportCoverageExtensionsWithBatchInstantiationStrategy() {
    // the reason is that CoverageExtensions currently depend on Project
    BatchPluginRepository pluginRepository = mock(BatchPluginRepository.class);
    when(pluginRepository.getPluginsByMetadata()).thenReturn(newPlugin(InvalidCoverageExtension.class));
    Module module = new FakeModule().init();
    BatchExtensionInstaller installer = new BatchExtensionInstaller(pluginRepository, new EnvironmentInformation("ant", "1.7"), new DryRun(false));

    installer.install(module);
  }

  public static class FakeModule extends Module {
    @Override
    protected void configure() {
    }
  }

  @InstantiationStrategy(InstantiationStrategy.PER_BATCH)
  public static class BatchService implements BatchExtension {

  }

  public static class ProjectService implements BatchExtension {

  }

  public static class ServerService implements ServerExtension {

  }

  @InstantiationStrategy(InstantiationStrategy.PER_BATCH)
  public static class BatchServiceProvider extends ExtensionProvider implements BatchExtension {

    @Override
    public Object provide() {
      return Arrays.asList(BatchService.class, ServerService.class);
    }
  }

  public static class ProjectServiceProvider extends ExtensionProvider implements BatchExtension {
    @Override
    public Object provide() {
      return ProjectService.class;
    }
  }

  @InstantiationStrategy(InstantiationStrategy.PER_BATCH)
  public static class InvalidCoverageExtension implements CoverageExtension {
    // strategy PER_BATCH is not allowed
  }
}
