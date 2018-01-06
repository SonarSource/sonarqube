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
package org.sonar.scanner.source;

import javax.annotation.CheckForNull;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.source.Symbolizable;
import org.sonar.scanner.deprecated.perspectives.PerspectiveBuilder;
import org.sonar.scanner.sensor.DefaultSensorStorage;

public class SymbolizableBuilder extends PerspectiveBuilder<Symbolizable> {

  private final DefaultSensorStorage sensorStorage;
  private final AnalysisMode analysisMode;

  public SymbolizableBuilder(DefaultSensorStorage sensorStorage, AnalysisMode analysisMode) {
    super(Symbolizable.class);
    this.sensorStorage = sensorStorage;
    this.analysisMode = analysisMode;
  }

  @CheckForNull
  @Override
  public Symbolizable loadPerspective(Class<Symbolizable> perspectiveClass, InputComponent component) {
    if (component.isFile()) {
      InputFile path = (InputFile) component;
      return new DefaultSymbolizable(path, sensorStorage, analysisMode);
    }
    return null;
  }
}
