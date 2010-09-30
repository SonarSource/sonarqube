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
import org.picocontainer.MutablePicoContainer;
import org.sonar.api.BatchExtension;
import org.sonar.api.ServerExtension;
import org.sonar.api.batch.AbstractCoverageExtension;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.IocContainer;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class BatchPluginRepositoryTest {

  @Test
  public void shouldRegisterBatchExtension() {
    MutablePicoContainer pico = IocContainer.buildPicoContainer();
    pico.addComponent(new PropertiesConfiguration());
    BatchPluginRepository repository = new BatchPluginRepository();

    // check classes
    assertThat(repository.shouldRegisterExtension(pico, "foo", FakeBatchExtension.class), is(true));
    assertThat(repository.shouldRegisterExtension(pico, "foo", FakeServerExtension.class), is(false));
    assertThat(repository.shouldRegisterExtension(pico, "foo", String.class), is(false));

    // check objects
    assertThat(repository.shouldRegisterExtension(pico, "foo", new FakeBatchExtension()), is(true));
    assertThat(repository.shouldRegisterExtension(pico, "foo", new FakeServerExtension()), is(false));
    assertThat(repository.shouldRegisterExtension(pico, "foo", "bar"), is(false));
  }

  @Test
  public void shouldRegisterOnlyCoberturaExtensionByDefault() {
    BatchPluginRepository repository = new BatchPluginRepository();
    PropertiesConfiguration conf = new PropertiesConfiguration();
    assertThat(repository.shouldRegisterCoverageExtension("cobertura", newJavaProject(), conf), is(true));
    assertThat(repository.shouldRegisterCoverageExtension("clover", newJavaProject(),conf), is(false));
  }

  @Test
  public void shouldRegisterCustomCoverageExtension() {
    Configuration conf = new PropertiesConfiguration();
    conf.setProperty(AbstractCoverageExtension.PARAM_PLUGIN, "clover,phpunit");
    BatchPluginRepository repository = new BatchPluginRepository();
    assertThat(repository.shouldRegisterCoverageExtension("cobertura", newJavaProject(),conf), is(false));
    assertThat(repository.shouldRegisterCoverageExtension("clover", newJavaProject(), conf), is(true));
    assertThat(repository.shouldRegisterCoverageExtension("phpunit", newJavaProject(),conf), is(true));
    assertThat(repository.shouldRegisterCoverageExtension("other", newJavaProject(),conf), is(false));
  }

  @Test
  public void shouldActivateOldVersionOfEmma() {
    Configuration conf = new PropertiesConfiguration();
    conf.setProperty(AbstractCoverageExtension.PARAM_PLUGIN, "emma");
    BatchPluginRepository repository = new BatchPluginRepository();

    assertThat(repository.shouldRegisterCoverageExtension("sonar-emma-plugin", newJavaProject(),conf), is(true));
    assertThat(repository.shouldRegisterCoverageExtension("emma", newJavaProject(),conf), is(true));

    assertThat(repository.shouldRegisterCoverageExtension("sonar-jacoco-plugin", newJavaProject(),conf), is(false));
    assertThat(repository.shouldRegisterCoverageExtension("jacoco", newJavaProject(),conf), is(false));
    assertThat(repository.shouldRegisterCoverageExtension("clover", newJavaProject(),conf), is(false));
    assertThat(repository.shouldRegisterCoverageExtension("cobertura", newJavaProject(),conf), is(false));
  }

  @Test
  public void shouldActivateOldVersionOfJacoco() {
    Configuration conf = new PropertiesConfiguration();
    conf.setProperty(AbstractCoverageExtension.PARAM_PLUGIN, "cobertura,jacoco");
    BatchPluginRepository repository = new BatchPluginRepository();

    assertThat(repository.shouldRegisterCoverageExtension("sonar-jacoco-plugin", newJavaProject(),conf), is(true));
    assertThat(repository.shouldRegisterCoverageExtension("jacoco", newJavaProject(),conf), is(true));
    assertThat(repository.shouldRegisterCoverageExtension("emma", newJavaProject(),conf), is(false));
  }

  @Test
  public void shouldNotCheckCoverageExtensionsOnNonJavaProjects() {
    Configuration conf = new PropertiesConfiguration();
    conf.setProperty(AbstractCoverageExtension.PARAM_PLUGIN, "cobertura");
    BatchPluginRepository repository = new BatchPluginRepository();

    assertThat(repository.shouldRegisterCoverageExtension("groovy", newGroovyProject(),conf), is(true));
    assertThat(repository.shouldRegisterCoverageExtension("groovy", newJavaProject(),conf), is(false));
  }

  private static Project newJavaProject() {
    return new Project("foo").setLanguageKey(Java.KEY);
  }

  private static Project newGroovyProject() {
    return new Project("foo").setLanguageKey("grvy");
  }

  public static class FakeBatchExtension implements BatchExtension {

  }

  public static class FakeServerExtension implements ServerExtension {

  }
}
