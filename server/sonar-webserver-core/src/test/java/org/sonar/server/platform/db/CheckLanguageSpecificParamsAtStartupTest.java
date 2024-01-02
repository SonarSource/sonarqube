/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.platform.db;

import org.junit.After;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.event.Level;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.CoreProperties.LANGUAGE_SPECIFIC_PARAMETERS;
import static org.sonar.api.CoreProperties.LANGUAGE_SPECIFIC_PARAMETERS_LANGUAGE_KEY;
import static org.sonar.api.CoreProperties.LANGUAGE_SPECIFIC_PARAMETERS_MAN_DAYS_KEY;

public class CheckLanguageSpecificParamsAtStartupTest {

  @ClassRule
  public static LogTester logTester = new LogTester().setLevel(LoggerLevel.WARN);
  @Rule
  public final DbTester dbTester = DbTester.create(System2.INSTANCE);
  private final MapSettings settings = new MapSettings();
  private final CheckLanguageSpecificParamsAtStartup underTest = new CheckLanguageSpecificParamsAtStartup(settings.asConfig());

  @After
  public void tearDown() {
    logTester.clear();
    underTest.stop();
  }

  @Test
  public void log_shows_when_language_specific_params_used() {
    String aLanguage = "aLanguage";
    String anotherLanguage = "anotherLanguage";
    settings.setProperty(LANGUAGE_SPECIFIC_PARAMETERS, "0,1");
    settings.setProperty(LANGUAGE_SPECIFIC_PARAMETERS + "." + "0" + "." + LANGUAGE_SPECIFIC_PARAMETERS_LANGUAGE_KEY, aLanguage);
    settings.setProperty(LANGUAGE_SPECIFIC_PARAMETERS + "." + "0" + "." + LANGUAGE_SPECIFIC_PARAMETERS_MAN_DAYS_KEY, "30");
    settings.setProperty(LANGUAGE_SPECIFIC_PARAMETERS + "." + "0" + "." + CoreProperties.LANGUAGE_SPECIFIC_PARAMETERS_SIZE_METRIC_KEY, CoreMetrics.NCLOC_KEY);
    settings.setProperty(LANGUAGE_SPECIFIC_PARAMETERS + "." + "1" + "." + LANGUAGE_SPECIFIC_PARAMETERS_LANGUAGE_KEY, anotherLanguage);
    settings.setProperty(LANGUAGE_SPECIFIC_PARAMETERS + "." + "1" + "." + LANGUAGE_SPECIFIC_PARAMETERS_MAN_DAYS_KEY, "40");
    settings.setProperty(LANGUAGE_SPECIFIC_PARAMETERS + "." + "1" + "." + CoreProperties.LANGUAGE_SPECIFIC_PARAMETERS_SIZE_METRIC_KEY, CoreMetrics.COMPLEXITY_KEY);

    underTest.start();
    assertThat(logTester.logs(Level.WARN))
      .contains("The development cost used for calculating the technical debt is currently configured with 2 language specific parameters [Key: languageSpecificParameters]. " +
      "Please be aware that this functionality is deprecated, and will be removed in a future version.");
  }

  @Test
  public void log_does_not_show_when_language_specific_params_used() {
    underTest.start();
    boolean noneMatch = logTester.logs(Level.WARN).stream()
      .noneMatch(s -> s.startsWith("The development cost used for calculating the technical debt is currently configured with"));
    assertThat(noneMatch).isTrue();
  }

}
