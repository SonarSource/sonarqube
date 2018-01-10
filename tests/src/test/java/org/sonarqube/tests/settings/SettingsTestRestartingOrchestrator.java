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
package org.sonarqube.tests.settings;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonarqube.qa.util.pageobjects.EncryptionPage;
import org.sonarqube.qa.util.pageobjects.Navigation;
import util.user.UserRule;

import static com.codeborne.selenide.Condition.visible;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.pluginArtifact;
import static util.ItUtils.projectDir;
import static util.ItUtils.xooPlugin;

/**
 * This class start a new orchestrator on each test case
 */
public class SettingsTestRestartingOrchestrator {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private Orchestrator orchestrator;

  private UserRule userRule;

  @After
  public void stop() {
    if (orchestrator != null) {
      userRule.resetUsers();
      orchestrator.stop();
    }
  }

  @Test
  public void test_settings() {
    URL secretKeyUrl = getClass().getResource("/settings/SettingsTest/sonar-secret.txt");
    orchestrator = Orchestrator.builderEnv()
      .addPlugin(pluginArtifact("settings-plugin"))
      .addPlugin(pluginArtifact("license-plugin"))
      .setServerProperty("sonar.secretKeyPath", secretKeyUrl.getFile())
      .build();
    startOrchestrator();

    String adminUser = userRule.createAdminUser();
    Navigation nav = Navigation.create(orchestrator).openHome().logIn().submitCredentials(adminUser);

    nav.openSettings(null)
      .assertMenuContains("General")
      .assertSettingDisplayed("sonar.dbcleaner.cleanDirectory")
      .assertSettingNotDisplayed("settings.extension.hidden")
      .assertSettingNotDisplayed("settings.extension.global");

    EncryptionPage encryptionPage = nav.openEncryption();
    assertThat(encryptionPage.encryptValue("clear")).isEqualTo("{aes}4aQbfYe1lrEjiRzv/ETbyg==");
    encryptionPage.generateNewKey();
    encryptionPage.generationForm().shouldBe(visible).submit();
    encryptionPage.generationForm().shouldNotBe(visible);
    encryptionPage.newSecretKey().shouldBe(visible);
  }

  @Test
  public void property_relocation() {
    orchestrator = Orchestrator.builderEnv()
      .addPlugin(pluginArtifact("property-relocation-plugin"))
      .addPlugin(xooPlugin())
      .setServerProperty("sonar.deprecatedKey", "true")
      .build();
    startOrchestrator();

    SonarScanner withDeprecatedKey = SonarScanner.create(projectDir("shared/xoo-sample"))
      .setProperty("sonar.deprecatedKey", "true");
    SonarScanner withNewKey = SonarScanner.create(projectDir("shared/xoo-sample"))
      .setProperty("sonar.newKey", "true");
    // should not fail
    orchestrator.executeBuilds(withDeprecatedKey, withNewKey);

    String adminUser = userRule.createAdminUser();
    Navigation.create(orchestrator).openHome().logIn().submitCredentials(adminUser).openSettings(null)
      .assertMenuContains("General")
      .assertSettingDisplayed("sonar.newKey")
      .assertSettingNotDisplayed("sonar.deprecatedKey");
  }

  private void startOrchestrator() {
    orchestrator.start();
    userRule = UserRule.from(orchestrator);
  }

}
