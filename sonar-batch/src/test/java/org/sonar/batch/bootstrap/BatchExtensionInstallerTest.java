/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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

import org.junit.Test;
import org.sonar.api.*;
import org.sonar.api.batch.CoverageExtension;
import org.sonar.api.batch.InstanciationStrategy;
import org.sonar.batch.bootstrapper.EnvironmentInformation;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BatchExtensionInstallerTest {

  @Test
  public void shouldInstallExtensionsWithBatchInstantiationStrategy() {
    BatchPluginRepository pluginRepository = mock(BatchPluginRepository.class);
    when(pluginRepository.getPlugins()).thenReturn(Arrays.asList((Plugin) new SonarPlugin() {
      public List getExtensions() {
        return Arrays.asList(BatchService.class, ProjectService.class, ServerService.class);
      }
    }));
    Module module = new FakeModule().init();
    BatchExtensionInstaller installer = new BatchExtensionInstaller(pluginRepository, new EnvironmentInformation("ant", "1.7"));

    installer.install(module);

    assertThat(module.getComponent(BatchService.class), not(nullValue()));
    assertThat(module.getComponent(ProjectService.class), nullValue());
    assertThat(module.getComponent(ServerService.class), nullValue());
  }

  @Test
  public void shouldInstallProvidersWithBatchInstantiationStrategy() {
    BatchPluginRepository pluginRepository = mock(BatchPluginRepository.class);
    when(pluginRepository.getPlugins()).thenReturn(Arrays.asList((Plugin) new SonarPlugin(){
      public List getExtensions() {
        return Arrays.asList(BatchServiceProvider.class, ProjectServiceProvider.class);
      }
    }));
    Module module = new FakeModule().init();
    BatchExtensionInstaller installer = new BatchExtensionInstaller(pluginRepository, new EnvironmentInformation("ant", "1.7"));

    installer.install(module);

    assertThat(module.getComponent(BatchService.class), not(nullValue()));
    assertThat(module.getComponent(ProjectService.class), nullValue());
    assertThat(module.getComponent(ServerService.class), nullValue());
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldNotSupportCoverageExtensionsWithBatchInstantiationStrategy() {
    // the reason is that CoverageExtensions currently depend on Project
    BatchPluginRepository pluginRepository = mock(BatchPluginRepository.class);
    when(pluginRepository.getPlugins()).thenReturn(Arrays.asList((Plugin) new SonarPlugin(){
      public List getExtensions() {
        return Arrays.asList(InvalidCoverageExtension.class);
      }
    }));
    Module module = new FakeModule().init();
    BatchExtensionInstaller installer = new BatchExtensionInstaller(pluginRepository, new EnvironmentInformation("ant", "1.7"));

    installer.install(module);
  }

  public static class FakeModule extends Module {
    @Override
    protected void configure() {
    }
  }

  @InstanciationStrategy(InstanciationStrategy.PER_BATCH)
  public static class BatchService implements BatchExtension {

  }

  public static class ProjectService implements BatchExtension {

  }

  public static class ServerService implements ServerExtension {

  }

  @InstanciationStrategy(InstanciationStrategy.PER_BATCH)
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

  @InstanciationStrategy(InstanciationStrategy.PER_BATCH)
  public static class InvalidCoverageExtension implements CoverageExtension {
    // strategy PER_BATCH is not allowed
  }
}
