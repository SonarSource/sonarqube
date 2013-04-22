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
package org.sonar.plugins.cpd;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;

import static org.fest.assertions.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class SonarBridgeEngineTest {

  private SonarBridgeEngine engine;
  private Settings settings;

  @Before
  public void init() {
    settings = new Settings();
    engine = new SonarBridgeEngine(null, null, null, settings);
  }

  @Test
  public void shouldLogExclusions() {
    Logger logger = mock(Logger.class);
    engine.logExclusions(new String[0], logger);
    verify(logger, never()).info(anyString());

    logger = mock(Logger.class);
    engine.logExclusions(new String[] {"Foo*", "**/Bar*"}, logger);

    String message = "Copy-paste detection exclusions:"
      + "\n  Foo*"
      + "\n  **/Bar*";
    verify(logger, times(1)).info(message);
  }

  @Test
  public void shouldReturnDefaultBlockSize() {
    assertThat(SonarBridgeEngine.getDefaultBlockSize("cobol"), is(30));
    assertThat(SonarBridgeEngine.getDefaultBlockSize("natur"), is(20));
    assertThat(SonarBridgeEngine.getDefaultBlockSize("abap"), is(20));
    assertThat(SonarBridgeEngine.getDefaultBlockSize("other"), is(10));
  }

  @Test
  public void defaultBlockSize() {
    Project project = newProject("foo", "java");

    assertThat(engine.getBlockSize(project)).isEqualTo(10);
  }

  @Test
  public void blockSizeForCobol() {
    Project project = newProject("foo", "cobol");
    settings.setProperty("sonar.cpd.cobol.minimumLines", "42");

    assertThat(engine.getBlockSize(project)).isEqualTo(42);
  }

  @Test
  public void defaultMinimumTokens() {
    Project project = newProject("foo", "java");

    assertThat(engine.getMinimumTokens(project), is(CoreProperties.CPD_MINIMUM_TOKENS_DEFAULT_VALUE));
  }

  @Test
  public void generalMinimumTokens() {
    Project project = newProject("foo", "java");
    settings.setProperty("sonar.cpd.minimumTokens", 33);

    assertThat(engine.getMinimumTokens(project), is(33));
  }

  @Test
  public void minimumTokensByLanguage() {
    Project javaProject = newProject("foo", "java");
    settings.setProperty("sonar.cpd.java.minimumTokens", "42");
    settings.setProperty("sonar.cpd.php.minimumTokens", "33");
    assertThat(engine.getMinimumTokens(javaProject), is(42));

    Project phpProject = newProject("foo", "php");
    settings.setProperty("sonar.cpd.java.minimumTokens", "42");
    settings.setProperty("sonar.cpd.php.minimumTokens", "33");
    assertThat(engine.getMinimumTokens(phpProject), is(33));
  }

  private static Project newProject(String key, String language) {
    PropertiesConfiguration conf = new PropertiesConfiguration();
    conf.setProperty("sonar.language", language);
    return new Project(key).setConfiguration(conf).setAnalysisType(Project.AnalysisType.DYNAMIC);
  }
}
