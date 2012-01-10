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

import org.junit.Test;
import org.sonar.api.BatchExtension;
import org.sonar.api.ServerExtension;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.batch.SupportedEnvironment;
import org.sonar.batch.bootstrapper.EnvironmentInformation;
import org.sonar.core.NotDryRun;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ExtensionUtilsTest {

  @Test
  public void shouldBeBatchInstantiationStrategy() {
    assertThat(ExtensionUtils.isInstantiationStrategy(BatchService.class, InstantiationStrategy.PER_BATCH), is(true));
    assertThat(ExtensionUtils.isInstantiationStrategy(new BatchService(), InstantiationStrategy.PER_BATCH), is(true));
    assertThat(ExtensionUtils.isInstantiationStrategy(ProjectService.class, InstantiationStrategy.PER_BATCH), is(false));
    assertThat(ExtensionUtils.isInstantiationStrategy(new ProjectService(), InstantiationStrategy.PER_BATCH), is(false));
    assertThat(ExtensionUtils.isInstantiationStrategy(DefaultService.class, InstantiationStrategy.PER_BATCH), is(false));
    assertThat(ExtensionUtils.isInstantiationStrategy(new DefaultService(), InstantiationStrategy.PER_BATCH), is(false));
  }

  @Test
  public void shouldBeProjectInstantiationStrategy() {
    assertThat(ExtensionUtils.isInstantiationStrategy(BatchService.class, InstantiationStrategy.PER_PROJECT), is(false));
    assertThat(ExtensionUtils.isInstantiationStrategy(new BatchService(), InstantiationStrategy.PER_PROJECT), is(false));
    assertThat(ExtensionUtils.isInstantiationStrategy(ProjectService.class, InstantiationStrategy.PER_PROJECT), is(true));
    assertThat(ExtensionUtils.isInstantiationStrategy(new ProjectService(), InstantiationStrategy.PER_PROJECT), is(true));
    assertThat(ExtensionUtils.isInstantiationStrategy(DefaultService.class, InstantiationStrategy.PER_PROJECT), is(true));
    assertThat(ExtensionUtils.isInstantiationStrategy(new DefaultService(), InstantiationStrategy.PER_PROJECT), is(true));
  }

  @Test
  public void testIsBatchExtension() {
    assertThat(ExtensionUtils.isBatchExtension(BatchService.class), is(true));
    assertThat(ExtensionUtils.isBatchExtension(new BatchService()), is(true));

    assertThat(ExtensionUtils.isBatchExtension(ServerService.class), is(false));
    assertThat(ExtensionUtils.isBatchExtension(new ServerService()), is(false));
  }

  @Test
  public void shouldCheckEnvironment() {
    assertThat(ExtensionUtils.isSupportedEnvironment(new MavenService(), new EnvironmentInformation("maven", "2.2.1")), is(true));
    assertThat(ExtensionUtils.isSupportedEnvironment(new BuildToolService(), new EnvironmentInformation("maven", "2.2.1")), is(true));
    assertThat(ExtensionUtils.isSupportedEnvironment(new DefaultService(), new EnvironmentInformation("maven", "2.2.1")), is(true));

    assertThat(ExtensionUtils.isSupportedEnvironment(new BuildToolService(), new EnvironmentInformation("eclipse", "0.1")), is(false));
  }

  @Test
  public void shouldBeMavenExtensionOnly() {
    assertThat(ExtensionUtils.isMavenExtensionOnly(MavenService.class), is(true));
    assertThat(ExtensionUtils.isMavenExtensionOnly(BuildToolService.class), is(false));
  }

  @Test
  public void shouldCheckDryRun() {
    assertThat(ExtensionUtils.checkDryRun(BatchService.class, true), is(true));
    assertThat(ExtensionUtils.checkDryRun(PersistentService.class, true), is(false));
  }

  @Test
  public void shouldNotCheckDryRun() {
    assertThat(ExtensionUtils.checkDryRun(BatchService.class, false), is(true));
    assertThat(ExtensionUtils.checkDryRun(PersistentService.class, false), is(true));
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

  @NotDryRun
  public static class PersistentService implements BatchExtension {

  }
}
