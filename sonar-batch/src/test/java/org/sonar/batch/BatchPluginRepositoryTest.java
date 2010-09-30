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
package org.sonar.batch;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.Test;
import org.sonar.api.BatchExtension;
import org.sonar.api.ServerExtension;
import org.sonar.api.batch.AbstractCoverageExtension;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class BatchPluginRepositoryTest {

  @Test
  public void shouldRegisterBatchExtension() {
    BatchPluginRepository repository = new BatchPluginRepository(new PropertiesConfiguration());

    // check classes
    assertThat(repository.shouldRegisterExtension("foo", FakeBatchExtension.class), is(true));
    assertThat(repository.shouldRegisterExtension("foo", FakeServerExtension.class), is(false));
    assertThat(repository.shouldRegisterExtension("foo", String.class), is(false));

    // check objects
    assertThat(repository.shouldRegisterExtension("foo", new FakeBatchExtension()), is(true));
    assertThat(repository.shouldRegisterExtension("foo", new FakeServerExtension()), is(false));
    assertThat(repository.shouldRegisterExtension("foo", "bar"), is(false));
  }

  @Test
  public void shouldRegisterOnlyCoberturaExtensionByDefault() {
    Configuration conf = new PropertiesConfiguration();
    BatchPluginRepository repository = new BatchPluginRepository(conf);
    assertThat(repository.shouldRegisterCoverageExtension("cobertura"), is(true));
    assertThat(repository.shouldRegisterCoverageExtension("clover"), is(false));
  }

  @Test
  public void shouldRegisterCustomCoverageExtension() {
    Configuration conf = new PropertiesConfiguration();
    conf.setProperty(AbstractCoverageExtension.PARAM_PLUGIN, "clover,phpunit");
    BatchPluginRepository repository = new BatchPluginRepository(conf);
    assertThat(repository.shouldRegisterCoverageExtension("cobertura"), is(false));
    assertThat(repository.shouldRegisterCoverageExtension("clover"), is(true));
    assertThat(repository.shouldRegisterCoverageExtension("phpunit"), is(true));
    assertThat(repository.shouldRegisterCoverageExtension("other"), is(false));
  }

  @Test
  public void shouldActivateOldVersionOfEmma() {
    Configuration conf = new PropertiesConfiguration();
    conf.setProperty(AbstractCoverageExtension.PARAM_PLUGIN, "emma");
    BatchPluginRepository repository = new BatchPluginRepository(conf);

    assertThat(repository.shouldRegisterCoverageExtension("sonar-emma-plugin"), is(true));
    assertThat(repository.shouldRegisterCoverageExtension("emma"), is(true));

    assertThat(repository.shouldRegisterCoverageExtension("sonar-jacoco-plugin"), is(false));
    assertThat(repository.shouldRegisterCoverageExtension("jacoco"), is(false));
    assertThat(repository.shouldRegisterCoverageExtension("clover"), is(false));
    assertThat(repository.shouldRegisterCoverageExtension("cobertura"), is(false));
  }

  @Test
  public void shouldActivateOldVersionOfJacoco() {
    Configuration conf = new PropertiesConfiguration();
    conf.setProperty(AbstractCoverageExtension.PARAM_PLUGIN, "cobertura,jacoco");
    BatchPluginRepository repository = new BatchPluginRepository(conf);

    assertThat(repository.shouldRegisterCoverageExtension("sonar-jacoco-plugin"), is(true));
    assertThat(repository.shouldRegisterCoverageExtension("jacoco"), is(true));
    assertThat(repository.shouldRegisterCoverageExtension("emma"), is(false));
  }

  public static class FakeBatchExtension implements BatchExtension {

  }

  public static class FakeServerExtension implements ServerExtension {

  }
}
