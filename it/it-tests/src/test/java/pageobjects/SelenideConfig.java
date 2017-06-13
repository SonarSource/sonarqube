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
import com.codeborne.selenide.WebDriverRunner;
import com.sonar.orchestrator.Orchestrator;
import java.util.stream.Collectors;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import util.selenium.SeleniumDriver;
import util.selenium.ThreadSafeDriver;

import static java.util.Arrays.stream;

public class SelenideConfig {

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

  public static WebDriver configure(Orchestrator orchestrator) {
    String browserKey = orchestrator.getConfiguration().getString("orchestrator.browser", Browser.firefox.name());
    Browser browser = Browser.of(browserKey);
    Configuration.browser = browser.name();
    Configuration.baseUrl = orchestrator.getServer().getUrl();
    Configuration.timeout = 8_000;
    Configuration.reportsFolder = "target/screenshots";
    Configuration.screenshots = true;
    Configuration.captureJavascriptErrors = true;
    Configuration.savePageSource = true;
    Configuration.startMaximized = true;
    return getWebDriver();
  }

  public static WebDriver getWebDriver() {
    if (Configuration.browser.equals(Browser.firefox.name())) {
      return PER_THREAD_FIREFOX_DRIVER.get();
    }
    return WebDriverRunner.getWebDriver();
  }

  private static final ThreadLocal<SeleniumDriver> PER_THREAD_FIREFOX_DRIVER = ThreadLocal.withInitial(() -> {
    FirefoxProfile profile = new FirefoxProfile();
    profile.setPreference("browser.startup.homepage", "about:blank");
    profile.setPreference("startup.homepage_welcome_url", "about:blank");
    profile.setPreference("startup.homepage_welcome_url.additional", "about:blank");
    profile.setPreference("nglayout.initialpaint.delay", 0);
    profile.setPreference("extensions.checkCompatibility", false);
    profile.setPreference("browser.cache.use_new_backend", 1);
    profile.setPreference("geo.enabled", false);
    profile.setPreference("layout.spellcheckDefault", 0);
    FirefoxOptions options = new FirefoxOptions().setProfile(profile).setLegacy(true);
    FirefoxDriver driver = new FirefoxDriver(options);
    return ThreadSafeDriver.makeThreadSafe(driver);
  });
}
