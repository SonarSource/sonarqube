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
package org.sonar.server.setting.ws;

import java.io.File;
import java.nio.charset.StandardCharsets;
import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.config.internal.Encryption;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Settings.EncryptWsResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.server.setting.ws.SettingsWsParameters.PARAM_VALUE;

public class EncryptActionIT {
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  private MapSettings settings = new MapSettings();
  private Encryption encryption = settings.getEncryption();
  private EncryptAction underTest = new EncryptAction(userSession, settings);
  private WsActionTester ws = new WsActionTester(underTest);

  @Before
  public void setUpSecretKey() throws Exception {
    logInAsSystemAdministrator();

    File secretKeyFile = folder.newFile();
    FileUtils.writeStringToFile(secretKeyFile, "fCVFf/JHRi8Qwu5KLNva7g==", StandardCharsets.UTF_8);

    encryption.setPathToSecretKey(secretKeyFile.getAbsolutePath());
  }

  @Test
  public void encrypt() {
    logInAsSystemAdministrator();

    EncryptWsResponse result = call("my value!");

    assertThat(result.getEncryptedValue()).matches("^\\{aes-gcm\\}.+");
  }

  @Test
  public void definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.key()).isEqualTo("encrypt");
    assertThat(definition.isPost()).isTrue();
    assertThat(definition.isInternal()).isTrue();
    assertThat(definition.responseExampleAsString()).isNotEmpty();
    assertThat(definition.params()).hasSize(1);
  }

  @Test
  public void throw_ForbiddenException_if_not_system_administrator() {
    userSession.logIn().setNonSystemAdministrator();

    assertThatThrownBy(() -> call("my value"))
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void fail_if_value_is_not_provided() {
    logInAsSystemAdministrator();

    assertThatThrownBy(() -> call(null))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void fail_if_value_is_empty() {
    logInAsSystemAdministrator();

    assertThatThrownBy(() ->  call("  "))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The 'value' parameter is missing");
  }

  @Test
  public void fail_if_no_secret_key_available() {
    logInAsSystemAdministrator();

    encryption.setPathToSecretKey("unknown/path/to/secret/key");

    assertThatThrownBy(() ->  call("my value"))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("No secret key available");
  }

  private EncryptWsResponse call(@Nullable String value) {
    TestRequest request = ws.newRequest()
      .setMethod("POST");

    if (value != null) {
      request.setParam(PARAM_VALUE, value);
    }

    return request.executeProtobuf(EncryptWsResponse.class);
  }

  private void logInAsSystemAdministrator() {
    userSession.logIn().setSystemAdministrator();
  }
}
