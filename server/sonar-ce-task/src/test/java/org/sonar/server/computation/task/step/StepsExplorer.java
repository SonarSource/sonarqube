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
package org.sonar.server.computation.task.step;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import java.lang.reflect.Modifier;
import java.util.Set;
import javax.annotation.Nonnull;
import org.reflections.Reflections;

import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.FluentIterable.from;

public class StepsExplorer {
  /**
   * Compute set of canonical names of classes implementing ComputationStep in the specified package using reflection.
   */
  public static Set<String> retrieveStepPackageStepsCanonicalNames(String packageName) {
    Reflections reflections = new Reflections(packageName);

    return from(reflections.getSubTypesOf(ComputationStep.class))
        .filter(NotAbstractClass.INSTANCE)
        .transform(ClassToCanonicalName.INSTANCE)
        // anonymous classes do not have canonical names
        .filter(notNull())
        .toSet();
  }

  private enum NotAbstractClass implements Predicate<Class<? extends ComputationStep>> {
    INSTANCE;

    @Override
    public boolean apply(Class<? extends ComputationStep> input) {
      return !Modifier.isAbstract(input.getModifiers());
    }
  }

  public static Function<Class<?>, String> toCanonicalName() {
    return ClassToCanonicalName.INSTANCE;
  }

  private enum ClassToCanonicalName implements Function<Class<?>, String> {
    INSTANCE;

    @Override
    public String apply(@Nonnull Class<?> input) {
      return input.getCanonicalName();
    }
  }
}
