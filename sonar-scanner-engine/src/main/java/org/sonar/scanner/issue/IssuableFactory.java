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
package org.sonar.scanner.issue;

import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.issue.Issuable;
import org.sonar.scanner.deprecated.perspectives.PerspectiveBuilder;
import org.sonar.scanner.sensor.DefaultSensorContext;

/**
 * Create the perspective {@link Issuable} on components.
 * @since 3.6
 */
public class IssuableFactory extends PerspectiveBuilder<Issuable> {

  private final SensorContext sensorContext;

  public IssuableFactory(DefaultSensorContext sensorContext) {
    super(Issuable.class);
    this.sensorContext = sensorContext;
  }

  @Override
  public Issuable loadPerspective(Class<Issuable> perspectiveClass, InputComponent component) {
    return new DefaultIssuable(component, sensorContext);
  }
}
