/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.ce.configuration;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;

public class CeWorkerCountSettingWarningTest {
  private static final String PROPERTY_SONAR_CE_WORKER_COUNT = "sonar.ce.workerCount";

  @Rule
  public LogTester logTester = new LogTester();

  private MapSettings settings = new MapSettings();
  private CeWorkerCountSettingWarning underTest = new CeWorkerCountSettingWarning(settings.asConfig());

  @Test
  public void start_does_not_log_anything_if_there_is_no_settings() {
    underTest.start();

    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  public void start_logs_a_warning_if_property_workerCount_exists_and_empty() {
    settings.setProperty(PROPERTY_SONAR_CE_WORKER_COUNT, "");

    underTest.start();

    verifyWarnMessage();
  }

  @Test
  public void start_logs_a_warning_if_property_ceWorkerCount_exists_with_a_value() {
    settings.setProperty(PROPERTY_SONAR_CE_WORKER_COUNT, randomAlphabetic(12));

    underTest.start();

    verifyWarnMessage();
  }

  private void verifyWarnMessage() {
    assertThat(logTester.logs()).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.WARN)).containsOnly("Property sonar.ce.workerCount is not supported anymore and will be ignored." +
      " Remove it from sonar.properties to remove this warning.");
  }
}
