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
package org.sonarqube.qa.util;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.WebDriverRunner;
import com.sonar.orchestrator.Orchestrator;
import java.util.Locale;
import java.util.stream.Collectors;
import org.openqa.selenium.WebDriver;

import static java.util.Arrays.stream;

public class SelenideConfig {

  private enum Browser {
    FIREFOX("(v46 and lower)"),
    MARIONETTE("(recent Firefox, require Geckodriver)"),
    CHROME("(require Chromedriver)");

    private final String label;

    Browser(String label) {
      this.label = label;
    }

    static Browser of(String s) {
      try {
        return Browser.valueOf(s.toUpperCase(Locale.US));
      } catch (Exception e) {
        throw new IllegalArgumentException("Invalid browser: " + s + ". Supported values are " +
          stream(values()).map(b -> b.name() + " " + b.label).collect(Collectors.joining(", ")));
      }
    }
  }

  public static WebDriver configure(Orchestrator orchestrator) {
    String browserKey = orchestrator.getConfiguration().getString("orchestrator.browser", Browser.FIREFOX.name());
    Browser browser = Browser.of(browserKey);
    Configuration.browser = browser.name();
    Configuration.baseUrl = orchestrator.getServer().getUrl();
    Configuration.timeout = 8_000;
    Configuration.reportsFolder = "target/screenshots";
    Configuration.screenshots = true;
    Configuration.captureJavascriptErrors = true;
    Configuration.savePageSource = true;
    Configuration.browserSize = "1280x1024";
    return getWebDriver();
  }

  static WebDriver getWebDriver() {
    return WebDriverRunner.getWebDriver();
  }
}
