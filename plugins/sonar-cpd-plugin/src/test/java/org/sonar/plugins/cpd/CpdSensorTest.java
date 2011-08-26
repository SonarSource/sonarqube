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
package org.sonar.plugins.cpd;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.CpdMapping;
import org.sonar.api.resources.Project;

public class CpdSensorTest {

  @Test
  public void generalSkip() {
    PropertiesConfiguration conf = new PropertiesConfiguration();
    conf.setProperty("sonar.cpd.skip", "true");

    Project project = createJavaProject().setConfiguration(conf);

    CpdSensor sensor = new CpdSensor(new SonarEngine(), new PmdEngine(new CpdMapping[0]));
    assertTrue(sensor.isSkipped(project));
  }

  @Test
  public void doNotSkipByDefault() {
    Project project = createJavaProject().setConfiguration(new PropertiesConfiguration());

    CpdSensor sensor = new CpdSensor(new SonarEngine(), new PmdEngine(new CpdMapping[0]));
    assertFalse(sensor.isSkipped(project));
  }

  @Test
  public void skipByLanguage() {
    PropertiesConfiguration conf = new PropertiesConfiguration();
    conf.setProperty("sonar.cpd.skip", "false");
    conf.setProperty("sonar.cpd.php.skip", "true");

    Project phpProject = createPhpProject().setConfiguration(conf);
    Project javaProject = createJavaProject().setConfiguration(conf);

    CpdSensor sensor = new CpdSensor(new SonarEngine(), new PmdEngine(new CpdMapping[0]));
    assertTrue(sensor.isSkipped(phpProject));
    assertFalse(sensor.isSkipped(javaProject));
  }

  @Test
  public void defaultMinimumTokens() {
    Project project = createJavaProject().setConfiguration(new PropertiesConfiguration());

    PmdEngine sensor = new PmdEngine(new CpdMapping[0]);
    assertEquals(CoreProperties.CPD_MINIMUM_TOKENS_DEFAULT_VALUE, sensor.getMinimumTokens(project));
  }

  @Test
  public void generalMinimumTokens() {
    PropertiesConfiguration conf = new PropertiesConfiguration();
    conf.setProperty("sonar.cpd.minimumTokens", "33");
    Project project = createJavaProject().setConfiguration(conf);

    PmdEngine sensor = new PmdEngine(new CpdMapping[0]);
    assertEquals(33, sensor.getMinimumTokens(project));
  }

  @Test
  public void minimumTokensByLanguage() {
    PropertiesConfiguration conf = new PropertiesConfiguration();
    conf.setProperty("sonar.cpd.minimumTokens", "100");
    conf.setProperty("sonar.cpd.php.minimumTokens", "33");

    Project phpProject = createPhpProject().setConfiguration(conf);
    Project javaProject = createJavaProject().setConfiguration(conf);

    PmdEngine sensor = new PmdEngine(new CpdMapping[0]);
    assertEquals(100, sensor.getMinimumTokens(javaProject));
    assertEquals(33, sensor.getMinimumTokens(phpProject));
  }

  private Project createJavaProject() {
    return new Project("java_project").setLanguageKey("java");
  }

  private Project createPhpProject() {
    return new Project("php_project").setLanguageKey("php");
  }
}
