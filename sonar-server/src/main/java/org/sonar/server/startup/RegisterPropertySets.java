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
package org.sonar.server.startup;

import com.google.common.collect.ImmutableList;
import org.sonar.api.config.PropertySetDefinitions;
import org.sonar.api.config.PropertySetTemplate;
import org.sonar.api.utils.TimeProfiler;

import java.util.List;

/**
 * @since 3.3
 */
public final class RegisterPropertySets {
  private final List<PropertySetTemplate> propertySetTemplates;
  private final PropertySetDefinitions propertySetDefinitions;

  public RegisterPropertySets(PropertySetTemplate[] propertySetTemplates, PropertySetDefinitions propertySetDefinitions) {
    this.propertySetTemplates = ImmutableList.copyOf(propertySetTemplates);
    this.propertySetDefinitions = propertySetDefinitions;
  }

  public void start() {
    TimeProfiler profiler = new TimeProfiler().start("Register dashboards");

    for (PropertySetTemplate template : propertySetTemplates) {
      propertySetDefinitions.register(template.getName(), template.createPropertySet());
    }

    profiler.stop();
  }
}
