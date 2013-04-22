/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.batch.bootstrap;

import org.junit.Test;
import org.sonar.api.BatchExtension;
import org.sonar.api.ServerExtension;
import org.sonar.core.DryRunIncompatible;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.batch.SupportedEnvironment;
import org.sonar.batch.bootstrapper.EnvironmentInformation;

import static org.fest.assertions.Assertions.assertThat;

public class ExtensionUtilsTest {

  @Test
  public void shouldBeBatchInstantiationStrategy() {
    assertThat(ExtensionUtils.isInstantiationStrategy(BatchService.class, InstantiationStrategy.PER_BATCH)).isTrue();
    assertThat(ExtensionUtils.isInstantiationStrategy(new BatchService(), InstantiationStrategy.PER_BATCH)).isTrue();
    assertThat(ExtensionUtils.isInstantiationStrategy(ProjectService.class, InstantiationStrategy.PER_BATCH)).isFalse();
    assertThat(ExtensionUtils.isInstantiationStrategy(new ProjectService(), InstantiationStrategy.PER_BATCH)).isFalse();
    assertThat(ExtensionUtils.isInstantiationStrategy(DefaultService.class, InstantiationStrategy.PER_BATCH)).isFalse();
    assertThat(ExtensionUtils.isInstantiationStrategy(new DefaultService(), InstantiationStrategy.PER_BATCH)).isFalse();
  }

  @Test
  public void shouldBeProjectInstantiationStrategy() {
    assertThat(ExtensionUtils.isInstantiationStrategy(BatchService.class, InstantiationStrategy.PER_PROJECT)).isFalse();
    assertThat(ExtensionUtils.isInstantiationStrategy(new BatchService(), InstantiationStrategy.PER_PROJECT)).isFalse();
    assertThat(ExtensionUtils.isInstantiationStrategy(ProjectService.class, InstantiationStrategy.PER_PROJECT)).isTrue();
    assertThat(ExtensionUtils.isInstantiationStrategy(new ProjectService(), InstantiationStrategy.PER_PROJECT)).isTrue();
    assertThat(ExtensionUtils.isInstantiationStrategy(DefaultService.class, InstantiationStrategy.PER_PROJECT)).isTrue();
    assertThat(ExtensionUtils.isInstantiationStrategy(new DefaultService(), InstantiationStrategy.PER_PROJECT)).isTrue();
  }

  @Test
  public void testIsBatchExtension() {
    assertThat(ExtensionUtils.isBatchExtension(BatchService.class)).isTrue();
    assertThat(ExtensionUtils.isBatchExtension(new BatchService())).isTrue();

    assertThat(ExtensionUtils.isBatchExtension(ServerService.class)).isFalse();
    assertThat(ExtensionUtils.isBatchExtension(new ServerService())).isFalse();
  }

  @Test
  public void shouldCheckEnvironment() {
    assertThat(ExtensionUtils.supportsEnvironment(new MavenService(), new EnvironmentInformation("maven", "2.2.1"))).isTrue();
    assertThat(ExtensionUtils.supportsEnvironment(new BuildToolService(), new EnvironmentInformation("maven", "2.2.1"))).isTrue();
    assertThat(ExtensionUtils.supportsEnvironment(new DefaultService(), new EnvironmentInformation("maven", "2.2.1"))).isTrue();

    assertThat(ExtensionUtils.supportsEnvironment(new BuildToolService(), new EnvironmentInformation("eclipse", "0.1"))).isFalse();
  }

  @Test
  public void shouldBeMavenExtensionOnly() {
    assertThat(ExtensionUtils.isMavenExtensionOnly(DefaultService.class)).isFalse();
    assertThat(ExtensionUtils.isMavenExtensionOnly(new DefaultService())).isFalse();
    assertThat(ExtensionUtils.isMavenExtensionOnly(MavenService.class)).isTrue();
    assertThat(ExtensionUtils.isMavenExtensionOnly(new MavenService())).isTrue();
    assertThat(ExtensionUtils.isMavenExtensionOnly(BuildToolService.class)).isFalse();
    assertThat(ExtensionUtils.isMavenExtensionOnly(new BuildToolService())).isFalse();
  }

  @Test
  public void shouldSupportDryRun() {
    assertThat(ExtensionUtils.supportsDryRun(BatchService.class)).isTrue();
    assertThat(ExtensionUtils.supportsDryRun(new BatchService())).isTrue();
    assertThat(ExtensionUtils.supportsDryRun(PersistentService.class)).isFalse();
    assertThat(ExtensionUtils.supportsDryRun(new PersistentService())).isFalse();
  }

  @InstantiationStrategy(InstantiationStrategy.PER_BATCH)
  public static class BatchService implements BatchExtension {

  }

  @InstantiationStrategy(InstantiationStrategy.PER_PROJECT)
  public static class ProjectService implements BatchExtension {

  }

  public static class DefaultService implements BatchExtension {

  }

  public static class ServerService implements ServerExtension {

  }

  @SupportedEnvironment("maven")
  public static class MavenService implements BatchExtension {

  }

  @SupportedEnvironment({"maven", "ant", "gradle"})
  public static class BuildToolService implements BatchExtension {

  }

  @DryRunIncompatible
  public static class PersistentService implements BatchExtension {

  }
}
