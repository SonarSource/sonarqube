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

package org.sonar.application.config;

import java.util.Optional;
import java.util.Properties;
import org.sonar.process.ProcessProperties;
import org.sonar.process.Props;

/**
 * Simple implementation of {@link AppSettings} that loads
 * the default values defined by {@link ProcessProperties}.
 */
public class TestAppSettings implements AppSettings {

  private Props properties;

  public TestAppSettings() {
    this.properties = new Props(new Properties());
    ProcessProperties.completeDefaults(this.properties);
  }

  public TestAppSettings set(String key, String value) {
    this.properties.set(key, value);
    return this;
  }

  @Override
  public Props getProps() {
    return properties;
  }

  @Override
  public Optional<String> getValue(String key) {
    return Optional.ofNullable(properties.value(key));
  }

  @Override
  public void reload(Props copy) {
    this.properties = copy;
  }
}
