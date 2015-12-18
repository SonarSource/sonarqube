/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package util.selenium;

import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;

public enum Browser {
  FIREFOX;

  private final ThreadLocal<SeleniumDriver> perThreadDriver = new ThreadLocal<SeleniumDriver>() {
    @Override
    protected SeleniumDriver initialValue() {
      FirefoxProfile profile = new FirefoxProfile();
      profile.setPreference("browser.startup.homepage", "about:blank");
      profile.setPreference("startup.homepage_welcome_url", "about:blank");
      profile.setPreference("startup.homepage_welcome_url.additional", "about:blank");
      profile.setPreference("nglayout.initialpaint.delay", 0);
      profile.setPreference("extensions.checkCompatibility", false);
      profile.setPreference("browser.cache.use_new_backend", 1);
      profile.setPreference("geo.enabled", false);
      profile.setPreference("layout.spellcheckDefault", 0);
      return ThreadSafeDriver.makeThreadSafe(new FirefoxDriver(profile));
    }
  };

  public SeleniumDriver getDriverForThread() {
    return perThreadDriver.get();
  }
}
