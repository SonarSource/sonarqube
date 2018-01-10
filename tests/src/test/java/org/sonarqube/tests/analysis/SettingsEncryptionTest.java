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
package org.sonarqube.tests.analysis;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildFailureException;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarScanner;
import org.sonarqube.tests.Category3Suite;
import java.io.File;
import java.net.URL;
import org.junit.ClassRule;
import org.junit.Test;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class SettingsEncryptionTest {

  @ClassRule
  public static Orchestrator orchestrator = Category3Suite.ORCHESTRATOR;

  /**
   * SONAR-2084
   * SONAR-4061
   */
  @Test
  public void testEncryptedProperty() throws Exception {
    SonarScanner build = SonarScanner.create(ItUtils.projectDir("shared/xoo-sample"))
      .setProperty("sonar.secretKeyPath", pathToValidSecretKey())
      .setProperty("sonar.login", "admin")
      // wrong password
      .setProperty("sonar.password", "{aes}wrongencryption==")// wrong password
      // "this is a secret" encrypted with the above secret key
      .setProperty("encryptedProperty", "{aes}9mx5Zq4JVyjeChTcVjEide4kWCwusFl7P2dSVXtg9IY=");
    BuildResult result = orchestrator.executeBuildQuietly(build);
    assertThat(result.getStatus()).isNotEqualTo(0);
    assertThat(result.getLogs()).contains("Fail to decrypt the property sonar.password. Please check your secret key");

    build = SonarScanner.create(ItUtils.projectDir("shared/xoo-sample"))
      .setProperty("sonar.secretKeyPath", pathToValidSecretKey())
      // "admin" encrypted with the above secret key
      .setProperty("sonar.login", "{aes}evRHXHsEyPr5RjEuxUJcHA==")
      .setProperty("sonar.password", "{aes}evRHXHsEyPr5RjEuxUJcHA==")
      // "this is a secret" encrypted with the above secret key
      .setProperty("encryptedProperty", "{aes}9mx5Zq4JVyjeChTcVjEide4kWCwusFl7P2dSVXtg9IY=");
    // no error
    orchestrator.executeBuild(build);
  }

  /**
   * SONAR-2084
   */
  @Test(expected = BuildFailureException.class)
  public void failIfEncryptedPropertyButNoSecretKey() {
    // path to secret key is missing
    SonarScanner build = SonarScanner.create(ItUtils.projectDir("shared/xoo-sample"))
      .setProperty("encryptedProperty", "{aes}9mx5Zq4JVyjeChTcVjEide4kWCwusFl7P2dSVXtg9IY=");
    orchestrator.executeBuild(build);
  }

  private String pathToValidSecretKey() throws Exception {
    URL resource = getClass().getResource("/analysis/SettingsEncryptionTest/sonar-secret.txt");
    return new File(resource.toURI()).getCanonicalPath();
  }
}
