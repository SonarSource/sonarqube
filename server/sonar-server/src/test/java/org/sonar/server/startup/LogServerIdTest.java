/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.startup;

import javax.annotation.Nullable;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.property.PropertyDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LogServerIdTest {

  @Rule
  public LogTester logTester = new LogTester();

  DbClient dbClient = mock(DbClient.class, RETURNS_DEEP_STUBS);
  LogServerId underTest = new LogServerId(dbClient);

  @After
  public void tearDown() {
    underTest.stop();
  }

  @Test
  public void log_all_information_at_startup() {
    setProperty(CoreProperties.PERMANENT_SERVER_ID, "123456789");
    setProperty(CoreProperties.ORGANISATION, "SonarSource");
    setProperty(CoreProperties.SERVER_ID_IP_ADDRESS, "1.2.3.4");

    underTest.start();

    verifyLog("\"123456789\"", "\"SonarSource\"", "\"1.2.3.4\"");
  }

  @Test
  public void do_not_log_if_server_id_is_absent() {
    setProperty(CoreProperties.PERMANENT_SERVER_ID, null);

    underTest.start();

    assertThat(logTester.logs(LoggerLevel.INFO)).isEmpty();
  }

  @Test
  public void log_partial_information_if_organisation_is_missing() {
    setProperty(CoreProperties.PERMANENT_SERVER_ID, "123456789");
    setProperty(CoreProperties.SERVER_ID_IP_ADDRESS, "1.2.3.4");

    underTest.start();

    verifyLog("\"123456789\"", "-", "\"1.2.3.4\"");
  }

  @Test
  public void log_partial_information_if_ip_is_missing() {
    setProperty(CoreProperties.PERMANENT_SERVER_ID, "123456789");
    setProperty(CoreProperties.ORGANISATION, "SonarSource");

    underTest.start();

    verifyLog("\"123456789\"", "\"SonarSource\"", "-");
  }

  @Test
  public void log_partial_information_if_ip_and_organisation_are_missing() {
    setProperty(CoreProperties.PERMANENT_SERVER_ID, "123456789");

    underTest.start();

    verifyLog("\"123456789\"", "-", "-");
  }

  @Test
  public void log_partial_information_if_property_is_set_without_value() {
    setProperty(CoreProperties.PERMANENT_SERVER_ID, "123456789");
    PropertyDto dto = new PropertyDto().setKey(CoreProperties.ORGANISATION).setValue(null);
    when(dbClient.propertiesDao().selectGlobalProperty(any(DbSession.class), eq(CoreProperties.ORGANISATION))).thenReturn(dto);

    underTest.start();

    verifyLog("\"123456789\"", "-", "-");
  }

  private void setProperty(String propertyKey, @Nullable String propertyValue) {
    PropertyDto dto = null;
    if (propertyValue != null) {
      dto = new PropertyDto().setKey(propertyKey).setValue(propertyValue);
    }
    when(dbClient.propertiesDao().selectGlobalProperty(any(DbSession.class), eq(propertyKey))).thenReturn(dto);
  }

  private void verifyLog(String expectedId, String expectedOrganisation, String expectedIp) {
    assertThat(logTester.logs(LoggerLevel.INFO)).contains("Server information:\n"
      + "  - ID           : " + expectedId + "\n"
      + "  - Organization : " + expectedOrganisation + "\n"
      + "  - Registered IP: " + expectedIp + "\n");
  }

}
