/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package batch;

import util.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildFailureException;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarRunner;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

public class SettingsEncryptionTest {
  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
    .addPlugin(ItUtils.pluginArtifact("settings-encryption-plugin"))
    .addPlugin(ItUtils.xooPlugin())
    .build();

  /**
   * SONAR-2084
   * SONAR-4061
   */
  @Test
  public void testEncryptedProperty() throws Exception {
    SonarRunner build = SonarRunner.create(ItUtils.projectDir("shared/xoo-sample"))
      .setProperty("sonar.secretKeyPath", pathToValidSecretKey())
      .setProperty("sonar.login", "admin")
      // wrong password
      .setProperty("sonar.password", "{aes}wrongencryption==")// wrong password
      // "this is a secret" encrypted with the above secret key
      .setProperty("encryptedProperty", "{aes}9mx5Zq4JVyjeChTcVjEide4kWCwusFl7P2dSVXtg9IY=");
    BuildResult result = orchestrator.executeBuildQuietly(build);
    assertThat(result.getStatus()).isNotEqualTo(0);
    assertThat(result.getLogs()).contains("Fail to decrypt the property sonar.password. Please check your secret key");

    build = SonarRunner.create(ItUtils.projectDir("shared/xoo-sample"))
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
  public void failIfEncryptedPropertyButNoSecretKey() throws Exception {
    // path to secret key is missing
    SonarRunner build = SonarRunner.create(ItUtils.projectDir("shared/xoo-sample"))
      .setProperty("encryptedProperty", "{aes}9mx5Zq4JVyjeChTcVjEide4kWCwusFl7P2dSVXtg9IY=");
    orchestrator.executeBuild(build);
  }

  private String pathToValidSecretKey() throws Exception {
    URL resource = getClass().getResource("/batch/SettingsEncryptionTest/sonar-secret.txt");
    return new File(resource.toURI()).getCanonicalPath();
  }
}
