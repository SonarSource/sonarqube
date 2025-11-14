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
package org.sonar.server.setting.ws;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.property.PropertiesDao;
import org.sonar.db.property.PropertyDto;
import org.sonar.server.loginmessage.LoginMessageFeature;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.core.config.WebConstants.SONAR_LOGIN_DISPLAY_MESSAGE;
import static org.sonar.core.config.WebConstants.SONAR_LOGIN_MESSAGE;

public class LoginMessageActionTest {

  private final DbClient dbClient = mock(DbClient.class);
  private final LoginMessageFeature loginMessageFeature = mock(LoginMessageFeature.class);
  private final LoginMessageAction underTest = new LoginMessageAction(dbClient, loginMessageFeature);
  private final WsActionTester ws = new WsActionTester(underTest);
  private static final String LOGIN_MESSAGE_TEXT = "test link [SonarQube™ Home Page](https://www.sonarsource.com/products/sonarqube)\n* list 1\n* list 2";
  private static final String FORMATTED_LOGIN_MESSAGE_TEXT = "test link \\u003ca href\\u003d\\\"https://www.sonarsource.com/products/sonarqube\\\" target\\u003d\\\"_blank\\\" rel\\u003d\\\"noopener noreferrer\\\"\\u003eSonarQube\\u0026trade; Home Page\\u003c/a\\u003e\\u003cbr/\\u003e\\u003cul\\u003e\\u003cli\\u003elist 1\\u003c/li\\u003e\\n\\u003cli\\u003elist 2\\u003c/li\\u003e\\u003c/ul\\u003e";
  private static final String JSON_RESPONSE = "{\"message\":\"" + FORMATTED_LOGIN_MESSAGE_TEXT + "\"}";
  private static final String EMPTY_JSON_RESPONSE = "{\"message\":\"\"}";
  private PropertiesDao propertiesDao;

  @Before
  public void setup() {
    propertiesDao = mock(PropertiesDao.class);
    doReturn(true).when(loginMessageFeature).isAvailable();
    doReturn(propertiesDao).when(dbClient).propertiesDao();
  }

  @Test
  public void test_definition() {
    WebService.Action action = ws.getDef();

    assertThat(action.key()).isEqualTo("login_message");
    assertThat(action.isPost()).isFalse();
    assertThat(action.isInternal()).isTrue();
    assertThat(action.params()).isEmpty();
  }

  @Test
  public void returns_login_formatted_message_json() {
    mockProperty(SONAR_LOGIN_DISPLAY_MESSAGE, "true");
    mockProperty(SONAR_LOGIN_MESSAGE, LOGIN_MESSAGE_TEXT);
    TestResponse response = ws.newRequest().execute();

    assertThat(response.getInput()).isEqualTo(JSON_RESPONSE);
  }

  @Test
  public void return_empty_message_when_no_message_saved() {
    mockProperty(SONAR_LOGIN_DISPLAY_MESSAGE, "true");
    TestResponse response = ws.newRequest().execute();

    assertThat(response.getInput()).isEqualTo(EMPTY_JSON_RESPONSE);
  }

  @Test
  public void return_empty_message_when_feature_not_enabled() {
    mockProperty(SONAR_LOGIN_DISPLAY_MESSAGE, "true");
    mockProperty(SONAR_LOGIN_MESSAGE, LOGIN_MESSAGE_TEXT);
    when(loginMessageFeature.isAvailable()).thenReturn(false);
    TestResponse response = ws.newRequest().execute();

    assertThat(response.getInput()).isEqualTo(EMPTY_JSON_RESPONSE);
  }

  @Test
  public void return_empty_message_when_login_message_flag_is_disabled() {
    mockProperty(SONAR_LOGIN_DISPLAY_MESSAGE, "false");
    mockProperty(SONAR_LOGIN_MESSAGE, LOGIN_MESSAGE_TEXT);
    TestResponse response = ws.newRequest().execute();

    assertThat(response.getInput()).isEqualTo(EMPTY_JSON_RESPONSE);
  }

  private void mockProperty(String key, String value) {
    var propertyDto = mock(PropertyDto.class);
    doReturn(value).when(propertyDto).getValue();
    doReturn(key).when(propertyDto).getKey();
    doReturn(propertyDto).when(propertiesDao).selectGlobalProperty(key);
  }

}
