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
package org.sonar.ce.task.projectanalysis.measure;

import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.rules.ExternalResource;
import org.sonar.api.ce.measure.MeasureComputer;
import org.sonar.ce.task.projectanalysis.api.measurecomputer.MeasureComputerWrapper;

import static java.util.Objects.requireNonNull;

public class MeasureComputersHolderRule extends ExternalResource implements MeasureComputersHolder {

  private final MeasureComputer.MeasureComputerDefinitionContext context;

  private List<MeasureComputerWrapper> measureComputers = new ArrayList<>();

  public MeasureComputersHolderRule(MeasureComputer.MeasureComputerDefinitionContext context) {
    this.context = context;
  }

  @After
  public void tearDown() {
    measureComputers.clear();
  }

  @Override
  public Iterable<MeasureComputerWrapper> getMeasureComputers() {
    return measureComputers;
  }

  public void addMeasureComputer(MeasureComputer measureComputer) {
    requireNonNull(measureComputer, "Measure computer cannot be null");
    MeasureComputer.MeasureComputerDefinition definition = measureComputer.define(context);
    this.measureComputers.add(new MeasureComputerWrapper(measureComputer, definition));
  }
}
