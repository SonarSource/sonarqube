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
import java.net.URL;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
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
  public void test_settings() {
    URL secretKeyUrl = getClass().getResource("/settings/SettingsTest/sonar-secret.txt");
    orchestrator = Orchestrator.builderEnv()
      .addPlugin(pluginArtifact("settings-plugin"))
      .addPlugin(pluginArtifact("license-plugin"))
      .setServerProperty("sonar.secretKeyPath", secretKeyUrl.getFile())
      .build();
    orchestrator.start();

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("test_settings",
      "/settings/SettingsTest/general-settings.html",

      // SONAR-2869 the annotation @Properties can be used on extensions and not only on plugin entry points
      "/settings/SettingsTest/hidden-extension-property.html",
      "/settings/SettingsTest/global-extension-property.html",

      // SONAR-3344 - licenses
      "/settings/SettingsTest/ignore-corrupted-license.html",
      "/settings/SettingsTest/display-license.html",
      "/settings/SettingsTest/display-untyped-license.html",

      // SONAR-2084 - encryption
      "/settings/SettingsTest/generate-secret-key.html",
      "/settings/SettingsTest/encrypt-text.html",

      // SONAR-1378 - property types
      "/settings/SettingsTest/validate-property-type.html",

      // SONAR-3127 - hide passwords
      "/settings/SettingsTest/hide-passwords.html"
      ).build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

  @Test
  public void property_relocation() {
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

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("property_relocation",
      "/settings/SettingsTest/property_relocation.html"
      ).build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

}
