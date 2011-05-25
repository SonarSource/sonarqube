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
import org.sonar.api.BatchExtension;
import org.sonar.api.ServerExtension;
import org.sonar.api.batch.InstanciationStrategy;
import org.sonar.api.batch.SupportedEnvironment;
import org.sonar.batch.bootstrapper.EnvironmentInformation;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ExtensionUtilsTest {

  @Test
  public void shouldBeBatchInstantiationStrategy() {
    assertThat(ExtensionUtils.isInstantiationStrategy(BatchService.class, InstanciationStrategy.PER_BATCH), is(true));
    assertThat(ExtensionUtils.isInstantiationStrategy(new BatchService(), InstanciationStrategy.PER_BATCH), is(true));
    assertThat(ExtensionUtils.isInstantiationStrategy(ProjectService.class, InstanciationStrategy.PER_BATCH), is(false));
    assertThat(ExtensionUtils.isInstantiationStrategy(new ProjectService(), InstanciationStrategy.PER_BATCH), is(false));
    assertThat(ExtensionUtils.isInstantiationStrategy(DefaultService.class, InstanciationStrategy.PER_BATCH), is(false));
    assertThat(ExtensionUtils.isInstantiationStrategy(new DefaultService(), InstanciationStrategy.PER_BATCH), is(false));
  }

  @Test
  public void shouldBeProjectInstantiationStrategy() {
    assertThat(ExtensionUtils.isInstantiationStrategy(BatchService.class, InstanciationStrategy.PER_PROJECT), is(false));
    assertThat(ExtensionUtils.isInstantiationStrategy(new BatchService(), InstanciationStrategy.PER_PROJECT), is(false));
    assertThat(ExtensionUtils.isInstantiationStrategy(ProjectService.class, InstanciationStrategy.PER_PROJECT), is(true));
    assertThat(ExtensionUtils.isInstantiationStrategy(new ProjectService(), InstanciationStrategy.PER_PROJECT), is(true));
    assertThat(ExtensionUtils.isInstantiationStrategy(DefaultService.class, InstanciationStrategy.PER_PROJECT), is(true));
    assertThat(ExtensionUtils.isInstantiationStrategy(new DefaultService(), InstanciationStrategy.PER_PROJECT), is(true));
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

  @InstanciationStrategy(InstanciationStrategy.PER_BATCH)
  public static class BatchService implements BatchExtension {

  }

  @InstanciationStrategy(InstanciationStrategy.PER_PROJECT)
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
}
