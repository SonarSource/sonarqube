/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.computation.step;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import javax.annotation.Nonnull;
import org.sonar.core.platform.ContainerPopulator;

/**
 * Abstract implementation of {@link ComputationStep} which provides the implementation of {@link ComputationSteps#instances()}
 * based on a {@link org.sonar.core.platform.ContainerPopulator.Container}.
 */
public abstract class AbstractComputationSteps implements ComputationSteps {
  private final ContainerPopulator.Container container;

  protected AbstractComputationSteps(ContainerPopulator.Container container) {
    this.container = container;
  }

  @Override
  public Iterable<ComputationStep> instances() {
    return Iterables.transform(orderedStepClasses(), new Function<Class<? extends ComputationStep>, ComputationStep>() {
      @Override
      public ComputationStep apply(@Nonnull Class<? extends ComputationStep> input) {
        ComputationStep computationStepType = container.getComponentByType(input);
        if (computationStepType == null) {
          throw new IllegalStateException(String.format("Component not found: %s", input));
        }
        return computationStepType;
      }
    });
  }
}
