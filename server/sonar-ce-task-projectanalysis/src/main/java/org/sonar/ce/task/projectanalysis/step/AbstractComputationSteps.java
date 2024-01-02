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
package org.sonar.ce.task.projectanalysis.step;

import com.google.common.collect.Iterables;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.ce.task.step.ComputationSteps;
import org.sonar.core.platform.Container;

/**
 * Abstract implementation of {@link ComputationStep} which provides the implementation of {@link ComputationSteps#instances()}
 * based on a {@link org.sonar.core.platform.Container}.
 */
public abstract class AbstractComputationSteps implements ComputationSteps {
  private final Container container;

  protected AbstractComputationSteps(Container container) {
    this.container = container;
  }

  @Override
  public Iterable<ComputationStep> instances() {
    return Iterables.transform(orderedStepClasses(), container::getComponentByType);
  }
}
