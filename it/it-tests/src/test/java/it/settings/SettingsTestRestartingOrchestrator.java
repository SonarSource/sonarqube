/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package it.settings;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.selenium.Selenese;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import pageobjects.Navigation;
import util.selenium.SeleneseTest;

import static util.ItUtils.pluginArtifact;
import static util.ItUtils.projectDir;
import static util.ItUtils.xooPlugin;

/**
 * This class start a new orchestrator on each test case
 */
public class SettingsTestRestartingOrchestrator {

  Orchestrator orchestrator;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @After
  public void stop() {
    if (orchestrator != null) {
      orchestrator.stop();
    }
  }

  @Test
  public void test_settings() throws UnsupportedEncodingException {
    URL secretKeyUrl = getClass().getResource("/settings/SettingsTest/sonar-secret.txt");
    orchestrator = Orchestrator.builderEnv()
      .addPlugin(pluginArtifact("settings-plugin"))
      .addPlugin(pluginArtifact("license-plugin"))
      .setServerProperty("sonar.secretKeyPath", secretKeyUrl.getFile())
      .build();
    orchestrator.start();

    Navigation.get(orchestrator).openHomepage().logIn().asAdmin().openSettings(null)
      .assertMenuContains("General")
      .assertSettingDisplayed("sonar.dbcleaner.cleanDirectory")
      .assertSettingNotDisplayed("settings.extension.hidden")
      .assertSettingNotDisplayed("settings.extension.global");

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("test_settings",
      // test encryption
      "/settings/SettingsTest/generate-secret-key.html",
      "/settings/SettingsTest/encrypt-text.html"

      // test licenses
      // TODO enable when license page will be rewritten
      // "/settings/SettingsTest/ignore-corrupted-license.html",
      // "/settings/SettingsTest/display-license.html",
      // "/settings/SettingsTest/display-untyped-license.html"
    ).build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

  @Test
  public void property_relocation() throws UnsupportedEncodingException {
    orchestrator = Orchestrator.builderEnv()
      .addPlugin(pluginArtifact("property-relocation-plugin"))
      .addPlugin(xooPlugin())
      .setServerProperty("sonar.deprecatedKey", "true")
      .build();
    orchestrator.start();

    SonarScanner withDeprecatedKey = SonarScanner.create(projectDir("shared/xoo-sample"))
      .setProperty("sonar.deprecatedKey", "true");
    SonarScanner withNewKey = SonarScanner.create(projectDir("shared/xoo-sample"))
      .setProperty("sonar.newKey", "true");
    // should not fail
    orchestrator.executeBuilds(withDeprecatedKey, withNewKey);

    Navigation.get(orchestrator).openHomepage().logIn().asAdmin().openSettings(null)
      .assertMenuContains("General")
      .assertSettingDisplayed("sonar.newKey")
      .assertSettingNotDisplayed("sonar.deprecatedKey");
  }

}
