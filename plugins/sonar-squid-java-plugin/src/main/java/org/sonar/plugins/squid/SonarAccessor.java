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
package org.sonar.plugins.squid;

import org.sonar.api.batch.SensorContext;
import org.sonar.squid.api.CodeScanner;

import java.util.Collection;
import java.util.Collections;

/**
 * TODO Godin: I didn't found better way to register component in picocontainer for Squid.
 */
public class SonarAccessor extends CodeScanner {

  private SensorContext sensorContext;

  public void setSensorContext(SensorContext sensorContext) {
    this.sensorContext = sensorContext;
  }

  public SensorContext getSensorContext() {
    return sensorContext;
  }

  @Override
  public Collection getVisitorClasses() {
    return Collections.emptyList();
  }

}
