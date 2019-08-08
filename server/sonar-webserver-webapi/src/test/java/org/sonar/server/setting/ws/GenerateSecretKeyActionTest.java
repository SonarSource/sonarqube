/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.config.Encryption;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Settings.GenerateSecretKeyWsResponse;

import static org.assertj.core.api.Assertions.assertThat;

public class GenerateSecretKeyActionTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone().logIn().setSystemAdministrator();
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private MapSettings settings = new MapSettings();
  private Encryption encryption = settings.getEncryption();
  private GenerateSecretKeyAction underTest = new GenerateSecretKeyAction(settings, userSession);
  private WsActionTester ws = new WsActionTester(underTest);

  @Test
  public void generate_valid_secret_key() throws IOException {
    GenerateSecretKeyWsResponse result = call();

    String secretKey = result.getSecretKey();
    File file = temporaryFolder.newFile();
    FileUtils.writeStringToFile(file, secretKey);
    encryption.setPathToSecretKey(file.getAbsolutePath());
    String encryptedValue = encryption.encrypt("my value");
    String decryptedValue = encryption.decrypt(encryptedValue);
    assertThat(decryptedValue).isEqualTo("my value");
  }

  @Test
  public void definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.key()).isEqualTo("generate_secret_key");
    assertThat(definition.isPost()).isFalse();
    assertThat(definition.isInternal()).isTrue();
    assertThat(definition.responseExampleAsString()).isNotEmpty();
    assertThat(definition.params()).hasSize(0);
  }

  @Test
  public void throw_ForbiddenException_if_not_system_administrator() {
    userSession.logIn().setNonSystemAdministrator();

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    call();
  }


  private GenerateSecretKeyWsResponse call() {
    return ws.newRequest()
      .setMethod("GET")
      .executeProtobuf(GenerateSecretKeyWsResponse.class);
  }

}
