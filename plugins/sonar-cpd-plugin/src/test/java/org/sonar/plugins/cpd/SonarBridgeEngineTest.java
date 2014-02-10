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

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;

import static org.fest.assertions.Assertions.assertThat;
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
    assertThat(SonarBridgeEngine.getDefaultBlockSize("cobol")).isEqualTo(30);
    assertThat(SonarBridgeEngine.getDefaultBlockSize("natur")).isEqualTo(20);
    assertThat(SonarBridgeEngine.getDefaultBlockSize("abap")).isEqualTo(20);
    assertThat(SonarBridgeEngine.getDefaultBlockSize("other")).isEqualTo(10);
  }

  @Test
  public void defaultBlockSize() {
    Project project = newProject("foo");

    assertThat(engine.getBlockSize(project, "java")).isEqualTo(10);
  }

  @Test
  public void blockSizeForCobol() {
    Project project = newProject("foo");
    settings.setProperty("sonar.cpd.cobol.minimumLines", "42");

    assertThat(engine.getBlockSize(project, "cobol")).isEqualTo(42);
  }

  @Test
  public void defaultMinimumTokens() {
    Project project = newProject("foo");

    assertThat(engine.getMinimumTokens(project, "java")).isEqualTo(CoreProperties.CPD_MINIMUM_TOKENS_DEFAULT_VALUE);
  }

  @Test
  public void generalMinimumTokens() {
    Project project = newProject("foo");
    settings.setProperty("sonar.cpd.minimumTokens", 33);

    assertThat(engine.getMinimumTokens(project, "java")).isEqualTo(33);
  }

  @Test
  public void minimumTokensByLanguage() {
    Project javaProject = newProject("foo");
    settings.setProperty("sonar.cpd.java.minimumTokens", "42");
    settings.setProperty("sonar.cpd.php.minimumTokens", "33");
    assertThat(engine.getMinimumTokens(javaProject, "java")).isEqualTo(42);

    Project phpProject = newProject("foo");
    settings.setProperty("sonar.cpd.java.minimumTokens", "42");
    settings.setProperty("sonar.cpd.php.minimumTokens", "33");
    assertThat(engine.getMinimumTokens(phpProject, "php")).isEqualTo(33);
  }

  private static Project newProject(String key) {
    return new Project(key).setAnalysisType(Project.AnalysisType.DYNAMIC);
  }
}
