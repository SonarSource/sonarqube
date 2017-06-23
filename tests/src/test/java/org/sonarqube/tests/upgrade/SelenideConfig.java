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
package org.sonarqube.tests.upgrade;

import com.codeborne.selenide.Configuration;
import com.google.common.collect.ImmutableSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

enum SelenideConfig {
  INSTANCE;

  private static final Set<String> SUPPORTED_BROWSERS = ImmutableSet.of("firefox");

  SelenideConfig() {
    Configuration.reportsFolder = "target/screenshots";
  }

  public SelenideConfig setBrowser(String browser) {
    checkArgument(SUPPORTED_BROWSERS.contains(requireNonNull(browser)), "Browser is not supported: %s", browser);
    Configuration.browser = browser;
    return this;
  }

  public SelenideConfig setBaseUrl(String s) {
    Configuration.baseUrl = requireNonNull(s);
    return this;
  }

}
