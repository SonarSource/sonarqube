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
package org.sonar.plugins.cpd;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Project;
import org.sonar.plugins.cpd.index.IndexFactory;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class CpdSensorTest {

  SonarEngine sonarEngine;
  SonarBridgeEngine sonarBridgeEngine;
  CpdSensor sensor;
  Settings settings;

  @Before
  public void setUp() {
    IndexFactory indexFactory = mock(IndexFactory.class);
    sonarEngine = new SonarEngine(indexFactory, null, null, null);
    sonarBridgeEngine = new SonarBridgeEngine(indexFactory, null, null);
    settings = new Settings(new PropertyDefinitions(CpdPlugin.class));
    sensor = new CpdSensor(sonarEngine, sonarBridgeEngine, settings);
  }

  @Test
  public void test_global_skip() {
    settings.setProperty("sonar.cpd.skip", true);
    assertThat(sensor.isSkipped(createJavaProject())).isTrue();
  }

  @Test
  public void should_not_skip_by_default() {
    assertThat(sensor.isSkipped(createJavaProject())).isFalse();
  }

  @Test
  public void should_skip_by_language() {
    settings.setProperty("sonar.cpd.skip", false);
    settings.setProperty("sonar.cpd.php.skip", true);
    assertThat(sensor.isSkipped(createPhpProject())).isTrue();
    assertThat(sensor.isSkipped(createJavaProject())).isFalse();
  }

  @Test
  public void test_engine() {
    assertThat(sensor.getEngine(createJavaProject())).isSameAs(sonarEngine);
    assertThat(sensor.getEngine(createPhpProject())).isSameAs(sonarBridgeEngine);
  }

  private Project createJavaProject() {
    PropertiesConfiguration conf = new PropertiesConfiguration();
    conf.setProperty("sonar.language", "java");
    return new Project("java_project").setConfiguration(conf).setLanguage(Java.INSTANCE);
  }

  private Project createPhpProject() {
    PropertiesConfiguration conf = new PropertiesConfiguration();
    conf.setProperty("sonar.language", "php");
    Language phpLanguage = mock(Language.class);
    return new Project("php_project").setConfiguration(conf).setLanguage(phpLanguage);
  }

}
