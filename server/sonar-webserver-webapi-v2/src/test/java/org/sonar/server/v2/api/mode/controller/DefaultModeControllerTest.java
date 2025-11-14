/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.v2.api.mode.controller;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.config.Configuration;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.property.InternalPropertiesDao;
import org.sonar.db.property.PropertiesDao;
import org.sonar.db.property.PropertyDto;
import org.sonar.server.issue.notification.QualityGateMetricsUpdateNotification;
import org.sonar.server.notification.NotificationManager;
import org.sonar.server.qualitygate.QualityGateConditionsValidator;
import org.sonar.server.setting.SettingsChangeNotifier;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.v2.api.ControllerTester;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.core.config.MQRModeConstants.MULTI_QUALITY_MODE_ENABLED;
import static org.sonar.core.config.MQRModeConstants.MULTI_QUALITY_MODE_MODIFIED;
import static org.sonar.server.v2.WebApiEndpoints.JSON_MERGE_PATCH_CONTENT_TYPE;
import static org.sonar.server.v2.WebApiEndpoints.MODE_ENDPOINT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DefaultModeControllerTest {

  @RegisterExtension
  public UserSessionRule userSession = UserSessionRule.standalone();

  private final DbClient dbClient = mock(DbClient.class);
  private final Configuration configuration = mock(Configuration.class);
  private final SettingsChangeNotifier settingsChangeNotifier = mock(SettingsChangeNotifier.class);
  private final NotificationManager notificationManager = mock(NotificationManager.class);
  private final QualityGateConditionsValidator qualityGateConditionsValidator = mock(QualityGateConditionsValidator.class);
  private final PropertiesDao propertiesDao = mock(PropertiesDao.class);
  private final InternalPropertiesDao internalPropertiesDao = mock(InternalPropertiesDao.class);
  private final MockMvc mockMvc = ControllerTester
    .getMockMvc(new DefaultModeController(userSession, dbClient, configuration, settingsChangeNotifier, notificationManager, qualityGateConditionsValidator));
  private DbSession dbSession;

  @BeforeEach
  void before() {
    when(dbClient.propertiesDao()).thenReturn(propertiesDao);
    when(dbClient.internalPropertiesDao()).thenReturn(internalPropertiesDao);
    dbSession = mock(DbSession.class);
    when(dbClient.openSession(false)).thenReturn(dbSession);
  }

  @Test
  void getMode_whenMQRMode_shouldReturnExpectedMode() throws Exception {
    when(internalPropertiesDao.selectByKey(dbSession, MULTI_QUALITY_MODE_MODIFIED)).thenReturn(Optional.empty());
    when(configuration.getBoolean(MULTI_QUALITY_MODE_ENABLED)).thenReturn(Optional.of(true));
    mockMvc.perform(get(MODE_ENDPOINT))
      .andExpectAll(
        status().isOk(),
        content().json("""
          {"mode":"MQR", "modified": false}"""));
  }

  @Test
  void getMode_whenMQRModeAndHasBeenModified_shouldReturnExpectedMode() throws Exception {
    when(configuration.getBoolean(MULTI_QUALITY_MODE_ENABLED)).thenReturn(Optional.of(true));
    when(internalPropertiesDao.selectByKey(dbSession, MULTI_QUALITY_MODE_MODIFIED)).thenReturn(Optional.of("true"));
    mockMvc.perform(get(MODE_ENDPOINT + "/"))
      .andExpectAll(
        status().isOk(),
        content().json("""
          {"mode":"MQR", "modified": true}"""));
  }

  @Test
  void getMode_whenMissing_shouldReturnMQRMode() throws Exception {
    when(configuration.getBoolean(MULTI_QUALITY_MODE_ENABLED)).thenReturn(Optional.empty());
    mockMvc.perform(get(MODE_ENDPOINT))
      .andExpectAll(
        status().isOk(),
        content().json("""
          {"mode":"MQR"}"""));
  }

  @Test
  void getMode_whenStandard_shouldReturnExpectedMode() throws Exception {
    when(configuration.getBoolean(MULTI_QUALITY_MODE_ENABLED)).thenReturn(Optional.of(false));
    mockMvc.perform(get(MODE_ENDPOINT))
      .andExpectAll(
        status().isOk(),
        content().json("""
          {"mode":"STANDARD_EXPERIENCE"}"""));
  }

  @Test
  void patchMode_whenInsufficientPermissions_shouldThrowException() throws Exception {
    mockMvc.perform(patch(MODE_ENDPOINT)
      .contentType(JSON_MERGE_PATCH_CONTENT_TYPE)
      .content("""
        {"mode":"STANDARD_EXPERIENCE"}"""))
      .andExpectAll(status().isForbidden());
  }

  @Test
  void patchMode_whenNotChanged_shouldNotUpdate() throws Exception {
    userSession.logIn().setSystemAdministrator();
    when(configuration.getBoolean(MULTI_QUALITY_MODE_ENABLED)).thenReturn(Optional.of(false));
    mockMvc.perform(patch(MODE_ENDPOINT)
      .contentType(JSON_MERGE_PATCH_CONTENT_TYPE)
      .content("""
        {"mode":"STANDARD_EXPERIENCE"}"""))
      .andExpectAll(
        status().isOk(),
        content().json("""
          {"mode":"STANDARD_EXPERIENCE"}"""));

    verifyNoInteractions(propertiesDao);
    verifyNoInteractions(settingsChangeNotifier);
    verifyNoInteractions(notificationManager);
  }

  @Test
  void patchMode_whenModeHasChanged_shouldDoUpdate() throws Exception {
    userSession.logIn().setSystemAdministrator();
    when(configuration.getBoolean(MULTI_QUALITY_MODE_ENABLED)).thenReturn(Optional.of(false));
    mockMvc.perform(patch(MODE_ENDPOINT)
      .contentType(JSON_MERGE_PATCH_CONTENT_TYPE)
      .content("""
        {"mode":"MQR"}"""))
      .andExpectAll(
        status().isOk(),
        content().json("""
          {"mode":"MQR"}"""));

    verify(propertiesDao, times(1)).saveProperty(dbSession, new PropertyDto().setKey(MULTI_QUALITY_MODE_ENABLED).setValue("true"));
    verify(internalPropertiesDao, times(1)).save(dbSession, MULTI_QUALITY_MODE_MODIFIED, "true");
    verify(settingsChangeNotifier, times(1)).onGlobalPropertyChange(MULTI_QUALITY_MODE_ENABLED, "true");
    verifyNoInteractions(notificationManager);
  }

  @Test
  void patchMode_whenModeHasChangedAndConditionsMismatch_shouldSendNotification() throws Exception {
    userSession.logIn().setSystemAdministrator();
    when(qualityGateConditionsValidator.hasConditionsMismatch(true)).thenReturn(true);
    when(configuration.getBoolean(MULTI_QUALITY_MODE_ENABLED)).thenReturn(Optional.of(false));
    mockMvc.perform(patch(MODE_ENDPOINT)
      .contentType(JSON_MERGE_PATCH_CONTENT_TYPE)
      .content("""
        {"mode":"MQR"}"""))
      .andExpectAll(
        status().isOk(),
        content().json("""
          {"mode":"MQR"}"""));

    verify(notificationManager, times(1)).scheduleForSending(new QualityGateMetricsUpdateNotification(true));
  }
}
