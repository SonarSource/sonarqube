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
package org.sonar.server.user;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.event.Level;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.impl.ws.SimpleGetRequest;
import org.sonar.api.testfixtures.log.LogTester;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class SystemPasscodeImplTest {

  @Rule
  public LogTester logTester = new LogTester();

  private MapSettings settings = new MapSettings();
  private SystemPasscodeImpl underTest = new SystemPasscodeImpl(settings.asConfig());

  @After
  public void tearDown() {
    underTest.stop();
  }

  @Test
  public void startup_logs_show_that_feature_is_enabled() {
    configurePasscode("foo");
    underTest.start();

    assertThat(logTester.logs(Level.INFO)).contains("System authentication by passcode is enabled");
  }

  @Test
  public void startup_logs_show_that_feature_is_disabled() {
    underTest.start();

    assertThat(logTester.logs(Level.INFO)).contains("System authentication by passcode is disabled");
  }

  @Test
  public void passcode_is_disabled_if_blank_configuration() {
    configurePasscode("");
    underTest.start();

    assertThat(logTester.logs(Level.INFO)).contains("System authentication by passcode is disabled");
  }

  @DataProvider
  public static Object[][] passcodeConfigurationAndUserInput() {
    return new Object[][] {
      {"toto", "toto", true},
      {"toto", "tata", false},
      {"toto", "Toto", false},
      {"toto", "toTo", false},
      {null, null, false},
      {null, "toto", false},
      {"toto", null, false},
    };
  }


  @Test
  @UseDataProvider("passcodeConfigurationAndUserInput")
  public void isValidPasscode_worksCorrectly(String configuredPasscode, String userPasscode, boolean expectedResult) {
    configurePasscode(configuredPasscode);
    assertThat(underTest.isValidPasscode(userPasscode)).isEqualTo(expectedResult);
  }

  @Test
  @UseDataProvider("passcodeConfigurationAndUserInput")
  public void isValid_worksCorrectly(String configuredPasscode, String userPasscode, boolean expectedResult) {
    configurePasscode(configuredPasscode);

    SimpleGetRequest request = new SimpleGetRequest();
    request.setHeader("X-Sonar-Passcode", userPasscode);

    assertThat(underTest.isValid(request)).isEqualTo(expectedResult);
  }

  private void configurePasscode(String propertyValue) {
    settings.setProperty("sonar.web.systemPasscode", propertyValue);
    underTest.start();
  }
}
