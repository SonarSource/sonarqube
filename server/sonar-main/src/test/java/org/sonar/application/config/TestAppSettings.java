/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import org.sonar.core.extension.ServiceLoaderWrapper;
import org.sonar.process.ProcessProperties;
import org.sonar.process.Props;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Simple implementation of {@link AppSettings} that loads
 * the default values defined by {@link ProcessProperties}.
 */
public class TestAppSettings implements AppSettings {

  private Props props;

  public TestAppSettings() {
    this(ImmutableMap.of());
  }

  public TestAppSettings(Map<String, String> initialSettings) {
    Properties properties = new Properties();
    properties.putAll(initialSettings);
    this.props = new Props(properties);
    ServiceLoaderWrapper serviceLoaderWrapper = mock(ServiceLoaderWrapper.class);
    when(serviceLoaderWrapper.load()).thenReturn(ImmutableSet.of());
    new ProcessProperties(serviceLoaderWrapper).completeDefaults(this.props);
  }

  @Override
  public Props getProps() {
    return props;
  }

  @Override
  public Optional<String> getValue(String key) {
    return Optional.ofNullable(props.value(key));
  }

  @Override
  public void reload(Props copy) {
    this.props = copy;
  }

  public void clearProperty(String key) {
    this.props.rawProperties().remove(key);
  }
}
