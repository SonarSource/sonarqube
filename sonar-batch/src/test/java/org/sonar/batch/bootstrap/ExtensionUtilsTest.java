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
    assertThat(ExtensionUtils.isInstantiationStrategy(BatchService.class, InstantiationStrategy.BATCH), is(true));
    assertThat(ExtensionUtils.isInstantiationStrategy(new BatchService(), InstantiationStrategy.BATCH), is(true));
    assertThat(ExtensionUtils.isInstantiationStrategy(ProjectService.class, InstantiationStrategy.BATCH), is(false));
    assertThat(ExtensionUtils.isInstantiationStrategy(new ProjectService(), InstantiationStrategy.BATCH), is(false));
    assertThat(ExtensionUtils.isInstantiationStrategy(DefaultService.class, InstantiationStrategy.BATCH), is(false));
    assertThat(ExtensionUtils.isInstantiationStrategy(new DefaultService(), InstantiationStrategy.BATCH), is(false));
  }

  @Test
  public void shouldBeProjectInstantiationStrategy() {
    assertThat(ExtensionUtils.isInstantiationStrategy(BatchService.class, InstantiationStrategy.PROJECT), is(false));
    assertThat(ExtensionUtils.isInstantiationStrategy(new BatchService(), InstantiationStrategy.PROJECT), is(false));
    assertThat(ExtensionUtils.isInstantiationStrategy(ProjectService.class, InstantiationStrategy.PROJECT), is(true));
    assertThat(ExtensionUtils.isInstantiationStrategy(new ProjectService(), InstantiationStrategy.PROJECT), is(true));
    assertThat(ExtensionUtils.isInstantiationStrategy(DefaultService.class, InstantiationStrategy.PROJECT), is(true));
    assertThat(ExtensionUtils.isInstantiationStrategy(new DefaultService(), InstantiationStrategy.PROJECT), is(true));
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
    assertThat(ExtensionUtils.supportsEnvironment(new MavenService(), new EnvironmentInformation("maven", "2.2.1")), is(true));
    assertThat(ExtensionUtils.supportsEnvironment(new BuildToolService(), new EnvironmentInformation("maven", "2.2.1")), is(true));
    assertThat(ExtensionUtils.supportsEnvironment(new DefaultService(), new EnvironmentInformation("maven", "2.2.1")), is(true));

    assertThat(ExtensionUtils.supportsEnvironment(new BuildToolService(), new EnvironmentInformation("eclipse", "0.1")), is(false));
  }

  @Test
  public void shouldBeMavenExtensionOnly() {
    assertThat(ExtensionUtils.isMavenExtensionOnly(MavenService.class), is(true));
    assertThat(ExtensionUtils.isMavenExtensionOnly(BuildToolService.class), is(false));
  }

//  @Test
//  public void shouldCheckDryRun() {
//    assertThat(ExtensionUtils.supportsDryRun(BatchService.class, true), is(true));
//    assertThat(ExtensionUtils.supportsDryRun(PersistentService.class, true), is(false));
//  }
//
//  @Test
//  public void shouldNotCheckDryRun() {
//    assertThat(ExtensionUtils.supportsDryRun(BatchService.class, false), is(true));
//    assertThat(ExtensionUtils.supportsDryRun(PersistentService.class, false), is(true));
//  }

  @InstantiationStrategy(InstantiationStrategy.BATCH)
  public static class BatchService implements BatchExtension {

  }

  @InstantiationStrategy(InstantiationStrategy.PROJECT)
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
