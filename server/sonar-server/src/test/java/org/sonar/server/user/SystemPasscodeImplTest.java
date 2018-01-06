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
package org.sonar.server.user;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.server.ws.TestRequest;

import static org.assertj.core.api.Assertions.assertThat;

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
  public void isConfigured_is_true_if_property_is_not_blank() {
    verifyIsConfigured("foo", true);
  }

  @Test
  public void isConfigured_is_false_if_property_value_is_blank() {
    verifyIsConfigured("  ", false);
  }

  @Test
  public void isConfigured_is_false_if_property_value_is_empty() {
    verifyIsConfigured("", false);
  }

  @Test
  public void isConfigured_is_false_if_property_is_not_defined() {
    assertThat(underTest.isConfigured()).isFalse();
  }

  @Test
  public void startup_logs_show_that_feature_is_enabled() {
    configurePasscode("foo");
    underTest.start();

    assertThat(logTester.logs(LoggerLevel.INFO)).contains("System authentication by passcode is enabled");
  }

  @Test
  public void startup_logs_show_that_feature_is_disabled() {
    underTest.start();

    assertThat(logTester.logs(LoggerLevel.INFO)).contains("System authentication by passcode is disabled");
  }

  @Test
  public void isValid_is_true_if_request_header_matches_configured_passcode() {
    verifyIsValid(true, "foo", "foo");
  }

  @Test
  public void isValid_is_false_if_request_header_matches_configured_passcode_with_different_case() {
    verifyIsValid(false, "foo", "FOO");
  }

  @Test
  public void isValid_is_false_if_request_header_does_not_match_configured_passcode() {
    verifyIsValid(false, "foo", "bar");
  }

  @Test
  public void isValid_is_false_if_request_header_is_defined_but_passcode_is_not_configured() {
    verifyIsValid(false, null, "foo");
  }

  @Test
  public void isValid_is_false_if_request_header_is_empty() {
    verifyIsValid(false, "foo", "");
  }

  private void verifyIsValid(boolean expectedResult, String configuredPasscode, String header) {
    configurePasscode(configuredPasscode);

    TestRequest request = new TestRequest();
    request.setHeader("X-Sonar-Passcode", header);

    assertThat(underTest.isValid(request)).isEqualTo(expectedResult);
  }

  private void verifyIsConfigured(String propertyValue, boolean expectedResult) {
    configurePasscode(propertyValue);
    assertThat(underTest.isConfigured()).isEqualTo(expectedResult);
  }

  private void configurePasscode(String propertyValue) {
    settings.setProperty("sonar.web.systemPasscode", propertyValue);
    underTest.start();
  }
}
