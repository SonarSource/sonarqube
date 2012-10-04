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
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Project;
import org.sonar.plugins.cpd.index.IndexFactory;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class CpdSensorTest {

  private SonarEngine sonarEngine;
  private SonarBridgeEngine sonarBridgeEngine;
  private CpdSensor sensor;

  @Before
  public void setUp() {
    IndexFactory indexFactory = new IndexFactory(null);
    sonarEngine = new SonarEngine(indexFactory);
    sonarBridgeEngine = new SonarBridgeEngine(indexFactory);
    sensor = new CpdSensor(sonarEngine, sonarBridgeEngine);
  }

  @Test
  public void generalSkip() {
    PropertiesConfiguration conf = new PropertiesConfiguration();
    conf.setProperty("sonar.cpd.skip", "true");

    Project project = createJavaProject().setConfiguration(conf);

    assertTrue(sensor.isSkipped(project));
  }

  @Test
  public void doNotSkipByDefault() {
    Project project = createJavaProject().setConfiguration(new PropertiesConfiguration());

    assertFalse(sensor.isSkipped(project));
  }

  @Test
  public void shouldSkipByLanguage() {

    Project phpProject = createPhpProject();
    phpProject.getConfiguration().setProperty("sonar.cpd.skip", "false");
    phpProject.getConfiguration().setProperty("sonar.cpd.php.skip", "true");
    assertTrue(sensor.isSkipped(phpProject));

    Project javaProject = createJavaProject();
    javaProject.getConfiguration().setProperty("sonar.cpd.skip", "false");
    javaProject.getConfiguration().setProperty("sonar.cpd.php.skip", "true");
    assertFalse(sensor.isSkipped(javaProject));

  }

  @Test
  public void engine() {
    Project phpProject = createPhpProject();
    Project javaProject = createJavaProject();

    assertThat(sensor.getEngine(javaProject), is((CpdEngine) sonarEngine));
    assertThat(sensor.getEngine(phpProject), is((CpdEngine) sonarBridgeEngine));
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
