/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package pageobjects;

import com.codeborne.selenide.Configuration;
import com.sonar.orchestrator.Orchestrator;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

class SelenideConfig {

  private enum Browser {
    firefox("(v46 and lower)"),
    marionette("(recent Firefox)"),
    chrome("(require Chromedriver)"),
    phantomjs("(headless)");

    private final String label;

    Browser(String label) {
      this.label = label;
    }

    static Browser of(String s) {
      try {
        return Browser.valueOf(s);
      } catch (Exception e) {
        throw new IllegalArgumentException("Invalid browser: " + s + ". Supported values are " +
          stream(values()).map(b -> b.name() + " " + b.label).collect(Collectors.joining(", ")));
      }
    }
  }

  public static void configure(Orchestrator orchestrator) {
    String browserKey = orchestrator.getConfiguration().getString("orchestrator.browser", Browser.firefox.name());
    Browser browser = Browser.of(browserKey);
    Configuration.browser = browser.name();
    Configuration.baseUrl = orchestrator.getServer().getUrl();
    Configuration.timeout = 8_000;
    Configuration.reportsFolder = "target/screenshots";
    Configuration.screenshots = true;
    Configuration.captureJavascriptErrors = true;
    Configuration.savePageSource = true;
  }

}
