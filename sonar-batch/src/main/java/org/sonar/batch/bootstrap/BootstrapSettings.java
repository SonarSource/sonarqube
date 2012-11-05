/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.bootstrap;

import org.apache.commons.configuration.Configuration;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;

import javax.annotation.Nullable;

/**
 * @since 2.12
 */
public class BootstrapSettings extends Settings {
  private Configuration deprecatedConfiguration;
  private ProjectReactor reactor;

  public BootstrapSettings(PropertyDefinitions propertyDefinitions, ProjectReactor reactor, Configuration deprecatedConfiguration) {
    super(propertyDefinitions);
    this.reactor = reactor;
    this.deprecatedConfiguration = deprecatedConfiguration;
    init();
  }

  private void init() {
    // order is important -> bottom-up. The last one overrides all the others.
    addProperties(reactor.getRoot().getProperties());
    addEnvironmentVariables();
    addSystemProperties();
  }

  @Override
  protected void doOnSetProperty(String key, @Nullable String value) {
    deprecatedConfiguration.setProperty(key, value);
  }

  @Override
  protected void doOnRemoveProperty(String key) {
    deprecatedConfiguration.clearProperty(key);
  }

  @Override
  protected void doOnClearProperties() {
    deprecatedConfiguration.clear();
  }
}
