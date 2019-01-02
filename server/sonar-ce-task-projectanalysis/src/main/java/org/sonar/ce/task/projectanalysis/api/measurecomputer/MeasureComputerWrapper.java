/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.api.measurecomputer;

import javax.annotation.concurrent.Immutable;
import org.sonar.api.ce.measure.MeasureComputer;
import org.sonar.api.ce.measure.MeasureComputer.MeasureComputerDefinition;

import static java.util.Objects.requireNonNull;

@Immutable
public class MeasureComputerWrapper {

  private final MeasureComputer computer;
  private final MeasureComputerDefinition definition;

  public MeasureComputerWrapper(MeasureComputer computer, MeasureComputerDefinition definition) {
    this.computer = requireNonNull(computer);
    this.definition = requireNonNull(definition);
  }

  public MeasureComputer getComputer() {
    return computer;
  }

  public MeasureComputerDefinition getDefinition() {
    return definition;
  }

  @Override
  public String toString() {
    return computer.toString();
  }
}
