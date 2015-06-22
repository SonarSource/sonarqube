/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.computation.container;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import java.util.Set;
import org.reflections.Reflections;
import org.sonar.server.computation.step.ComputationStep;

public class StepsExplorer {
  /**
   * Compute set of canonical names of classes implementing ComputationStep in package step using reflection.
   */
  static Set<String> retrieveStepPackageStepsCanonicalNames() {
    Reflections reflections = new Reflections("org.sonar.server.computation.step");

    return Sets.newHashSet(Iterables.transform(reflections.getSubTypesOf(ComputationStep.class), ClassToCanonicalName.INSTANCE));
  }

  enum ClassToCanonicalName implements Function<Class<?>, String> {
    INSTANCE;

    @Override
    public String apply(Class<?> input) {
      return input.getCanonicalName();
    }
  }
}
